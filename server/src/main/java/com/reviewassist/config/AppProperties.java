package com.reviewassist.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Review review, Ai ai) {

    public record Jwt(String secret, Duration ttl) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Review(int maxCodeChars, int perMinuteLimit) {
    }

    public record Ai(String provider, Gemini gemini) {
    }

    public record Gemini(String apiKey, String model, List<String> fallbackModels, String baseUrl, Duration timeout) {
        public boolean hasKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
