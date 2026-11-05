package com.reviewassist.ai;

/** Strategy for reviewing code. Implementations: {@link GeminiAnalyzer} and {@link DemoAnalyzer}. */
public interface CodeAnalyzer {

    AnalysisResult analyze(String language, String code, String context);

    /** Short human-readable name of the backing engine, shown in the UI. */
    String engineName();
}
