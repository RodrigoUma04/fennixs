package com.fennixs.coreapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fennixs.coreapi.TestOidcConfiguration;
import com.fennixs.coreapi.TestcontainersConfiguration;

@Import({TestcontainersConfiguration.class, TestOidcConfiguration.class})
@SpringBootTest
class FennixsOidcUserServiceTests {
    @Autowired
    FennixsOidcUserService service;

    @Autowired
    ClientRegistrationRepository registrations;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void givenValidIdToken_whenLoadingUser_thenReturnsFennixsUserWithInternalId() {
        // arrange
        String subject = newSubject();

        // act
        OidcUser user = service.loadUser(request(newIssuer(), subject, "a@example.com"));

        // assert
        assertThat(user).isInstanceOf(FennixsOidcUser.class);
        assertThat(((FennixsOidcUser) user).getUserId()).isNotNull();
        assertThat(user.getSubject()).isEqualTo(subject);
    }

    @Test
    void givenSameIdentity_whenLoadingUserTwice_thenReturnsTheSameInternalId() {
        // arrange
        String issuer = newIssuer();
        String subject = newSubject();
        FennixsOidcUser first = (FennixsOidcUser) service.loadUser(request(issuer, subject, null));

        // act
        FennixsOidcUser second = (FennixsOidcUser) service.loadUser(request(issuer, subject, null));

        // assert
        assertThat(second.getUserId()).isEqualTo(first.getUserId());
    }

    @Test
    void givenDisabledUser_whenLoadingUser_thenLoginFailsWithAccessDenied() {
        // arrange
        String issuer = newIssuer();
        String subject = newSubject();
        service.loadUser(request(issuer, subject, null));
        disable(issuer, subject);

        // act
        Throwable thrown = catchThrowable(() -> service.loadUser(request(issuer, subject, null)));

        // assert
        assertThat(thrown)
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(
                        ex -> ((OAuth2AuthenticationException) ex).getError().getErrorCode())
                .isEqualTo(OAuth2ErrorCodes.ACCESS_DENIED);
    }

    private OidcUserRequest request(String issuer, String subject, String email) {
        Instant now = Instant.now();
        OidcIdToken.Builder idToken = OidcIdToken.withTokenValue("id-token")
                .issuer(issuer)
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60));
        if (email != null) {
            idToken.claim("email", email);
        }
        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token", now, now.plusSeconds(60));
        return new OidcUserRequest(registrations.findByRegistrationId("oidc"), accessToken, idToken.build());
    }

    private void disable(String issuer, String subject) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.queryForMap(
                    "SELECT set_config('app.login_iss', ?, true), set_config('app.login_sub', ?, true)",
                    issuer,
                    subject);
            jdbc.update("UPDATE users SET enabled = false WHERE issuer = ? AND subject = ?", issuer, subject);
        });
    }

    private static String newIssuer() {
        return "https://issuer.test/" + UUID.randomUUID();
    }

    private static String newSubject() {
        return UUID.randomUUID().toString();
    }
}
