package watcherbot.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import watcherbot.config.PageParserFactoryTestConfig;
import watcherbot.config.PageParserTestConfig;
import watcherbot.parser.page.*;

import javax.naming.OperationNotSupportedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PageParserFactoryTestConfig.class})
public class PageParserFactoryTest {
    @Autowired
    PageParserFactory pageParserFactory;

    @Test
    public void getParserForValidUrl() {
        assertEquals(pageParserFactory.getParserFor("http://antiques.ay.by/").getClass(), AyPageParser.class);
        assertEquals(pageParserFactory.getParserFor("http://ay.by/").getClass(), AyPageParser.class);

        assertEquals(pageParserFactory.getParserFor("http://ebay.de/").getClass(), EbayPageParser.class);
        assertEquals(pageParserFactory.getParserFor("http://ebay.com").getClass(), EbayPageParser.class);
        assertEquals(pageParserFactory.getParserFor("http://avito.ru/").getClass(), AvitoPageParser.class);

        assertEquals(pageParserFactory.getParserFor("http://www.etsy.com/").getClass(), EtsyPageParser.class);
        assertEquals(pageParserFactory.getParserFor("http://etsy.com/").getClass(), EtsyPageParser.class);

        assertEquals(pageParserFactory.getParserFor("http://kufar.by/").getClass(), KufarPageParser.class);
        assertEquals(pageParserFactory.getParserFor("http://meshok.net/").getClass(), MeshokPageParser.class);
    }

    @Test
    public void getParserForInvalidUrl(){
        assertThrows(OperationNotSupportedException.class, () -> pageParserFactory.getParserFor("http://unknown.by/"));
        assertThrows(OperationNotSupportedException.class, () -> pageParserFactory.getParserFor("http://ayyy.by/"));
        assertThrows(OperationNotSupportedException.class, () -> pageParserFactory.getParserFor("http://aavitoo.by/"));
    }
}