package com.fennixs.auth.auth.service;

import static com.fennixs.auth.common.crypto.HashUtils.sha256;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fennixs.auth.auth.dto.AuthTokens;
import com.fennixs.auth.auth.dto.LoginRequestDto;
import com.fennixs.auth.auth.dto.RegisterRequestDto;
import com.fennixs.auth.auth.entity.RefreshToken;
import com.fennixs.auth.auth.repository.RefreshTokenRepository;
import com.fennixs.auth.auth.service.SetupTokenService.Grant;
import com.fennixs.auth.common.exception.AuthException;
import com.fennixs.auth.common.exception.BusinessException;
import com.fennixs.auth.config.AppProperties;
import com.fennixs.auth.config.AuthPrincipal;
import com.fennixs.auth.user.entity.Role;
import com.fennixs.auth.user.entity.User;
import com.fennixs.auth.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final SetupTokenService setupTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthTokens register(RegisterRequestDto request) {
        Grant grant = setupTokenService.consume(request.setupToken());
        if (grant == Grant.DENIED) throw new AuthException("Invalid or missing setup token", HttpStatus.FORBIDDEN);

        try {
            Role role = grant == Grant.SETUP_TOKEN ? Role.OWNER : Role.USER;

            User user = createUser(normalizeEmail(request.email()), request.password(), role);

            AuthTokens tokens = issueTokens(user);

            log.info("user_id={} registered", user.getId());

            return tokens;
        } catch (RuntimeException e) {
            setupTokenService.restore(request.setupToken());
            throw e;
        }
    }

    @Transactional
    public AuthTokens login(LoginRequestDto request) {
        User user = userRepository
                .findByEmail(normalizeEmail(request.email()))
                .filter(u -> u.matchesPassword(request.password(), passwordEncoder))
                .orElseThrow(() -> new AuthException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        log.info("user_id={} logged in", user.getId());
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = AuthException.class)
    public AuthTokens refresh(String rawToken) {
        if (rawToken == null) throw new AuthException("Refresh token missing", HttpStatus.UNAUTHORIZED);

        String hash = sha256(rawToken);
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> new AuthException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new AuthException("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }

        refreshTokenRepository.delete(stored);
        User user = stored.getUser();
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null) return;
        refreshTokenRepository.findByTokenHash(sha256(rawToken)).ifPresent(refreshTokenRepository::delete);
    }

    // region Helper methods

    private User createUser(String email, String password, Role role) {
        if (userRepository.existsByEmail(email))
            throw new BusinessException("A resource with the provided data already exists", HttpStatus.CONFLICT);

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build();

        userRepository.saveAndFlush(user);

        return user;
    }

    private AuthTokens issueTokens(User user) {
        AuthPrincipal principal = new AuthPrincipal(user.getId(), user.getEmail(), user.getRole());
        String accessToken = jwtService.generateAccessToken(principal);
        String rawRefreshToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(rawRefreshToken))
                .expiresAt(
                        Instant.now().plusMillis(appProperties.security().jwt().refreshTokenExpirationMs()))
                .build();
        refreshTokenRepository.save(refreshToken);
        return new AuthTokens(accessToken, rawRefreshToken);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
    // endregion
}
