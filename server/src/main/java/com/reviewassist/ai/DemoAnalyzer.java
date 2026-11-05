package com.reviewassist.ai;

import com.reviewassist.ai.AnalysisResult.Issue;
import com.reviewassist.review.Category;
import com.reviewassist.review.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Offline, rule-based analyzer used when no AI key is configured (local demos, tests, CI).
 * It only catches a handful of common smells - it is a stand-in, not a replacement for the AI review.
 */
public class DemoAnalyzer implements CodeAnalyzer {

    private record Rule(Pattern pattern, Set<String> languages, Severity severity, Category category,
                        String title, String detail, String fix) {
        boolean appliesTo(String language) {
            return languages.isEmpty() || languages.contains(language);
        }
    }

    private static final Set<String> ANY = Set.of();
    private static final Set<String> JS = Set.of("javascript", "typescript");

    private static final List<Rule> RULES = List.of(
            new Rule(Pattern.compile("(?i)(password|passwd|secret|api[_-]?key|token)\\s*[:=]\\s*[\"'][^\"']{4,}[\"']"), ANY,
                    Severity.CRITICAL, Category.SECURITY, "Hard-coded secret",
                    "A credential appears to be committed in source code. Anyone with repository access can read it.",
                    "Load the value from an environment variable or a secrets manager instead."),
            new Rule(Pattern.compile("(?i)[\"'](select|insert|update|delete)\\b[^\"']*[\"']\\s*\\+"), ANY,
                    Severity.HIGH, Category.SECURITY, "SQL built by string concatenation",
                    "Concatenating input into SQL allows SQL injection.",
                    "Use parameterized queries / prepared statements."),
            new Rule(Pattern.compile("\\beval\\s*\\("), ANY,
                    Severity.HIGH, Category.SECURITY, "Use of eval",
                    "eval executes arbitrary code and is a common injection vector.",
                    "Parse the data explicitly (e.g. JSON.parse / ast.literal_eval) instead of evaluating it."),
            new Rule(Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{\\s*}|except[^:]*:\\s*pass\\b"), ANY,
                    Severity.MEDIUM, Category.BUG, "Exception silently swallowed",
                    "Errors are caught and ignored, which hides failures and makes debugging hard.",
                    "Log the error, handle it meaningfully, or let it propagate."),
            new Rule(Pattern.compile("[^=!]==[^=]"), JS,
                    Severity.LOW, Category.BUG, "Loose equality (==)",
                    "== performs type coercion and can produce surprising results.",
                    "Use === (and !==) for comparisons."),
            new Rule(Pattern.compile("\\bvar\\s+\\w+"), JS,
                    Severity.LOW, Category.STYLE, "var declaration",
                    "var is function-scoped and hoisted, which can cause subtle bugs.",
                    "Use const by default, or let when the variable is reassigned."),
            new Rule(Pattern.compile("System\\.out\\.print|console\\.log\\(|\\bprint\\("), ANY,
                    Severity.LOW, Category.MAINTAINABILITY, "Debug output left in code",
                    "Print statements are noisy in production and cannot be filtered by level.",
                    "Use the project's logger with an appropriate log level."),
            new Rule(Pattern.compile("(?i)\\b(TODO|FIXME|HACK)\\b"), ANY,
                    Severity.INFO, Category.MAINTAINABILITY, "Unresolved TODO",
                    "There is an open TODO/FIXME marker in this code.",
                    "Resolve it or track it in your issue tracker."));

    @Override
    public String engineName() {
        return "demo-rules";
    }

    @Override
    public AnalysisResult analyze(String language, String code, String context) {
        String lang = language.toLowerCase(Locale.ROOT);
        String[] lines = code.split("\\R", -1);
        List<Issue> issues = new ArrayList<>();

        for (Rule rule : RULES) {
            if (!rule.appliesTo(lang)) {
                continue;
            }
            for (int i = 0; i < lines.length; i++) {
                if (rule.pattern().matcher(lines[i]).find()) {
                    issues.add(new Issue(rule.severity(), rule.category(), i + 1, rule.title(), rule.detail(), rule.fix()));
                    break; // one finding per rule keeps the demo output readable
                }
            }
        }

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() > 120) {
                issues.add(new Issue(Severity.LOW, Category.STYLE, i + 1, "Very long line",
                        "Lines longer than 120 characters are hard to read and review.",
                        "Break the expression up or extract a helper variable."));
                break;
            }
        }

        int penalty = issues.stream().mapToInt(i -> i.severity().penalty()).sum();
        int score = Math.max(5, 100 - penalty);
        String summary = issues.isEmpty()
                ? "Demo mode: no common problems were detected by the built-in rules. Set GEMINI_API_KEY for a full AI review."
                : "Demo mode: the built-in rules found " + issues.size() + " potential problem(s). "
                + "Set GEMINI_API_KEY for a full AI review.";

        return new AnalysisResult(score, summary, issues, List.of(), "", engineName());
    }
}
