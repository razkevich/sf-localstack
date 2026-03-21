package co.razkevich.sflocalstack.config;

import org.springframework.context.annotation.Configuration;

/**
 * H2 database configuration.
 * JPA and datasource settings are in application.yml.
 * The H2 console is available at /h2-console when enabled.
 */
@Configuration
public class H2Config {
    // Configuration handled via application.yml spring.datasource and spring.jpa properties
}
