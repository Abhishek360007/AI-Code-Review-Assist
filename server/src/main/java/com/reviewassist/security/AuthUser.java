package com.reviewassist.security;

/** The authenticated caller, resolved from the JWT and exposed via {@code @AuthenticationPrincipal}. */
public record AuthUser(Long id, String username) {
}
