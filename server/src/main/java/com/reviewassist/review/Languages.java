package com.reviewassist.review;

import java.util.List;
import java.util.Locale;

/** Languages accepted by the API. Ids match Monaco editor language ids so the UI can use them directly. */
public final class Languages {

    public static final List<String> SUPPORTED = List.of(
            "java", "python", "javascript", "typescript", "cpp", "c", "csharp", "go",
            "rust", "kotlin", "php", "ruby", "swift", "sql", "shell", "plaintext");

    private Languages() {
    }

    public static boolean isSupported(String language) {
        return language != null && SUPPORTED.contains(normalize(language));
    }

    public static String normalize(String language) {
        return language.trim().toLowerCase(Locale.ROOT);
    }
}
