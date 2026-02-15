package parser.config;

import parser.driver.AutoCloseableWebDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.java.Log;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

@Log
@Configuration
@PropertySource("classpath:application.properties")
@ComponentScan("parser.driver")
public class WebDriverConfig {
    @Bean(destroyMethod = "quit", name = "webDriver")
    @Scope("prototype")
    @Profile("local")
    public static synchronized AutoCloseableWebDriver getLocalWebDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", java.util.List.of("enable-automation"));
        options.addArguments("--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.6.1 Safari/605.1.15");
        options.addArguments("--remote-allow-origins=*");

        return new AutoCloseableWebDriver(new ChromeDriver(options));
    }

    @Bean(destroyMethod = "quit", name = "webDriver")
    @Scope("prototype")
    @Profile({"!local"})
    public static synchronized AutoCloseableWebDriver getRemoteWebDriver(@Value("${docker.chromedriver.url}") String dockerChomeDriverUrl ) throws ExecutionException, InterruptedException {
        log.info(dockerChomeDriverUrl);
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", java.util.List.of("enable-automation"));
        options.addArguments("--accept=application/json, text/plain, */*");
        options.addArguments("--accept-encoding=gzip, deflate, br");
        options.addArguments("--accept-language=en-GB,en-US;q=0.9,en;q=0.8");
        options.addArguments("--sec-ch-ua=\"Chromium\";v=\"117\", \"Google Chrome\";v=\"117\", \"Not-A.Brand\";v=\"8\"");
        options.addArguments("--sec-ch-ua-mobile=?0");
        options.addArguments("--sec-ch-ua-platform=macOS");
        options.addArguments("--sec-fetch-dest=empty");
        options.addArguments("--sec-fetch-mode=no-cors");
        options.addArguments("--sec-fetch-site=same-origin");
        options.addArguments("--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36");
        options.addArguments("--remote-allow-origins=*");
//        options.addArguments("--accept-lang=ru-RU");


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
