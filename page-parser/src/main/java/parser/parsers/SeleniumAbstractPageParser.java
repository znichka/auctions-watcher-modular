package parser.parsers;

import parser.driver.WebDriverPool;
import lombok.extern.java.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Log
public abstract class SeleniumAbstractPageParser extends AbstractPageParser  {
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
