package com.thapar.gpf.database;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
@Profile("postgres")
public class NeonDataSourceConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource postgresDataSource(Environment environment) {
        String url = first(environment,
                "DB_URL",
                "POSTGRES_URL",
                "POSTGRES_URL_NON_POOLING");
        String username = first(environment, "DB_USER", "PGUSER", "POSTGRES_USER");
        String password = first(environment, "DB_PASSWORD", "PGPASSWORD", "POSTGRES_PASSWORD");

        if (url == null || url.isBlank()) {
            String host = first(environment, "PGHOST_UNPOOLED", "PGHOST", "POSTGRES_HOST");
            String database = first(environment, "PGDATABASE", "POSTGRES_DATABASE");
            if (host == null || host.isBlank()) {
                throw new IllegalStateException("PostgreSQL host is missing");
            }
            url = "jdbc:postgresql://" + host + "/" +
                    (database == null || database.isBlank() ? "grade_predictor" : database) +
                    "?sslmode=" + environment.getProperty("PGSSLMODE", "prefer");
        } else {
            url = toJdbcUrl(url);
        }

        URI uri = url.startsWith("jdbc:postgresql://")
                ? URI.create(url.substring("jdbc:".length()))
                : null;
        if (uri != null && uri.getUserInfo() != null) {
            String[] parts = uri.getUserInfo().split(":", 2);
            if (username == null || username.isBlank()) {
                username = decode(parts[0]);
            }
            if ((password == null || password.isBlank()) && parts.length == 2) {
                password = decode(parts[1]);
            }
            url = "jdbc:postgresql://" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                    + (uri.getRawPath() == null ? "" : uri.getRawPath())
                    + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        }

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "PostgreSQL password is missing; connect Neon to Vercel or set DB_PASSWORD");
        }

        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(url)
                .username(username)
                .password(password)
                .build();
        dataSource.setMaximumPoolSize(
                Integer.parseInt(environment.getProperty("DB_POOL_SIZE", "5")));
        return dataSource;
    }

    private static String first(Environment environment, String... names) {
        for (String name : names) {
            String value = environment.getProperty(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String toJdbcUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.startsWith("postgresql://")) {
            return "jdbc:" + trimmed;
        }
        if (trimmed.startsWith("postgres://")) {
            return "jdbc:postgresql://" + trimmed.substring("postgres://".length());
        }
        if (trimmed.startsWith("jdbc:postgresql://")) {
            return trimmed;
        }
        return trimmed;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
