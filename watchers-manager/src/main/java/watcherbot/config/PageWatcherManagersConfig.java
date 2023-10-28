package watcherbot.config;

import lombok.extern.java.Log;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Log
@Configuration
@EnableScheduling
public class PageWatcherManagersConfig {
    @Bean
    public static ScheduledExecutorService getScheduledExecutorService() {
        ScheduledThreadPoolExecutor service = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() - 1);
        service.setRemoveOnCancelPolicy(true);
        return service;
    }

    @Bean
    @Scope("prototype")
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

//    @Bean
//    public static ExecutorService getSenderExecutorService() {
//        return Executors.newSingleThreadExecutor();
//    }
}
