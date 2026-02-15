package parser.driver;

import lombok.SneakyThrows;
import lombok.extern.java.Log;

import org.openqa.selenium.Cookie;
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

    public WebDriverPool(@Value("${selenium.sessions.max:1}") int poolSize) {
        executor = Executors.newFixedThreadPool(poolSize);
    }

    @SneakyThrows
    public String get(String url, boolean scroll, ExpectedCondition<WebElement> expectedCondition) {
        Callable<String> callable = () -> {
            log.info("Obtaining WebDriver for "+url);
            AutoCloseableWebDriver driver = webDriverProvider.getObject();
            try ( driver  ) {
                driver.get(url);

                driver.manage().addCookie(new Cookie("cf_clearance", "mUt_2gYJQH30Q3B2ue.acU_Y7wrgGBem_cambFn1gvI-1725200416-1.2.1.1-SPturHzLLk227q2kzDNGjrJLkHLKCupQPQmzugAmBpw8WKYGUkMwF3sPsYVpuSKAHqRed_Yd0LcwB7bo_ifQc3z2gbYhCgk0lRNXKn3.kBhQgcGsRYonBOFR3TYADblZMWbhbmnePFUs69YmEzukETq0C3AX8XNRxo92qJSHU17A.zywY_pSrbMQ1gOafwzvmGSNgfIPH5LHUCnqZ7nSC7npb0usryY2fdq3tnN2ljBhwIMASWa3hp0m9Jb7OX3frImV6hfFtrkLhjvfpVcwAswT_MlW7_Zc1U.gUhJPtGHeYx5vdtRvH9t2s4joEZJNgMxA3IeYutE_6eylbNB7uIDYafZCOjVuxg1eF9XF7jYTlij5y14rMi8m8AN0GCvRcR7YwjFaWfiVnNrkBt1uuBo8ubbGVNLrZf0UkL8gbCw"));
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
