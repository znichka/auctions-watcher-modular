package watcherbot.config;

import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import watcherbot.bot.SenderQueue;
import watcherbot.description.ManagerDescription;
import watcherbot.description.TelegramBotCredentials;
import watcherbot.bot.TelegramBotSender;
import watcherbot.service.ItemsService;
import watcherbot.watchers.PageWatchersManager;

import javax.sql.DataSource;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ComponentScan({"watcherbot.watchers"})
@Import({ItemsService.class})
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
