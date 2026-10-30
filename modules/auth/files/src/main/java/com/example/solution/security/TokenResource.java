package com.example.solution.security;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TokenResource {
    public record Login(@NotBlank @Size(max = 50) String username, @NotBlank @Size(max = 2000) String password) {
        @JsonAnySetter public void unknown(String field, Object value) { throw new IllegalArgumentException("Unknown login field"); }
    }
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder encoder;
    public TokenResource(AuthenticationManager authenticationManager, JwtEncoder encoder) {
        this.authenticationManager = authenticationManager; this.encoder = encoder;
    }
    @PostMapping("/api/authenticate")
    public Map<String, String> login(@Valid @RequestBody Login input) {
        try {
            var authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(input.username(), input.password()));
            var now = Instant.now();
            var claims = JwtClaimsSet.builder().issuer("solution").subject(authentication.getName()).issuedAt(now).expiresAt(now.plusSeconds(1800))
                .claim("authorities", authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList()).build();
            var token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS512).build(), claims)).getTokenValue();
            return Map.of("id_token", token);
        } catch (AuthenticationException error) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"); }
    }
}
