package com.reviewassist.review;

import com.reviewassist.common.ApiException;
import org.springframework.http.HttpStatus;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window limit on reviews per user per minute, so one account can't burn through the AI quota.
 * In-memory and per instance - good enough for a single-node deployment.
 */
class ReviewRateLimiter {

    private static final long WINDOW_NANOS = 60_000_000_000L;

    private final int limit;
    private final Map<Long, Deque<Long>> calls = new ConcurrentHashMap<>();

    ReviewRateLimiter(int limitPerMinute) {
        this.limit = limitPerMinute;
    }

    void acquire(Long userId) {
        if (limit <= 0) {
            return; // disabled
        }
        long now = System.nanoTime();
        Deque<Long> window = calls.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() > WINDOW_NANOS) {
                window.pollFirst();
            }
            if (window.size() >= limit) {
                long waitSeconds = Math.max(1, (WINDOW_NANOS - (now - window.peekFirst())) / 1_000_000_000L);
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "You've reached " + limit + " reviews per minute. Try again in " + waitSeconds + "s.");
            }
            window.addLast(now);
        }
    }
}
