package watcherbot.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan("watcherbot.parser")
@Import(WebDriverConfig.class)
public class PageParserTestConfig {
}
