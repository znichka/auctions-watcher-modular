package watcherbot.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import watcherbot.bot.SenderQueue;
import watcherbot.bot.TelegramBotSender;
import watcherbot.repository.ManagerDescriptionRepository;
import watcherbot.repository.PageDescriptionRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ComponentScan({"watcherbot.controller",
                "watcherbot.parser",
                "watcherbot.service",
                "watcherbot.watchers"})
public class ConfigurationControllerTestConfig {
    @Bean
    public static ScheduledExecutorService getScheduledExecutorService() {
        return Executors.newScheduledThreadPool(1);
    }

    @Bean
    public ManagerDescriptionRepository getMockPageWatchersManagerRepository() {
        return Mockito.mock(ManagerDescriptionRepository.class);
    }

    @Bean
    public PageDescriptionRepository getMockPageWatcherRepository() {
        return Mockito.mock(PageDescriptionRepository.class);
    }

    @Bean
    public TelegramBotSender getTelegramBotSender(){
        return Mockito.mock(TelegramBotSender.class);
    }

    @Bean
    public SenderQueue getSenderQueue(){
        return Mockito.mock(SenderQueue.class);
    }

    @Bean
    public NamedParameterJdbcTemplate getItemsService() {
        return Mockito.mock(NamedParameterJdbcTemplate.class);
    }

}
