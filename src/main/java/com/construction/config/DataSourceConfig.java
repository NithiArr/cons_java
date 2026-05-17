package com.construction.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Custom DataSource configuration to support Railway's DATABASE_URL format.
 * Railway provides a full postgresql:// URI which Spring Boot can't parse natively.
 * This config converts it to a proper JDBC URL automatically.
 * Only active when the DATABASE_URL environment variable is set.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() throws URISyntaxException {
        String databaseUrl = System.getenv("DATABASE_URL");

        HikariConfig config = new HikariConfig();

        if (databaseUrl != null && !databaseUrl.isBlank()) {
            // ---- Railway Mode: Parse full postgresql:// URI ----
            // Railway provides: postgresql://user:password@host:port/dbname
            // We need:          jdbc:postgresql://host:port/dbname

            // Normalize "postgres://" to "postgresql://"
            String normalizedUrl = databaseUrl.replace("postgres://", "postgresql://");
            URI dbUri = new URI(normalizedUrl);

            String host = dbUri.getHost();
            int port = dbUri.getPort() == -1 ? 5432 : dbUri.getPort();
            String path = dbUri.getPath();
            String userInfo = dbUri.getUserInfo();

            String username = userInfo.split(":")[0];
            String password = userInfo.substring(username.length() + 1); // handles passwords with colons

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + "?sslmode=require";

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);

            System.out.println(">>> [DataSourceConfig] Using DATABASE_URL (Railway mode): " + host + ":" + port + path);
        } else {
            // ---- Local Mode: Use environment variables or defaults ----
            String pgHost = getEnvOrDefault("PGHOST", getEnvOrDefault("DB_HOST", "localhost"));
            String pgPort = getEnvOrDefault("PGPORT", getEnvOrDefault("DB_PORT", "5432"));
            String pgDb   = getEnvOrDefault("PGDATABASE", getEnvOrDefault("DB_NAME", "construction_db"));
            String pgUser = getEnvOrDefault("PGUSER", getEnvOrDefault("DB_USERNAME", "postgres"));
            String pgPass = getEnvOrDefault("PGPASSWORD", getEnvOrDefault("DB_PASSWORD", "admin123"));

            String jdbcUrl = "jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgDb + "?sslmode=prefer";

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(pgUser);
            config.setPassword(pgPass);

            System.out.println(">>> [DataSourceConfig] Using local PG config: " + pgHost + ":" + pgPort + "/" + pgDb);
        }

        // Common pool settings
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }

    private String getEnvOrDefault(String key, String defaultVal) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultVal;
    }
}
