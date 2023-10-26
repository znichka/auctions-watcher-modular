package watcherbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@SpringBootApplication
@EnableTransactionManagement
public class Starter {
    public static void main(String[] args)  {
        SpringApplication.run(Starter.class, args);
    }
}
