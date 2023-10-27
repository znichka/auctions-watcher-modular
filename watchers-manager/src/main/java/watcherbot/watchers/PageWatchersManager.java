package watcherbot.watchers;

import lombok.extern.java.Log;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import watcherbot.request.ParserService;
import watcherbot.request.SenderQueue;
import watcherbot.description.ItemDescription;
import watcherbot.description.ManagerDescription;
import watcherbot.description.PageDescription;
import watcherbot.service.ItemsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MINUTES;

@Log
@Scope("prototype")
@Component
public class PageWatchersManager  {
    final ManagerDescription description;

    @Value("${noimage.link}")
    String NO_IMAGE_LINK;

    @Autowired
    SenderQueue senderQueue;
    @Autowired
    ObjectProvider<ParserService> parserServicesProvider;
    @Autowired
    ScheduledExecutorService scheduledExecutorService;
    @Autowired
    ItemsService itemsService;

    final Map<Integer, PageDescription> registeredPages;
    final Map<Integer, ScheduledFuture<?>> registeredScheduledTasks;

    LinkedBlockingQueue<ItemDescription> items;

    public PageWatchersManager(ManagerDescription description) {
        this.description = description;

        registeredPages = new HashMap<>();
        registeredScheduledTasks = new HashMap<>();
        items = new LinkedBlockingQueue<>();
    }

    public List<ItemDescription> filterUniqueItems(List<ItemDescription> items){
        return items.stream().filter(item ->  itemsService.insertIfUnique(item, description.getId()))
                      .peek(item -> {
                          if (item.getPhotoUrl() == null)
                              item.setPhotoUrl(NO_IMAGE_LINK);
                      })
                      .collect(Collectors.toList());
    }



    public void registerPage(PageDescription pageDescription){
        Runnable runnable = () -> {
            log.info(String.format("Run for a page - %s", pageDescription.getDescription()));
            CompletableFuture.supplyAsync(() -> parserServicesProvider.getObject().getItems(pageDescription))
                    .exceptionally(e -> {
                        log.severe(String.format("Error while parsing the page. Url: %s", pageDescription.getDescription()));
                        log.severe(e.getMessage());
                        return List.of();
                    })
                    .thenApply(this::filterUniqueItems)
                    .thenAccept(items -> {
                        log.info(String.format("%d new items for %s page for %s bot", items.size(), pageDescription.getDescription(), description.getName()));
                        senderQueue.send(description.getCredentials(), items);
                    });
        };

        synchronized (this) {
            ScheduledFuture<?> future = scheduledExecutorService.scheduleAtFixedRate(runnable, 1, pageDescription.getPeriod(), MINUTES);
            registeredScheduledTasks.put(pageDescription.getId(), future);
            registeredPages.put(pageDescription.getId(), pageDescription);
        }
    }

    public synchronized boolean removePage(PageDescription pageDescription) {
        int pageId = pageDescription.getId();
        if (!registeredScheduledTasks.containsKey(pageId)) return false;
        if (!registeredPages.containsKey(pageId)) return false;

        registeredPages.remove(pageId);
        registeredScheduledTasks.get(pageId).cancel(true);
        registeredScheduledTasks.remove(pageId);

        log.info(String.format("Removed page watcher from %s bot. Page id = %d, description = %s", description.getName(), pageId, pageDescription.getDescription()));
        return true;
    }

    public int getId() {
        return description.getId();
    }
}
