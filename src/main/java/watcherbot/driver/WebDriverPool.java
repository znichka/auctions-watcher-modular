package watcherbot.driver;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Log
public class WebDriverPool {
    @Autowired
    private ObjectFactory<AutoCloseableWebDriver> webDriverProvider;

    private final ExecutorService executor;

    public WebDriverPool(@Value("${selenium.sessions.max}") int poolSize) {
        executor = Executors.newFixedThreadPool(poolSize);
    }

    @SneakyThrows
    public String get(String url, boolean scroll, ExpectedCondition<WebElement> expectedCondition) {
        Callable<String> callable = () -> {
            log.info("Obtaining WebDriver for "+url);
            AutoCloseableWebDriver driver = webDriverProvider.getObject();
            try ( driver  ) {
                driver.get(url);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

                if (scroll) {
                    int pageCount = 10;
                    for (int i = 0; i < pageCount; i++)
                        new Actions(driver).keyDown(Keys.SPACE).pause(1000).keyUp(Keys.SPACE).perform();
                }

                try {
                    wait.until(expectedCondition);
                } catch (Exception e) {
                    LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
                    logs.getAll().forEach(l -> log.warning(l.getMessage()));
                    throw e;
                }
                log.info("Getting page source for "+url);
                return driver.getPageSource();
            }

        };

        return executor.submit(callable).get();
    }
}
