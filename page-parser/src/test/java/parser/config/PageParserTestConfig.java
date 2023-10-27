package parser.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan({"parser.parsers", "parser.driver"})
@Import(WebDriverConfig.class)
public class PageParserTestConfig {
}
