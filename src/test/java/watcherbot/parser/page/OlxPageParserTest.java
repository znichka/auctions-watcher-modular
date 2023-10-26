package watcherbot.parser.page;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import watcherbot.config.PageParserTestConfig;
import watcherbot.parser.page.OlxPageParser;

import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PageParserTestConfig.class})
public class OlxPageParserTest {

    @Autowired
    OlxPageParser olxPageParser;

    @Test
    public void getAllItems() {
        String url = "https://www.olx.pl/d/oferty/q-bombka-prl/?search%5Border%5D=created_at:desc&view=grid";
        assertTrue(olxPageParser.getAllItems(url).size() > 0);
    }
}