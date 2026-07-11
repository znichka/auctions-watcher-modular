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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@Log
public class SenderQueue {
    ThreadPoolExecutor executorService = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
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
        long enqueuedAt = System.nanoTime();
        executorService.submit(() -> {
            waitTimer.record(System.nanoTime() - enqueuedAt, TimeUnit.NANOSECONDS);
            try {
                sender.sendItemDescription(credentials, item);
            } catch (IOException e) {
                log.severe(String.format("Error while sending item details to telegram bot %s. Item photo url: %s, item url: %s", credentials.getToken(), item.getPhotoUrl(), item.getItemUrl()));
            }
        });
    }

    public void send(TelegramBotCredentials credentials, List<ItemDescription> items){
        for (var item : items) {
            send(credentials, item);
        }
    }
}
