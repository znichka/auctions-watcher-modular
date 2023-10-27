package watcherbot.config;

import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import watcherbot.request.ParserService;
import watcherbot.request.SenderQueue;
import watcherbot.description.ManagerDescription;
import watcherbot.request.TelegramBotSender;
import watcherbot.service.ItemsService;
import watcherbot.watchers.PageWatchersManager;

import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ComponentScan({"watcherbot.watchers"})
@PropertySource("classpath:application.properties")
@Import({ItemsService.class, ParserService.class})
public class PageWatchersManagerTestConfig {
    @Autowired
    ObjectProvider<PageWatchersManager> pageWatchersManagerProvider;

    @Bean
    TelegramBotSender getMockTelegramBotSender() {
        return Mockito.mock(TelegramBotSender.class);
    }

    @Bean
    public SenderQueue getSenderQueue(){
        return Mockito.mock(SenderQueue.class);
    }

    @Bean
    ScheduledExecutorService getMockScheduledExecutorService() {
        return Mockito.mock(ScheduledExecutorService.class);
    }

    public PageWatchersManager getPageWatcherManager(ManagerDescription description){
        return pageWatchersManagerProvider.getObject(description);
    }
}
