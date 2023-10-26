package watcherbot.parser.page;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import watcherbot.config.PageParserTestConfig;
import watcherbot.parser.page.AyPageParser;

import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PageParserTestConfig.class})
public class AyPageTest {

    @Autowired
    AyPageParser ayPageParser;

    @Test
    public void getAllItems() {
        String url = "http://antiques.ay.by/retro-veschi/igrushki/yolochnye/?f=1&order=create&createdlots=1";
        assertTrue(ayPageParser.getAllItems(url).size() > 0);
    }
}