package com.reviewassist.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record CreateReviewRequest(
            @Size(max = 120) String title,
            @NotBlank String language,
            @NotBlank String code,
            @Size(max = 1000) String notes) {
    }

    public record FindingView(Severity severity, Category category, Integer line, String title, String detail, String fix) {
        static FindingView of(Finding f) {
            return new FindingView(f.getSeverity(), f.getCategory(), f.getLine(), f.getTitle(), f.getDetail(), f.getFix());
        }
    }

    public record ReviewDetail(
            Long id, String title, String language, String code, String notes,
            int score, String summary, List<FindingView> findings, List<String> strengths, String improvedCode,
            String engine, long durationMs, Instant createdAt) {

        static ReviewDetail of(Review r) {
            return new ReviewDetail(r.getId(), r.getTitle(), r.getLanguage(), r.getCode(), r.getNotes(),
                    r.getScore(), r.getSummary(), r.getFindings().stream().map(FindingView::of).toList(),
                    List.copyOf(r.getStrengths()), r.getImprovedCode(), r.getEngine(), r.getDurationMs(), r.getCreatedAt());
        }
    }

    public record ReviewListItem(
            Long id, String title, String language, int score, int issueCount, Severity worstSeverity,
            int lineCount, Instant createdAt) {

        static ReviewListItem of(Review r) {
            return new ReviewListItem(r.getId(), r.getTitle(), r.getLanguage(), r.getScore(), r.getIssueCount(),
                    r.getWorstSeverity(), (int) r.getCode().lines().count(), r.getCreatedAt());
        }
    }

    public record PageView<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }

    public record LanguageStat(String language, long reviews, double averageScore) {
    }

    public record ScorePoint(Long reviewId, Instant at, int score) {
    }

    public record StatsView(
            long totalReviews,
            double averageScore,
            long totalFindings,
            Map<Severity, Long> findingsBySeverity,
            Map<Category, Long> findingsByCategory,
            List<LanguageStat> languages,
            List<ScorePoint> recentScores) {
    }

    public record MetaView(String engine, boolean aiEnabled, int maxCodeChars, List<String> languages) {
    }
}
