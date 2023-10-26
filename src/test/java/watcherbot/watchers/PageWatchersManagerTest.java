package watcherbot.watchers;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.ContextConfiguration;
import watcherbot.description.ManagerDescription;
import watcherbot.config.PageWatchersManagerTestConfig;
import watcherbot.description.ItemDescription;
import watcherbot.parser.page.EbayPageParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = {PageWatchersManagerTestConfig.class})
class PageWatchersManagerTest {

    @Autowired
    PageWatchersManagerTestConfig config;

    @Test
    void send() {
        String url1 = "https://www.ebay.com/sch/i.html?_from=R40&_nkw=glass+garland&_sacat=907&_sop=10";
        String url2 = "https://www.ebay.com/sch/i.html?_from=R40&_nkw=wooden+toy&_sacat=907&_sop=10";
        EbayPageParser realPageParser = new EbayPageParser();
        List<ItemDescription> items = realPageParser.getAllItems(url1);

        PageWatchersManager manager = config.getPageWatcherManager(Mockito.mock(ManagerDescription.class));
//        manager.send(items);
        items = manager.filterUniqueItems(items);
        assertTrue(items.size() > 0);
        items = manager.filterUniqueItems(items);
        assertEquals(0, items.size());

        items = realPageParser.getAllItems(url2);
        items = manager.filterUniqueItems(items);
        assertTrue(items.size() > 0);

        ItemDescription item1 = new ItemDescription();
        item1.setPhotoUrl("https://b.itemimg.com/i/289756499.0.jpg");
        item1.setId("1");
        ItemDescription item2 = new ItemDescription();
        item2.setPhotoUrl("https://b.itemimg.com/i/288334525.0.jpg");
        item2.setId("2");
        ItemDescription item3 = new ItemDescription();
        item3.setPhotoUrl("https://b.itemimg.com/i/288334552.0.jpg");
        item3.setId("3");

        items = List.of(item1, item3);
        manager.filterUniqueItems(items);
        items = List.of(item3);
        items = manager.filterUniqueItems(items);
        assertEquals(0, items.size());

    }
}