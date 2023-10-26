package watcherbot.parser.page;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import watcherbot.config.PageParserTestConfig;
import watcherbot.parser.page.AvitoPageParser;

import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PageParserTestConfig.class})
@Disabled
public class AvitoPageTest {
    @Autowired
    AvitoPageParser avitoPageParser;

    @Test
    public void getAllItems() {
        String url = "https://www.avito.ru/rossiya?q=%D0%B5%D0%BB%D0%BE%D1%87%D0%BD%D0%B0%D1%8F+%D0%B8%D0%B3%D1%80%D1%83%D1%88%D0%BA%D0%B0&s=104";
        assertTrue(avitoPageParser.getAllItems(url).size() > 0);
    }
}