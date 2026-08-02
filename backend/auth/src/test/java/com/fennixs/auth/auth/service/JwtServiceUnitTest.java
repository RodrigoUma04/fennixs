package com.fennixs.auth.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fennixs.auth.config.AppProperties;
import com.fennixs.auth.config.AuthPrincipal;
import com.fennixs.auth.user.entity.Role;

class JwtServiceUnitTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "user@fennixs.com";
    private static final AuthPrincipal PRINCIPAL = new AuthPrincipal(USER_ID, EMAIL, Role.OWNER);

    private final JwtService jwtService = serviceWith(900_000L, "unit-test-signing-secret-at-least-32-bytes");

    // region generateAccessToken()
    @Test
    void givenPrincipal_whenGenerateAccessToken_thenClaimsRoundTripCorrectly() {
        // Act
        String token = jwtService.generateAccessToken(PRINCIPAL);

        // Assert
        assertThat(jwtService.extractSubject(token)).isEqualTo(USER_ID.toString());
        assertThat(jwtService.extractEmail(token)).isEqualTo(EMAIL);
        assertThat(jwtService.extractRole(token)).isEqualTo(Role.OWNER);
        assertThat(jwtService.isValid(token)).isTrue();
    }
    // endregion

    // region isValid()
    @Test
    void givenExpiredToken_whenIsValid_thenReturnFalse() {
        // Arrange
        JwtService expiredTokenIssuer = serviceWith(-1L, "unit-test-signing-secret-at-least-32-bytes");
        String token = expiredTokenIssuer.generateAccessToken(PRINCIPAL);

        // Act & Assert
        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void givenTokenSignedWithDifferentSecret_whenIsValid_thenReturnFalse() {
        // Arrange
        JwtService otherIssuer = serviceWith(900_000L, "a-completely-different-signing-secret-value");
        String token = otherIssuer.generateAccessToken(PRINCIPAL);

        // Act & Assert
        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void givenMalformedToken_whenIsValid_thenReturnFalse() {
        assertThat(jwtService.isValid("not-a-jwt")).isFalse();
    }
    // endregion

    private static JwtService serviceWith(long accessTokenExpirationMs, String secret) {
        var jwtProperties =
                new AppProperties.SecurityProperties.JwtProperties(secret, accessTokenExpirationMs, 2_592_000_000L);
        var securityProperties = new AppProperties.SecurityProperties(jwtProperties, false, List.of());
        return new JwtService(new AppProperties(null, securityProperties));
    }
}
