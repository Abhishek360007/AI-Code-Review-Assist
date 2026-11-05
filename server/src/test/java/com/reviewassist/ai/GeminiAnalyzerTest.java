package com.reviewassist.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiAnalyzerTest {

    @Test
    void stripsMarkdownFencesFromImprovedCode() {
        assertThat(GeminiAnalyzer.stripFences("```java\nint x = 1;\n```")).isEqualTo("int x = 1;");
        assertThat(GeminiAnalyzer.stripFences("int x = 1;")).isEqualTo("int x = 1;");
        assertThat(GeminiAnalyzer.stripFences("")).isEmpty();
    }
}
