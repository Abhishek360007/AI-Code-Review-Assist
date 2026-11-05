package com.reviewassist.review;

import com.reviewassist.ai.AnalysisResult;
import com.reviewassist.ai.CodeAnalyzer;
import com.reviewassist.common.ApiException;
import com.reviewassist.config.AppProperties;
import com.reviewassist.review.ReviewDtos.CreateReviewRequest;
import com.reviewassist.review.ReviewDtos.LanguageStat;
import com.reviewassist.review.ReviewDtos.PageView;
import com.reviewassist.review.ReviewDtos.ReviewDetail;
import com.reviewassist.review.ReviewDtos.ReviewListItem;
import com.reviewassist.review.ReviewDtos.ScorePoint;
import com.reviewassist.review.ReviewDtos.StatsView;
import com.reviewassist.security.AuthUser;
import com.reviewassist.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReviewService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewRepository reviews;
    private final UserRepository users;
    private final CodeAnalyzer analyzer;
    private final int maxCodeChars;
    private final ReviewRateLimiter rateLimiter;

    public ReviewService(ReviewRepository reviews, UserRepository users, CodeAnalyzer analyzer, AppProperties props) {
        this.reviews = reviews;
        this.users = users;
        this.analyzer = analyzer;
        this.maxCodeChars = props.review().maxCodeChars();
        this.rateLimiter = new ReviewRateLimiter(props.review().perMinuteLimit());
    }

    /**
     * Runs the analysis and stores the result. Deliberately not transactional: the AI call can take
     * many seconds and we don't want to hold a database connection open for it.
     */
    public ReviewDetail create(AuthUser user, CreateReviewRequest req) {
        String language = Languages.normalize(req.language());
        if (!Languages.isSupported(language)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported language '" + req.language() + "'");
        }
        String code = req.code().replace("\r\n", "\n");
        if (code.length() > maxCodeChars) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Code is " + code.length() + " characters; the limit is " + maxCodeChars);
        }

        String notes = blankToNull(req.notes());
        String title = blankToNull(req.title());
        if (title == null) {
            title = defaultTitle(language, code);
        }

        return ReviewDetail.of(runAndSave(new Review(users.getReferenceById(user.id()), title, language, code, notes)));
    }

    /** Reviews the same code again (e.g. after switching model) and stores it as a new review. */
    public ReviewDetail rerun(AuthUser user, Long id) {
        Review original = reviews.findByIdAndOwnerId(id, user.id()).orElseThrow(() -> ApiException.notFound("Review"));
        Review copy = new Review(users.getReferenceById(user.id()), original.getTitle(), original.getLanguage(),
                original.getCode(), original.getNotes());
        return ReviewDetail.of(runAndSave(copy));
    }

    private Review runAndSave(Review review) {
        rateLimiter.acquire(review.getOwner().getId());
        long start = System.nanoTime();
        AnalysisResult result = analyzer.analyze(review.getLanguage(), review.getCode(), review.getNotes());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        List<Finding> findings = result.issues().stream()
                .map(i -> new Finding(i.severity(), i.category(), i.line(), i.title(), i.detail(), i.fix()))
                .sorted((a, b) -> a.getSeverity().compareTo(b.getSeverity()))
                .toList();

        String improved = result.improvedCode() == null || result.improvedCode().isBlank()
                || result.improvedCode().strip().equals(review.getCode().strip()) ? null : result.improvedCode();

        review.applyResult(result.score(), result.summary(), improved, result.model(), elapsedMs,
                findings, result.strengths());
        return reviews.save(review);
    }

    @Transactional(readOnly = true)
    public ReviewDetail get(AuthUser user, Long id) {
        return reviews.findByIdAndOwnerId(id, user.id())
                .map(ReviewDetail::of)
                .orElseThrow(() -> ApiException.notFound("Review"));
    }

    @Transactional(readOnly = true)
    public PageView<ReviewListItem> list(AuthUser user, int page, int size, String language, String query) {
        Specification<Review> spec = (root, q, cb) -> cb.equal(root.get("owner").get("id"), user.id());
        if (language != null && !language.isBlank()) {
            String lang = Languages.normalize(language);
            spec = spec.and((root, q, cb) -> cb.equal(root.get("language"), lang));
        }
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("title")), like, '\\'));
        }

        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> result = reviews.findAll(spec, pageable);

        return new PageView<>(result.map(ReviewListItem::of).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public void delete(AuthUser user, Long id) {
        Review review = reviews.findByIdAndOwnerId(id, user.id()).orElseThrow(() -> ApiException.notFound("Review"));
        reviews.delete(review);
    }

    @Transactional(readOnly = true)
    public StatsView stats(AuthUser user) {
        Long uid = user.id();

        Map<Severity, Long> bySeverity = new EnumMap<>(Severity.class);
        for (Severity s : Severity.values()) {
            bySeverity.put(s, 0L);
        }
        reviews.countFindingsBySeverity(uid).forEach(row -> bySeverity.put((Severity) row[0], (Long) row[1]));

        Map<Category, Long> byCategory = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            byCategory.put(c, 0L);
        }
        reviews.countFindingsByCategory(uid).forEach(row -> byCategory.put((Category) row[0], (Long) row[1]));

        List<LanguageStat> languages = reviews.languageBreakdown(uid).stream()
                .map(row -> new LanguageStat((String) row[0], (Long) row[1], round1(((Number) row[2]).doubleValue())))
                .toList();

        // Oldest first so the UI can draw the trend left to right.
        List<ScorePoint> recent = new ArrayList<>(reviews.findTop20ByOwnerIdOrderByCreatedAtDesc(uid).stream()
                .map(r -> new ScorePoint(r.getId(), r.getCreatedAt(), r.getScore()))
                .toList());
        Collections.reverse(recent);

        Double avg = reviews.averageScore(uid);
        long totalFindings = bySeverity.values().stream().mapToLong(Long::longValue).sum();
        return new StatsView(reviews.countByOwnerId(uid), avg == null ? 0 : round1(avg), totalFindings,
                bySeverity, byCategory, languages, recent);
    }

    private static String defaultTitle(String language, String code) {
        String firstLine = code.lines().map(String::strip).filter(l -> !l.isEmpty()).findFirst().orElse("");
        if (firstLine.length() > 60) {
            firstLine = firstLine.substring(0, 57) + "...";
        }
        return firstLine.isEmpty() ? "Untitled " + language + " snippet" : firstLine;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
