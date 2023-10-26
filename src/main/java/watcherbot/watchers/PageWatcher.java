package watcherbot.watchers;

import lombok.Getter;
import lombok.extern.java.Log;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import watcherbot.description.ItemDescription;
import watcherbot.description.PageDescription;
import watcherbot.parser.AbstractPageParser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Log
@Component
@Scope("prototype")
public class PageWatcher {
    final PageDescription description;

    private final AbstractPageParser parser;
    @Getter
    private LocalDateTime timestamp;

    public PageWatcher(AbstractPageParser parser, PageDescription pageDescription) {
        this.description = pageDescription;
        this.parser = parser;
    }

    public List<ItemDescription> getNewItems() {
        try {
            List<ItemDescription> items = parser.getAllItems(description.getUrl());
            if (items.size() != 0)
                timestamp = LocalDateTime.now();
            return items;
        } catch (Exception e) {
            log.warning(String.format("Error while getting new items for a page. Page info: %s", description));
            return new ArrayList<>();
        }
    }

    public String getDescription() {
        return description.getDescription();
    }

    public Long getNotify() {
        return description.getNotify();
    }

    public Integer getPeriod() {
        return description.getPeriod();
    }
}
