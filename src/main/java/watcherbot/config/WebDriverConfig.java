package watcherbot.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.java.Log;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import watcherbot.driver.AutoCloseableWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

@Log
@Configuration
@PropertySource("classpath:application.properties")
@ComponentScan("watcherbot.driver")
public class WebDriverConfig {
    @Bean(destroyMethod = "quit", name = "webDriver")
    @Scope("prototype")
    @Profile("local")
    public static synchronized AutoCloseableWebDriver getLocalWebDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.6.1 Safari/605.1.15");
        options.addArguments("--remote-allow-origins=*");

        return new AutoCloseableWebDriver(new ChromeDriver(options));
    }

    @Bean(destroyMethod = "quit", name = "webDriver")
    @Scope("prototype")
    @Profile({"!local"})
    public static synchronized AutoCloseableWebDriver getRemoteWebDriver(@Value("${docker.chromedriver.url}") String dockerChomeDriverUrl ) throws ExecutionException, InterruptedException {
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.6.1 Safari/605.1.15");
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = null;
        try {
            driver = new RemoteWebDriver(new URL(dockerChomeDriverUrl), options);
        } catch (MalformedURLException e) {
            log.severe(String.format("Web driver not created, url is %s, exception message is %s", dockerChomeDriverUrl, e.getMessage()));
            e.printStackTrace();
        }

        if (driver == null)
            log.severe("Web driver not created, possible timeout. Url is " + dockerChomeDriverUrl);
        return new AutoCloseableWebDriver(driver);
    }
}
