package com.reviewassist.ai;

import com.reviewassist.review.Category;
import com.reviewassist.review.Severity;

import java.util.List;

/** What an analyzer returns for one piece of code. */
public record AnalysisResult(
        int score,
        String summary,
        List<Issue> issues,
        List<String> strengths,
        String improvedCode,
        String model) {

    public record Issue(
            Severity severity,
            Category category,
            Integer line,
            String title,
            String detail,
            String fix) {
    }
}
