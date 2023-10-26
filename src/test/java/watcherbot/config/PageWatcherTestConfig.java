package watcherbot.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan({"watcherbot.parser", "watcherbot.watchers"})
@Import(WebDriverConfig.class)
public class PageWatcherTestConfig {
}
