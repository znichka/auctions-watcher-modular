package watcherbot.parser;

import lombok.extern.java.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import watcherbot.driver.AutoCloseableWebDriver;
import watcherbot.driver.WebDriverPool;

import java.io.IOException;
import java.time.Duration;

@Component
@Log
public abstract class SeleniumAbstractPageParser extends AbstractPageParser  {
//    @Autowired
//    private ObjectFactory<AutoCloseableWebDriver> webDriverProvider;

    @Autowired
    WebDriverPool webDriverPool;

    protected abstract ExpectedCondition<WebElement> expectedCondition();

    protected boolean scroll = false;

    @Override
    protected Document getDocument(String url) throws IOException {
        try {
            return Jsoup.parse(webDriverPool.get(url, scroll, expectedCondition()));
        } catch (BeansException e) {
            log.warning(String.format("Web driver for parser %s is not available, falling back to default parsing method", this.getDomainName()));
            return super.getDocument(url);
        } catch (Exception e){
            log.warning(String.format("Error while parsing the page, possible timeout. Url: %s", url));
            System.out.println(e.getMessage());
            throw e;
        }
    }
}
