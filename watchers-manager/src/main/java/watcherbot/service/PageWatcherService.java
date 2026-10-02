package watcherbot.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.java.Log;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import watcherbot.description.ManagerDescription;
import watcherbot.description.PageDescription;
import watcherbot.exception.ManagerNotFoundException;
import watcherbot.exception.PageNotFoundException;
import watcherbot.repository.PageDescriptionRepository;
import watcherbot.repository.ManagerDescriptionRepository;
import watcherbot.request.ParserService;
import watcherbot.watchers.PageWatchersManager;

import java.util.*;

@Service
@Transactional
@Log
public class PageWatcherService {
    Map<Integer, PageWatchersManager> workerList = new HashMap<>();;

    @Autowired
    ManagerDescriptionRepository managerDescriptionRepository;
    @Autowired
    PageDescriptionRepository pageDescriptionRepository;
    @Autowired
    ItemsService itemsService;
    @Autowired
    ObjectProvider<PageWatchersManager> pageWatchersManagerProvider;
    @Autowired
    ObjectProvider<ParserService> parserRequestsProvider;

    public PageDescription addPage(PageDescription pageDescription, int managerId) {
        pageDescriptionRepository.save(pageDescription);

        getManagerDescription(managerId).addPage(pageDescription);

        // Seed the DB with the page's current items (marked as already-seen) before the
        // scheduled watcher is registered below. Otherwise, for a slow-to-parse source (e.g.
        // Selenium-backed eBay/Meshok/Olx/Avito), the watcher's own first run (fixed 1-minute
        // initial delay) can fire and find the table still empty, treating every existing item
        // as "new" and sending it to the bot's chat before this seeding step gets a chance to run.
        try {
            parserRequestsProvider.getObject().getItems(pageDescription).forEach(item -> itemsService.insertIfUnique(item, managerId));
        } catch (Exception e) {
            log.severe("Error while seeding existing items for " + pageDescription.getDescription() + ": " + e.getMessage());
        }

        return loadPage(pageDescription, managerId);
    }

    public ManagerDescription addManager(ManagerDescription managerDescription) {
        managerDescriptionRepository.save(managerDescription);
        loadPageWatchersManager(managerDescription);
        return managerDescription;
    }


    public List<PageDescription> getAllPages(int managerId) {
        return getManagerDescription(managerId).getPages().stream().toList();
    }

    public List<ManagerDescription> getAllManagers() {
        return managerDescriptionRepository.findAll();
    }

    public ManagerDescription getManagerDescription(int managerId) {
        Optional<ManagerDescription> result = managerDescriptionRepository.findById(managerId);
        if (result.isEmpty()) throw new ManagerNotFoundException();
        return result.get();
    }

    @PostConstruct
    public void loadAllPageWatchersManagers() {
        for (ManagerDescription managerDescription : managerDescriptionRepository.findAll()) {
            loadPageWatchersManager(managerDescription);
        }
    }

    private PageWatchersManager loadPageWatchersManager(ManagerDescription managerDescription) {
        PageWatchersManager manager = pageWatchersManagerProvider.getObject(managerDescription);
        workerList.put(manager.getId(), manager);
        managerDescription.getPages().forEach(p -> loadPage(p, managerDescription.getId()));
        return manager;
    }

    private PageDescription loadPage(PageDescription pageDescription, int managerId) {
        try {
            getPageWatchersManager(managerId).registerPage(pageDescription);
            return pageDescription;
        } catch (Exception e) {
            log.severe("Error while adding " + pageDescription.getDescription());
        }
        return null;
    }

    private PageWatchersManager getPageWatchersManager(int managerId) {
        if (!workerList.containsKey(managerId)) throw new ManagerNotFoundException();
        return workerList.get(managerId);
    }

    private PageDescription getPageDescription(int pageId) {
        Optional<PageDescription> result = pageDescriptionRepository.findById(pageId);
        if (result.isEmpty()) throw new PageNotFoundException();
        return result.get();
    }

    public boolean deletePage(int managerId, int pageId) {
        PageDescription pageDescription = getPageDescription(pageId);
        boolean result1 = getManagerDescription(managerId).removePage(pageDescription);
        boolean result2 = getPageWatchersManager(managerId).removePage(pageDescription);
        return result1 & result2;
    }
}
