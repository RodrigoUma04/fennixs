package com.fennixs.auth.auth.service;

import static java.util.Date.from;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.fennixs.auth.config.AppProperties;
import com.fennixs.auth.config.AuthPrincipal;
import com.fennixs.auth.user.entity.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final AppProperties appProperties;
    private final SecretKey signingKey;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.signingKey =
                Keys.hmacShaKeyFor(appProperties.security().jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("java:S2143")
    public String generateAccessToken(AuthPrincipal principal) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(principal.userId().toString())
                .claim("email", principal.email())
                .claim("role", principal.role().name())
                .issuedAt(from(now))
                .expiration(from(now.plusMillis(appProperties.security().jwt().accessTokenExpirationMs())))
                .signWith(signingKey)
                .compact();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    public Role extractRole(String token) {
        return Role.valueOf(parseClaims(token).get("role", String.class));
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException _) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
