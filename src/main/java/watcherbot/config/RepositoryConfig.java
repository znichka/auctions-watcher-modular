package watcherbot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import watcherbot.service.PageWatcherService;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories({"watcherbot.repository"})
public class RepositoryConfig {
    @Bean
    NamedParameterJdbcTemplate getNamedParameterJdbcTemplate(@Autowired DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
