package com.reviewassist.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank
            @Size(min = 3, max = 40)
            @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "may only contain letters, digits, '.', '_' and '-'")
            String username,

            @NotBlank @Email @Size(max = 254)
            String email,

            @NotBlank @Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
            String password,

            @Size(max = 80)
            String displayName) {
    }

    /** {@code login} may be either the username or the email address. */
    public record LoginRequest(@NotBlank String login, @NotBlank String password) {
    }

    public record UserView(Long id, String username, String email, String displayName, Instant joinedAt) {
    }

    public record AuthResponse(String token, Instant expiresAt, UserView user) {
    }
}
