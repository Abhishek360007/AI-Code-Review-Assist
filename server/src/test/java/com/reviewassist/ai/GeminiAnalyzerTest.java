package com.reviewassist.ai;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiAnalyzerTest {

    @Test
    void stripsMarkdownFencesFromImprovedCode() {
        assertThat(GeminiAnalyzer.stripFences("```java\nint x = 1;\n```")).isEqualTo("int x = 1;");
        assertThat(GeminiAnalyzer.stripFences("int x = 1;")).isEqualTo("int x = 1;");
        assertThat(GeminiAnalyzer.stripFences("")).isEmpty();
    }

    @Test
    void keepsOnlyLineNumbersThatExistInTheFile() {
        var json = JsonNodeFactory.instance;
        assertThat(GeminiAnalyzer.validLine(json.numberNode(4), 10)).isEqualTo(4);
        assertThat(GeminiAnalyzer.validLine(json.numberNode(10), 10)).isEqualTo(10);
        assertThat(GeminiAnalyzer.validLine(json.numberNode(11), 10)).isNull(); // past the end
        assertThat(GeminiAnalyzer.validLine(json.numberNode(0), 10)).isNull();
        assertThat(GeminiAnalyzer.validLine(json.nullNode(), 10)).isNull();     // file-wide
        assertThat(GeminiAnalyzer.validLine(json.textNode("seven"), 10)).isNull();
    }

    @Test
    void detectsImprovedCodeCollapsedOntoOneLine() {
        String collapsed = "async function f(db, id) { const rows = await db.query('...', [id]); return rows; }";
        String proper = String.join("\n", "async function f(db, id) {", "  const rows = await db.query('...', [id]);",
                "  let total = 0;", "  for (const r of rows) total += r.amount;", "  return total;", "}");
        assertThat(GeminiAnalyzer.looksCollapsed(collapsed, 14)).isTrue();
        assertThat(GeminiAnalyzer.looksCollapsed(proper, 14)).isFalse();
        assertThat(GeminiAnalyzer.looksCollapsed("x = 1", 3)).isFalse(); // tiny inputs are left alone
        assertThat(GeminiAnalyzer.looksCollapsed("", 20)).isFalse();
    }
}
