package com.reviewassist.review;

import com.reviewassist.user.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "reviews", indexes = @Index(name = "idx_reviews_owner_created", columnList = "owner_id, created_at"))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 24)
    private String language;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private int score;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "improved_code", columnDefinition = "TEXT")
    private String improvedCode;

    @Column(nullable = false, length = 64)
    private String engine;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    /** Denormalized for cheap list views. */
    @Column(name = "issue_count", nullable = false)
    private int issueCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "worst_severity", length = 16)
    private Severity worstSeverity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "review_findings", joinColumns = @JoinColumn(name = "review_id"))
    @OrderColumn(name = "position")
    private List<Finding> findings = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "review_strengths", joinColumns = @JoinColumn(name = "review_id"))
    @OrderColumn(name = "position")
    @Column(name = "strength", length = 500)
    private List<String> strengths = new ArrayList<>();

    protected Review() {
    }

    public Review(User owner, String title, String language, String code, String notes) {
        this.owner = owner;
        this.title = title;
        this.language = language;
        this.code = code;
        this.notes = notes;
    }

    public void applyResult(int score, String summary, String improvedCode, String engine, long durationMs,
                            List<Finding> findings, List<String> strengths) {
        this.score = score;
        this.summary = summary;
        this.improvedCode = improvedCode;
        this.engine = engine;
        this.durationMs = durationMs;
        this.findings = new ArrayList<>(findings);
        this.strengths = new ArrayList<>(strengths.stream().map(s -> s.length() > 500 ? s.substring(0, 497) + "..." : s).toList());
        this.issueCount = findings.size();
        // Enum order is most to least severe, so the minimum ordinal is the worst.
        this.worstSeverity = findings.stream().map(Finding::getSeverity).min(Comparator.naturalOrder()).orElse(null);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public String getLanguage() {
        return language;
    }

    public String getCode() {
        return code;
    }

    public String getNotes() {
        return notes;
    }

    public int getScore() {
        return score;
    }

    public String getSummary() {
        return summary;
    }

    public String getImprovedCode() {
        return improvedCode;
    }

    public String getEngine() {
        return engine;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getIssueCount() {
        return issueCount;
    }

    public Severity getWorstSeverity() {
        return worstSeverity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Finding> getFindings() {
        return findings;
    }

    public List<String> getStrengths() {
        return strengths;
    }
}
