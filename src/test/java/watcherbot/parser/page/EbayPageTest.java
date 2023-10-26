package watcherbot.parser.page;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import watcherbot.config.PageParserTestConfig;
import watcherbot.parser.page.EbayPageParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PageParserTestConfig.class})
public class EbayPageTest {
    @Autowired
    EbayPageParser ebayPageParser;

    @Test
    public void getAllItemsEbayCom() {
        String url = "https://www.ebay.com/sch/i.html?_from=R40&_nkw=glass+garland&_sacat=907&_sop=10";
        assertTrue(ebayPageParser.getAllItems(url).size() > 0);
    }

    @Test
    public void getNoItemsEbayCom() {
        String url = "https://www.ebay.com/sch/i.html?_from=R40&_trksid=p2334524.m570.l1313&_nkw=pushkin+glass+abrakadabra&_sacat=907&LH_TitleDesc=0&_odkw=pushkin+glass+abrakadabra&_osacat=907&_sop=10";
        assertEquals(0, ebayPageParser.getAllItems(url).size());
    }

    @Test
    public void getNoItemsEbayDe() {
        String url = "https://www.ebay.de/sch/i.html?_from=R40&_trksid=p2334524.m570.l1313&_nkw=wiltrud+elbert+christbaumschmuck&_sacat=0&LH_TitleDesc=0&_odkw=pushkin+glass+abrakadabra&_osacat=0";
        assertEquals(0, ebayPageParser.getAllItems(url).size());
    }

    @Test
    public void getAllItemsEbayDe_query() {
        String url = "https://www.ebay.de/sch/i.html?_from=R40&_nkw=alter+christbaum&_sacat=0&_sop=10";
        assertTrue(ebayPageParser.getAllItems(url).size() > 0);
    }

    @Test
    public void getAllItemsEbayDe_category() {
        String url = "https://www.ebay.de/b/Christbaum-Feiertagsschmuck/77988?Herstellungsjahr=1910%7C1920%7C1930%7C1900&mag=1&rt=nc&_pgn=2&_sop=10";
        assertTrue(ebayPageParser.getAllItems(url).size() > 0);
    }

    @Test
    public void getNoItemsEbayCom2() {
        String url = "https://www.ebay.com/sch/i.html?_from=R40&_trksid=p2380057.m570.l1313&_nkw=wiltrud+elbert+christbaumschmuck&_sacat=0";
        assertEquals(0, ebayPageParser.getAllItems(url).size());

//        url = "https://www.ebay.com/sch/i.html?_from=R40&_trksid=p2380057.m570.l1313&_nkw=wiltrud+elbert+christbaumschmuck&_sacat=0";
//        assertEquals(0, ebayPageParser.getAllItems(url).size());
    }
}