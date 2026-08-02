package com.fennixs.auth.auth.controller;

import static com.fennixs.auth.config.CookieNames.ACCESS_TOKEN;
import static com.fennixs.auth.config.CookieNames.ACCESS_TOKEN_PATH;
import static com.fennixs.auth.config.CookieNames.REFRESH_TOKEN;
import static com.fennixs.auth.config.CookieNames.REFRESH_TOKEN_PATH;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import com.fennixs.auth.auth.dto.AuthTokens;
import com.fennixs.auth.auth.dto.LoginRequestDto;
import com.fennixs.auth.auth.dto.RegisterRequestDto;
import com.fennixs.auth.auth.service.AuthService;
import com.fennixs.auth.common.exception.AuthException;
import com.fennixs.auth.config.AppProperties;
import com.fennixs.auth.config.AuthPrincipal;
import com.fennixs.auth.generated.api.AuthApi;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final AuthService service;
    private final AppProperties appProperties;

    private static final String SAME_SITE = "Strict";

    @Override
    public ResponseEntity<Void> register(RegisterRequestDto request) {
        AuthTokens tokens = service.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie(tokens.accessToken()).toString())
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(tokens.refreshToken()).toString())
                .build();
    }

    @Override
    public ResponseEntity<Void> login(LoginRequestDto request) {
        AuthTokens tokens = service.login(request);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie(tokens.accessToken()).toString())
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(tokens.refreshToken()).toString())
                .build();
    }

    @Override
    public ResponseEntity<Void> verify() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth != null && auth.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new AuthException("Not authenticated", HttpStatus.UNAUTHORIZED);
        }

        return ResponseEntity.ok()
                .header("X-User-Id", principal.userId().toString())
                .header("X-User-Role", principal.role().name())
                .build();
    }

    @Override
    public ResponseEntity<Void> refreshToken(String refreshToken) {
        AuthTokens tokens = service.refresh(refreshToken);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie(tokens.accessToken()).toString())
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(tokens.refreshToken()).toString())
                .build();
    }

    @Override
    public ResponseEntity<Void> logout(String refreshToken) {
        service.logout(refreshToken);

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearCookie(ACCESS_TOKEN, ACCESS_TOKEN_PATH).toString())
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearCookie(REFRESH_TOKEN, REFRESH_TOKEN_PATH).toString())
                .build();
    }

    private ResponseCookie accessCookie(String rawToken) {
        return ResponseCookie.from(ACCESS_TOKEN, rawToken)
                .httpOnly(true)
                .secure(appProperties.security().cookieSecure())
                .sameSite(SAME_SITE)
                .path(ACCESS_TOKEN_PATH)
                .maxAge(Duration.ofMillis(appProperties.security().jwt().accessTokenExpirationMs()))
                .build();
    }

    private ResponseCookie refreshCookie(String rawToken) {
        return ResponseCookie.from(REFRESH_TOKEN, rawToken)
                .httpOnly(true)
                .secure(appProperties.security().cookieSecure())
                .sameSite(SAME_SITE)
                .path(REFRESH_TOKEN_PATH)
                .maxAge(Duration.ofMillis(appProperties.security().jwt().refreshTokenExpirationMs()))
                .build();
    }

    private ResponseCookie clearCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(appProperties.security().cookieSecure())
                .sameSite(SAME_SITE)
                .path(path)
                .maxAge(0)
                .build();
    }
}
