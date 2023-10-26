package watcherbot.parser.page;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import watcherbot.config.PageParserTestConfig;
import watcherbot.parser.page.EtsyPageParser;

import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PageParserTestConfig.class})
@Disabled
public class EtsyPageTest {

    @Autowired
    EtsyPageParser etsyPageParser;

    @Test
    public void getAllItems() {
        String url = "https://www.etsy.com/search/vintage?explicit=1&q=glass+vintage+ornament&ship_to=BY&order=date_desc";
        assertTrue(etsyPageParser.getAllItems(url).size() > 0);
    }
}