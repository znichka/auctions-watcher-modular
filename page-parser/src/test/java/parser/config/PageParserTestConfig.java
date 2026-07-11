package parser.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan({"parser.parsers", "parser.driver"})
@Import(WebDriverConfig.class)
public class PageParserTestConfig {
    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
