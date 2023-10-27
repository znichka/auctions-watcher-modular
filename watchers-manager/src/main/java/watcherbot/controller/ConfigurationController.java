package watcherbot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import watcherbot.description.PageDescription;
import watcherbot.description.ManagerDescription;
import watcherbot.service.PageWatcherService;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ConfigurationController {
    @Autowired
    PageWatcherService service;

    @GetMapping("/health")
    @ResponseBody
    String healthcheck() {
        return "ok";
    }

    @GetMapping("/bots")
    @ResponseBody
    List<ManagerDescription> getAllManagers() {
        return service.getAllManagers();
    }

    @PostMapping("/bots")
    @ResponseBody
    ManagerDescription addManager(@RequestBody ManagerDescription managerDescription) {
        return service.addManager(managerDescription);//.getDescription();
    }

    @GetMapping("/bots/{manager_id}")
    @ResponseBody
    ManagerDescription getManager(@PathVariable("manager_id") int managerId) {
        return service.getManagerDescription(managerId);
    }

    @GetMapping("/bots/{manager_id}/pages")
    @ResponseBody
    List<PageDescription> getAllPages(@PathVariable("manager_id") int managerId) {
        List<PageDescription> list = service.getAllPages(managerId);
        if (list.size() == 0) return new ArrayList<>();
        return list;
    }

    @PostMapping("/bots/{manager_id}/pages")
    @ResponseBody
    PageDescription addPage(@PathVariable("manager_id") int managerId, @RequestBody PageDescription pageDescription) {
        return service.addPage(pageDescription, managerId);
    }

    @DeleteMapping("/bots/{manager_id}/pages/{page_id}")
    @ResponseBody
    boolean deletePage(@PathVariable("manager_id") int managerId, @PathVariable("page_id") int pageId) {
        return service.deletePage(managerId, pageId);
    }
}
