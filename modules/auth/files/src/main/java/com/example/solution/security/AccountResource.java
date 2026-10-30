package com.example.solution.security;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccountResource {
    @GetMapping("/api/account")
    public Map<String, Object> account(Authentication authentication) {
        return Map.of("login", authentication.getName(), "authorities",
            authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList());
    }
}
