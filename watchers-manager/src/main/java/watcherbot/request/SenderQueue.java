package watcherbot.request;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import watcherbot.description.ItemDescription;
import watcherbot.description.TelegramBotCredentials;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@Log
public class SenderQueue {
    private static final int MAX_ATTEMPTS = 3;
    private static final long MIN_INTERVAL_MILLIS = 1100;
    private static final long DEFAULT_RETRY_DELAY_SECONDS = 5;

    ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(1);
    long lastSendAtMillis = 0;
    Timer waitTimer;

    @Autowired
    TelegramBotSender sender;

    @Autowired
    public SenderQueue(MeterRegistry registry) {
        registry.gauge("sender.queue.depth", executorService.getQueue(), q -> q.size());
        waitTimer = Timer.builder("sender.queue.wait")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void send(TelegramBotCredentials credentials, ItemDescription item) {
        trySend(credentials, item, System.nanoTime(), 1);
    }

    public void send(TelegramBotCredentials credentials, List<ItemDescription> items){
        for (var item : items) {
            send(credentials, item);
        }
    }

    private void trySend(TelegramBotCredentials credentials, ItemDescription item, long enqueuedAt, int attempt) {
        executorService.submit(() -> {
            waitTimer.record(System.nanoTime() - enqueuedAt, TimeUnit.NANOSECONDS);
            pace();
            try {
                sender.sendItemDescription(credentials, item);
            } catch (TelegramRateLimitException e) {
                if (attempt < MAX_ATTEMPTS) {
                    long delaySeconds = e.getRetryAfterSeconds() != null ? e.getRetryAfterSeconds() : DEFAULT_RETRY_DELAY_SECONDS;
                    log.warning(String.format("Rate limited by Telegram, retrying item %s in %ds (attempt %d/%d)", item.getItemUrl(), delaySeconds, attempt + 1, MAX_ATTEMPTS));
                    executorService.schedule(() -> trySend(credentials, item, System.nanoTime(), attempt + 1), delaySeconds, TimeUnit.SECONDS);
                } else {
                    log.severe(String.format("Giving up sending item to telegram bot %s after %d attempts due to rate limiting. Item url: %s", credentials.getToken(), MAX_ATTEMPTS, item.getItemUrl()));
                }
            } catch (IOException e) {
                log.severe(String.format("Error while sending item details to telegram bot %s. Item photo url: %s, item url: %s. Cause: %s", credentials.getToken(), item.getPhotoUrl(), item.getItemUrl(), e.getMessage()));
            }
        });
    }

    private void pace() {
        long waitMillis = MIN_INTERVAL_MILLIS - (System.currentTimeMillis() - lastSendAtMillis);
        if (waitMillis > 0) {
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastSendAtMillis = System.currentTimeMillis();
    }
}
