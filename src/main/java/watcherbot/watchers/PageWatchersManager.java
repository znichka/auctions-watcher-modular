package watcherbot.watchers;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import watcherbot.bot.SenderQueue;
import watcherbot.bot.TelegramBotSender;
import watcherbot.description.ItemDescription;
import watcherbot.description.ManagerDescription;
import watcherbot.description.PageDescription;
import watcherbot.service.ItemsService;

import java.io.IOException;
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
//    @Autowired
//    TelegramBotSender sender;
    @Autowired
    SenderQueue senderQueue;

    @Autowired
    ScheduledExecutorService scheduledExecutorService;
    @Autowired
    ItemsService itemsService;

    final Map<Integer, PageWatcher> registeredPageWatchers;
    final Map<Integer, ScheduledFuture<?>> registeredScheduledTasks;

    LinkedBlockingQueue<ItemDescription> items;

    public PageWatchersManager(ManagerDescription description) {
        this.description = description;

        registeredPageWatchers = new HashMap<>();
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

    public void registerPageWatcher(PageWatcher pageWatcher){
        Runnable runnable = () -> {
            log.info(String.format("Run for a page - %s", pageWatcher.getDescription()));
            CompletableFuture.supplyAsync(pageWatcher::getNewItems)
                    .exceptionally(e -> {
                        log.severe(String.format("Error while parsing the page. Url: %s", pageWatcher.getDescription()));
                        log.severe(e.getMessage());
                        return List.of();
                    })
                    .thenApply(this::filterUniqueItems)
                    .thenAccept(items -> {
                        log.info(String.format("%d new items for %s page for %s bot", items.size(), pageWatcher.getDescription(), description.getName()));
                        senderQueue.send(description.getCredentials(), items);
                    });
        };

        synchronized (this) {
            ScheduledFuture<?> future = scheduledExecutorService.scheduleAtFixedRate(runnable, 1, pageWatcher.getPeriod(), MINUTES);
            registeredScheduledTasks.put(pageWatcher.description.getId(), future);
            registeredPageWatchers.put(pageWatcher.description.getId(), pageWatcher);
        }
    }

    public synchronized boolean removePageWatcher(PageDescription pageDescription) {
        int pageId = pageDescription.getId();
        if (!registeredScheduledTasks.containsKey(pageId)) return false;
        if (!registeredPageWatchers.containsKey(pageId)) return false;

        registeredPageWatchers.remove(pageId);
        registeredScheduledTasks.get(pageId).cancel(true);
        registeredScheduledTasks.remove(pageId);

        log.info(String.format("Removed page watcher from %s bot. Page id = %d, description = %s", description.getName(), pageId, pageDescription.getDescription()));
        return true;
    }

//    public void send(List<ItemDescription> items) {
//        for(ItemDescription item : items) {
//            try  {
//                sender.sendItemDescription(description.getCredentials(), item);
//            } catch (IOException e) {
//                log.severe(String.format("Error while sending item details to telegram bot %s. Item photo url: %s, item url: %s", description.getName(), item.getPhotoUrl(), item.getItemUrl()));
//            }
//        }
//    }

//    public void send(String message) {
//        try {
//            sender.sendMessage(description.getCredentials(), message);
//        } catch (IOException e) {
//            log.severe(String.format("Error while sending message to telegram bot %s. Message: %s", description.getName(), message));
//        }
//    }

    public int getId() {
        return description.getId();
    }
}
