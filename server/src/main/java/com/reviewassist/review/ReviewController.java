package com.reviewassist.review;

import com.reviewassist.ai.CodeAnalyzer;
import com.reviewassist.ai.DemoAnalyzer;
import com.reviewassist.config.AppProperties;
import com.reviewassist.review.ReviewDtos.CreateReviewRequest;
import com.reviewassist.review.ReviewDtos.MetaView;
import com.reviewassist.review.ReviewDtos.PageView;
import com.reviewassist.review.ReviewDtos.ReviewDetail;
import com.reviewassist.review.ReviewDtos.ReviewListItem;
import com.reviewassist.review.ReviewDtos.StatsView;
import com.reviewassist.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService service;
    private final CodeAnalyzer analyzer;
    private final AppProperties props;

    public ReviewController(ReviewService service, CodeAnalyzer analyzer, AppProperties props) {
        this.service = service;
        this.analyzer = analyzer;
        this.props = props;
    }

    /** Public: lets the UI show which engine is active and the size limit before the user logs in. */
    @GetMapping("/meta")
    public MetaView meta() {
        return new MetaView(analyzer.engineName(), !(analyzer instanceof DemoAnalyzer),
                props.review().maxCodeChars(), Languages.SUPPORTED);
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDetail create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreateReviewRequest request) {
        return service.create(user, request);
    }

    @GetMapping("/reviews")
    public PageView<ReviewListItem> list(@AuthenticationPrincipal AuthUser user,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String language,
                                         @RequestParam(required = false) String q) {
        return service.list(user, page, size, language, q);
    }

    @GetMapping("/reviews/stats")
    public StatsView stats(@AuthenticationPrincipal AuthUser user) {
        return service.stats(user);
    }

    @GetMapping("/reviews/{id}")
    public ReviewDetail get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.get(user, id);
    }

    @PostMapping("/reviews/{id}/rerun")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDetail rerun(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.rerun(user, id);
    }

    @DeleteMapping("/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        service.delete(user, id);
    }
}
