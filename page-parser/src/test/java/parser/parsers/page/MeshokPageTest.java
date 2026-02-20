package parser.parsers.page;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import parser.config.PageParserTestConfig;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PageParserTestConfig.class})
//@ActiveProfiles("local")
public class MeshokPageTest {
    @Autowired
    MeshokPageParser meshokPageParser;

    @Test
    public void getAllItems() {
        String url = "https://meshok.net/listing?a_o=15&city_id=32&good=14536&sort=beg_date&way=desc&reposted=N";
        assertTrue(meshokPageParser.getAllItems(url).size() > 0);

    }
    @Test
    public void getAllItemsSearch() {
        String url = "https://meshok.net/listing?a_o=15&good=14299&reposted=N&search=%D0%B1%D1%83%D1%81%D1%8B&sort=beg_date&way=desc&reposted=N";
        assertTrue(meshokPageParser.getAllItems(url).size() > 0);

    }
    @Test
    public void getNoItemsSearch() {
        String url = "https://meshok.net/listing?a=1&a_o=2&a_o=15&good=109&search=blablabla&pp=48&sort=beg_date&way=desc&reposted=N";
        assertEquals(0, meshokPageParser.getAllItems(url).size());
    }
}
