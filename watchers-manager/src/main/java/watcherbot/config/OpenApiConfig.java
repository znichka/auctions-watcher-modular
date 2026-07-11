package watcherbot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI watchersManagerOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Auctions Watcher — watchers-manager API")
                .description("Configure Telegram bots and the marketplace pages each one polls for new auction items.")
                .version("v1"));
    }
}
