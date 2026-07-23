package parser.driver;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import org.openqa.selenium.Keys;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Log
public class WebDriverPool {
    // Upper bound for a single render, comfortably above the 60s condition wait plus scrolling.
    // Guarantees a wedged Chrome session can never hold a pool slot indefinitely.
    private static final Duration RENDER_TIMEOUT = Duration.ofSeconds(150);
    private static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(60);

    @Autowired
    private ObjectFactory<AutoCloseableWebDriver> webDriverProvider;

    private final ExecutorService executor;
    private final Timer acquireWaitTimer;

    public WebDriverPool(@Value("${selenium.sessions.max:1}") int poolSize, MeterRegistry registry) {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        registry.gauge("webdriver.pool.queue.depth", pool.getQueue(), q -> q.size());
        executor = pool;
        acquireWaitTimer = Timer.builder("webdriver.pool.acquire.wait")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @SneakyThrows
    public String get(String url, boolean scroll, ExpectedCondition<?> expectedCondition) {
        return get(url, scroll, false, expectedCondition);
    }

    // Some sites (e.g. eBay) block a fresh session that navigates straight to a deep link
    // (e.g. a search results page) with no prior visit/cookies, but allow it once the session
    // has loaded the site's homepage first. `warmup` opts a parser into that homepage visit.
    @SneakyThrows
    public String get(String url, boolean scroll, boolean warmup, ExpectedCondition<?> expectedCondition) {
        Callable<String> callable = () -> {
            log.info("Obtaining WebDriver for "+url);
            AutoCloseableWebDriver driver = webDriverProvider.getObject();
            try ( driver  ) {
                driver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIMEOUT);

                if (warmup) {
                    // Avoid java.net.URI parsing here: several configured search URLs contain
                    // unencoded spaces in the query string, which Selenium's driver.get() tolerates
                    // fine but URI.create() rejects with IllegalArgumentException.
                    driver.get(url.replaceFirst("^(https?://[^/]+).*$", "$1"));
                }

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

        try {
            return submitBounded(callable);
        } catch (Exception e) {
            // A new-session failure (e.g. transient grid/Chrome resource pressure) is worth one retry.
            log.warning(String.format("Render failed for %s, retrying once. Cause: %s", url, e.getMessage()));
            return submitBounded(callable);
        }
    }

    private String submitBounded(Callable<String> callable) throws Exception {
        long enqueuedAt = System.nanoTime();
        Callable<String> timed = () -> {
            acquireWaitTimer.record(System.nanoTime() - enqueuedAt, TimeUnit.NANOSECONDS);
            return callable.call();
        };
        Future<String> future = executor.submit(timed);
        try {
            return future.get(RENDER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // Interrupt the worker so try-with-resources quits the wedged session and frees the slot.
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            throw (e.getCause() instanceof Exception cause) ? cause : e;
        }
    }
}
