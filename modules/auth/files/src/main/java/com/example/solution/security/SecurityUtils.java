package com.example.solution.security;

import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {}
    public static Optional<String> getCurrentUserLogin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken
            ? Optional.empty() : Optional.of(authentication.getName());
    }
    public static boolean hasCurrentUserThisAuthority(String role) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return getCurrentUserLogin().isPresent() && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
