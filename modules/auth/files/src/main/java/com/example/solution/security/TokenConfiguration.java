package com.example.solution.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration
public class TokenConfiguration {
    @Bean SecretKey signingKey(Environment environment) throws IOException {
        String encoded = environment.getProperty("APP_JWT_SECRET");
        if (encoded == null || encoded.isBlank()) {
            Path directory = Path.of(".local-data"); Files.createDirectories(directory);
            Path path = directory.resolve("jwt-secret");
            if (Files.notExists(path)) {
                byte[] random = new byte[64]; new SecureRandom().nextBytes(random);
                try {
                    Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                } catch (UnsupportedOperationException ignored) { Files.createFile(path); }
                Files.writeString(path, Base64.getEncoder().encodeToString(random));
            }
            encoded = Files.readString(path).trim();
        }
        byte[] bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length < 64) throw new IllegalStateException("JWT signing key must contain at least 64 random bytes");
        return new SecretKeySpec(bytes, "HmacSHA512");
    }
    @Bean JwtEncoder encoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder decoder(SecretKey key) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS512).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("solution")); return decoder;
    }
}
