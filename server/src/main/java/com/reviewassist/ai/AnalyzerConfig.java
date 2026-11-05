package com.reviewassist.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewassist.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/** Picks the analyzer implementation based on {@code app.ai.provider}. */
@Configuration
public class AnalyzerConfig {

    private static final Logger log = LoggerFactory.getLogger(AnalyzerConfig.class);

    @Bean
    CodeAnalyzer codeAnalyzer(AppProperties props, ObjectMapper objectMapper) {
        AppProperties.Gemini gemini = props.ai().gemini();
        String provider = props.ai().provider() == null ? "auto" : props.ai().provider().toLowerCase(Locale.ROOT);

        return switch (provider) {
            case "gemini" -> {
                if (!gemini.hasKey()) {
                    throw new IllegalStateException("AI_PROVIDER=gemini but GEMINI_API_KEY is not set");
                }
                yield new GeminiAnalyzer(gemini, objectMapper);
            }
            case "demo" -> new DemoAnalyzer();
            case "auto" -> {
                if (gemini.hasKey()) {
                    yield new GeminiAnalyzer(gemini, objectMapper);
                }
                log.warn("GEMINI_API_KEY is not set - using the offline demo analyzer. "
                        + "Reviews will be heuristic, not AI-generated.");
                yield new DemoAnalyzer();
            }
            default -> throw new IllegalStateException("Unknown AI_PROVIDER '" + provider + "' (use auto, gemini or demo)");
        };
    }
}
