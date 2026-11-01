package com.fennixs.coreapi.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fennixs.coreapi.TestOidcConfiguration;
import com.fennixs.coreapi.TestcontainersConfiguration;

@Import({TestcontainersConfiguration.class, TestOidcConfiguration.class})
@SpringBootTest(
        properties = {"spring.datasource.hikari.maximum-pool-size=1", "spring.datasource.hikari.connection-timeout=2000"
        })
class UserResolutionServiceTests {
    @Autowired
    UserResolutionService service;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void givenUnknownIdentity_whenResolving_thenCreatesEnabledUserWithTimestamps() {
        // act
        User user = service.resolve(newIssuer(), newSubject(), "a@example.com");

        // assert
        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("a@example.com");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void givenKnownIdentity_whenResolvingAgain_thenReturnsTheSameUser() {
        // arrange
        String issuer = newIssuer();
        String subject = newSubject();
        User first = service.resolve(issuer, subject, "a@example.com");

        // act
        User second = service.resolve(issuer, subject, "a@example.com");

        // assert
        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    void givenKnownIdentity_whenResolvingWithNewEmail_thenStoresNewEmail() {
        // arrange
        String issuer = newIssuer();
        String subject = newSubject();
        User first = service.resolve(issuer, subject, "old@example.com");

        // act
        User second = service.resolve(issuer, subject, "new@example.com");

        // assert
        assertThat(storedEmail(issuer, subject)).isEqualTo("new@example.com");
        assertThat(second.getUpdatedAt()).isAfter(first.getUpdatedAt());
        assertThat(second.getCreatedAt()).isEqualTo(first.getCreatedAt());
    }

    @Test
    void givenKnownIdentity_whenResolvingWithSameEmail_thenLeavesUpdatedAtUnchanged() {
        // arrange
        String issuer = newIssuer();
        String subject = newSubject();
        service.resolve(issuer, subject, "a@example.com");
        User loaded = service.resolve(issuer, subject, "a@example.com");

        // act
        User again = service.resolve(issuer, subject, "a@example.com");

        // assert
        assertThat(again.getUpdatedAt()).isEqualTo(loaded.getUpdatedAt());
    }

    @Test
    void givenKnownIdentity_whenTokenHasNoEmail_thenStoresNoEmail() {
        // arrange
        String issuer = newIssuer();
        String subject = newSubject();
        service.resolve(issuer, subject, "a@example.com");

        // act
        service.resolve(issuer, subject, null);

        // assert
        assertThat(storedEmail(issuer, subject)).isNull();
    }

    @Test
    void givenSameSubjectFromAnotherIssuer_whenResolving_thenCreatesSeparateUser() {
        // arrange
        String subject = newSubject();
        User first = service.resolve(newIssuer(), subject, null);

        // act
        User second = service.resolve(newIssuer(), subject, null);

        // assert
        assertThat(second.getId()).isNotEqualTo(first.getId());
    }

    @Test
    void givenResolvedUser_whenNextTransactionRuns_thenLoginSettingsAreEmpty() {
        // arrange
        service.resolve(newIssuer(), newSubject(), null);

        // act
        String settings = new TransactionTemplate(transactionManager)
                .execute(status -> jdbc.queryForObject(
                        "SELECT current_setting('app.login_iss', true) || current_setting('app.login_sub', true)",
                        String.class));

        // assert
        assertThat(settings).isNullOrEmpty();
    }

    private @Nullable String storedEmail(String issuer, String subject) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            jdbc.queryForMap(
                    "SELECT set_config('app.login_iss', ?, true), set_config('app.login_sub', ?, true)",
                    issuer,
                    subject);
            return jdbc.queryForObject(
                    "SELECT email FROM users WHERE issuer = ? AND subject = ?", String.class, issuer, subject);
        });
    }

    private static String newIssuer() {
        return "https://issuer.test/" + UUID.randomUUID();
    }

    private static String newSubject() {
        return UUID.randomUUID().toString();
    }
}
