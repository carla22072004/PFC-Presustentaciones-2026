package ec.edu.uteq.presustentaciones.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cache.annotation.EnableCaching;

@Configuration
@EnableJpaRepositories(basePackages = "ec.edu.uteq.presustentaciones.repositories")
@EnableAsync
@EnableCaching
public class AppConfig {
}
