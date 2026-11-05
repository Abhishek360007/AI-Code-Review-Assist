package com.reviewassist.review;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** One issue found during a review. Stored as an element collection of {@link Review}. */
@Embeddable
public class Finding {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Category category;

    @Column(name = "line_number")
    private Integer line;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(columnDefinition = "TEXT")
    private String fix;

    protected Finding() {
    }

    public Finding(Severity severity, Category category, Integer line, String title, String detail, String fix) {
        this.severity = severity;
        this.category = category;
        this.line = line;
        this.title = title.length() > 300 ? title.substring(0, 297) + "..." : title;
        this.detail = detail;
        this.fix = fix;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Category getCategory() {
        return category;
    }

    public Integer getLine() {
        return line;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public String getFix() {
        return fix;
    }
}
