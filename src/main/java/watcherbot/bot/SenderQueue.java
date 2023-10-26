package watcherbot.bot;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import watcherbot.description.ItemDescription;
import watcherbot.description.TelegramBotCredentials;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Log
public class SenderQueue {
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    TelegramBotSender sender;

    public synchronized void send(TelegramBotCredentials credentials, ItemDescription item) {
        executorService.submit(() -> {
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
