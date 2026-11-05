package com.reviewassist.auth;

import com.reviewassist.auth.AuthDtos.AuthResponse;
import com.reviewassist.auth.AuthDtos.LoginRequest;
import com.reviewassist.auth.AuthDtos.RegisterRequest;
import com.reviewassist.auth.AuthDtos.UserView;
import com.reviewassist.common.ApiException;
import com.reviewassist.security.AuthUser;
import com.reviewassist.security.JwtService;
import com.reviewassist.user.User;
import com.reviewassist.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String username = req.username().trim();
        String email = req.email().trim().toLowerCase();

        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "That username is already taken");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with that email already exists");
        }

        String displayName = req.displayName() == null ? null : req.displayName().trim();
        User user = users.save(new User(username, email, passwordEncoder.encode(req.password()), displayName));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String login = req.login().trim();
        User user = users.findByUsernameIgnoreCaseOrEmailIgnoreCase(login, login)
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public UserView me(AuthUser principal) {
        return users.findById(principal.id())
                .map(AuthService::view)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
    }

    private AuthResponse tokenFor(User user) {
        String token = jwtService.issue(new AuthUser(user.getId(), user.getUsername()));
        return new AuthResponse(token, jwtService.expiryFromNow(), view(user));
    }

    static UserView view(User u) {
        return new UserView(u.getId(), u.getUsername(), u.getEmail(), u.getDisplayName(), u.getCreatedAt());
    }
}
