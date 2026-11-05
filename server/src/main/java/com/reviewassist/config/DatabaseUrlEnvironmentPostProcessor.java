package com.reviewassist.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lets the app be configured with a standard Postgres connection string, the format Neon, Supabase,
 * Render, Railway and Heroku hand out:
 *
 * <pre>DATABASE_URL=postgresql://user:password@host:5432/dbname?sslmode=require</pre>
 *
 * It is translated into the JDBC url, username and password Spring expects. If DATABASE_URL is not
 * set, the DB_URL / DB_HOST / DB_USER ... variables in application.yml apply as before.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        String raw = env.getProperty("DATABASE_URL");
        if (raw == null || raw.isBlank()) {
            return;
        }
        Map<String, Object> props = new LinkedHashMap<>();
        toJdbc(raw.trim(), props);
        env.getPropertySources().addFirst(new MapPropertySource("databaseUrl", props));
    }

    static void toJdbc(String raw, Map<String, Object> out) {
        URI uri;
        try {
            uri = URI.create(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("DATABASE_URL is not a valid URL (check for unescaped characters in the password)", e);
        }
        String scheme = uri.getScheme();
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            throw new IllegalStateException("DATABASE_URL must start with postgresql:// (got '" + scheme + "://')");
        }
        if (uri.getHost() == null) {
            throw new IllegalStateException("DATABASE_URL has no host");
        }

        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() != -1) {
            jdbc.append(':').append(uri.getPort());
        }
        jdbc.append(uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/postgres" : uri.getRawPath());

        // Keep driver options such as sslmode; drop libpq-only ones the JDBC driver doesn't understand.
        String query = uri.getRawQuery() == null ? "" : Arrays.stream(uri.getRawQuery().split("&"))
                .filter(p -> !p.isBlank() && !p.startsWith("channel_binding="))
                .collect(Collectors.joining("&"));
        if (!query.isEmpty()) {
            jdbc.append('?').append(query);
        }
        out.put("spring.datasource.url", jdbc.toString());

        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            String user = colon >= 0 ? userInfo.substring(0, colon) : userInfo;
            out.put("spring.datasource.username", decode(user));
            if (colon >= 0) {
                out.put("spring.datasource.password", decode(userInfo.substring(colon + 1)));
            }
        }
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
