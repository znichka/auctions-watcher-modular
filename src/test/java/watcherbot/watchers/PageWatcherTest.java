package watcherbot.watchers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import watcherbot.config.PageWatcherTestConfig;
import watcherbot.description.ItemDescription;
import watcherbot.description.PageDescription;
import watcherbot.parser.page.EbayPageParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PageWatcherTestConfig.class})
class PageWatcherTest {

    @Test
    void getNewItems() {
        String url1 = "https://www.ebay.com/sch/i.html?_from=R40&_nkw=glass+garland&_sacat=907&_sop=10";
        String url2 = "https://www.ebay.com/sch/i.html?_from=R40&_nkw=wooden+toy&_sacat=907&_sop=10";

        PageDescription pageDescription = new PageDescription();
        EbayPageParser mockPageParser = Mockito.mock(EbayPageParser.class);
        EbayPageParser realPageParser = new EbayPageParser();
        pageDescription.setUrl(url1);

        List<ItemDescription> parserList = realPageParser.getAllItems(url1);
        assertTrue(parserList.size() > 0);

        Mockito.when(mockPageParser.getAllItems(anyString())).thenReturn(parserList);
        PageWatcher pageWatcher = new PageWatcher(mockPageParser, pageDescription);
        List<ItemDescription> watcherList = pageWatcher.getNewItems();
        assertEquals(0, watcherList.size());

        parserList = realPageParser.getAllItems(url2);
        assertTrue(parserList.size() > 0);
        Mockito.when(mockPageParser.getAllItems(anyString())).thenReturn(parserList);
        watcherList = pageWatcher.getNewItems();
        assertTrue(watcherList.size() > 0);

        Mockito.when(mockPageParser.getAllItems(anyString())).thenThrow(new RuntimeException());
        watcherList = pageWatcher.getNewItems();
        assertEquals(0, watcherList.size());


    }
}