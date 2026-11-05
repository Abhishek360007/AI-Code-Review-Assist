package com.reviewassist.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void convertsNeonStyleUrl() {
        Map<String, Object> props = new HashMap<>();
        DatabaseUrlEnvironmentPostProcessor.toJdbc(
                "postgresql://neondb_owner:p%40ss@ep-cool-name-123456-pooler.us-west-2.aws.neon.tech/neondb"
                        + "?sslmode=require&channel_binding=require", props);

        assertThat(props).containsEntry("spring.datasource.url",
                "jdbc:postgresql://ep-cool-name-123456-pooler.us-west-2.aws.neon.tech/neondb?sslmode=require");
        assertThat(props).containsEntry("spring.datasource.username", "neondb_owner");
        assertThat(props).containsEntry("spring.datasource.password", "p@ss");
    }

    @Test
    void keepsPortAndAcceptsPostgresScheme() {
        Map<String, Object> props = new HashMap<>();
        DatabaseUrlEnvironmentPostProcessor.toJdbc("postgres://u:p@db.example.com:6543/app", props);
        assertThat(props).containsEntry("spring.datasource.url", "jdbc:postgresql://db.example.com:6543/app");
    }

    @Test
    void rejectsNonPostgresUrls() {
        assertThatThrownBy(() -> DatabaseUrlEnvironmentPostProcessor.toJdbc("mysql://u:p@h/db", new HashMap<>()))
                .hasMessageContaining("postgresql://");
    }
}
