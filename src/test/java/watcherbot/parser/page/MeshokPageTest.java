package watcherbot.parser.page;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import watcherbot.config.PageParserTestConfig;
import watcherbot.parser.page.MeshokPageParser;

import java.time.Duration;

import static java.lang.System.out;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PageParserTestConfig.class})
//@ActiveProfiles("local")
public class MeshokPageTest {
    @Autowired
    MeshokPageParser meshokPageParser;
//    @Autowired
//    WebDriver driver;
//
//    @Test
//    public void test()
//    {
//        driver.get("https://meshok.net/en/listing?a_o=15&city_id=32&good=14299&sort=beg_date&way=desc&reposted=N");
//        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//        try
//        {
//            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[class^=itemCardList]")));
//        } catch (Exception e) {
//            LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
//            logs.getAll().forEach(l -> out.println(l.getMessage()));
//            throw e;
//        }
//    }
    @Test
    public void getAllItems() {
        String url = "https://meshok.net/en/listing?a_o=15&city_id=32&good=14299&sort=beg_date&way=desc&reposted=N";
        assertTrue(meshokPageParser.getAllItems(url).size() > 0);

    }
    @Test
    public void getAllItemsSearch() {
        String url = "https://meshok.net/en/listing?a_o=15&good=14299&reposted=N&search=%D0%B1%D1%83%D1%81%D1%8B&sort=beg_date&way=desc&reposted=N";
        assertTrue(meshokPageParser.getAllItems(url).size() > 0);

    }
    @Test
    public void getNoItemsSearch() {
        String url = "https://meshok.net/en/listing?a=1&a_o=2&a_o=15&good=109&search=blablabla&pp=48&sort=beg_date&way=desc&reposted=N";
        assertEquals(0, meshokPageParser.getAllItems(url).size());

    }
}