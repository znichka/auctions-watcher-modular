package watcherbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import watcherbot.description.PageDescription;
import watcherbot.description.ManagerDescription;
import watcherbot.service.PageWatcherService;

import java.util.ArrayList;
import java.util.List;

@RestController
@Tag(name = "Watcher configuration", description = "Manage Telegram bots and the marketplace pages each one polls for new items")
public class ConfigurationController {
    @Autowired
    PageWatcherService service;

    @Operation(summary = "Health check", description = "Used by uptime monitoring; returns \"ok\" whenever the app is running")
    @GetMapping("/health")
    @ResponseBody
    String healthcheck() {
        return "ok";
    }

    @Operation(summary = "List all managers", description = "Returns every configured Telegram bot along with the pages it watches")
    @GetMapping("/bots")
    @ResponseBody
    List<ManagerDescription> getAllManagers() {
        return service.getAllManagers();
    }

    @Operation(summary = "Register a new manager", description = "Creates a Telegram bot entry from its credentials. Pages are attached afterwards via POST /bots/{manager_id}/pages")
    @PostMapping("/bots")
    @ResponseBody
    ManagerDescription addManager(@RequestBody ManagerDescription managerDescription) {
        return service.addManager(managerDescription);//.getDescription();
    }

    @Operation(summary = "Get a manager by id")
    @GetMapping("/bots/{manager_id}")
    @ResponseBody
    ManagerDescription getManager(@Parameter(description = "Manager id") @PathVariable("manager_id") int managerId) {
        return service.getManagerDescription(managerId);
    }

    @Operation(summary = "List pages watched by a manager")
    @GetMapping("/bots/{manager_id}/pages")
    @ResponseBody
    List<PageDescription> getAllPages(@Parameter(description = "Manager id") @PathVariable("manager_id") int managerId) {
        List<PageDescription> list = service.getAllPages(managerId);
        if (list.size() == 0) return new ArrayList<>();
        return list;
    }

    @Operation(summary = "Add a page to watch", description = "Starts polling the given URL on a schedule (period in minutes); new items are pushed via the manager's Telegram bot")
    @PostMapping("/bots/{manager_id}/pages")
    @ResponseBody
    PageDescription addPage(@Parameter(description = "Manager id") @PathVariable("manager_id") int managerId, @RequestBody PageDescription pageDescription) {
        return service.addPage(pageDescription, managerId);
    }

    @Operation(summary = "Stop watching a page", description = "Removes the page and cancels its scheduled polling")
    @DeleteMapping("/bots/{manager_id}/pages/{page_id}")
    @ResponseBody
    boolean deletePage(@Parameter(description = "Manager id") @PathVariable("manager_id") int managerId, @Parameter(description = "Page id") @PathVariable("page_id") int pageId) {
        return service.deletePage(managerId, pageId);
    }
}
