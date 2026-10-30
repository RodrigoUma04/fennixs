package com.fennixs.coreapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.fennixs.coreapi.security.FennixsOidcUser;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        properties = {"spring.datasource.hikari.maximum-pool-size=1", "spring.datasource.hikari.connection-timeout=2000"
        })
class RlsTransactionManagerTests {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    PostgreSQLContainer postgres;

    @AfterEach
    void clearSecurityContext() throws SQLException {
        SecurityContextHolder.clearContext();
        asSuperuser("GRANT EXECUTE ON FUNCTION set_config(text, text, boolean) TO PUBLIC");
    }

    @Test
    void givenSetConfigFails_whenTransactionBegins_thenFailsAndReleasesTransaction() throws SQLException {
        // arrange
        signIn(UUID.randomUUID());
        asSuperuser("REVOKE EXECUTE ON FUNCTION set_config(text, text, boolean) FROM PUBLIC");

        // act
        Throwable thrown = catchThrowable(() -> inTransaction("SELECT 1"));
        asSuperuser("GRANT EXECUTE ON FUNCTION set_config(text, text, boolean) TO PUBLIC");

        // assert
        assertThat(thrown)
                .isInstanceOf(CannotCreateTransactionException.class)
                .rootCause()
                .hasMessageContaining("permission denied for function set_config");
        assertThat(TransactionSynchronizationManager.hasResource(entityManagerFactory))
                .isFalse();
        assertThat(inTransaction("SELECT current_setting('app.user_id', true)")).isNotEmpty();
    }

    @Test
    void givenSignedInUser_whenTransactionBegins_thenUserIdIsSet() {
        // arrange
        UUID userId = UUID.randomUUID();
        signIn(userId);

        // act
        String setting = inTransaction("SELECT current_setting('app.user_id', true)");

        // assert
        assertThat(setting).isEqualTo(userId.toString());
    }

    @Test
    void givenNoSignedInUser_whenTransactionBegins_thenUserIdIsUnset() {
        // act
        String setting = inTransaction("SELECT current_setting('app.user_id', true)");

        // assert
        assertThat(setting).isNullOrEmpty();
    }

    @Test
    void givenSeveralUsers_whenReadingUsers_thenSeesOnlyOwnRow() {
        // arrange
        createUser();
        signIn(createUser());

        // act
        String count = inTransaction("SELECT count(*) FROM users");

        // assert
        assertThat(count).isEqualTo("1");
    }

    @Test
    void givenCommittedTransaction_whenNextTransactionRunsWithoutUser_thenSeesNoRows() {
        // arrange
        signIn(createUser());
        String pidWithUser = inTransaction("SELECT pg_backend_pid()");
        SecurityContextHolder.clearContext();

        // act
        String pidWithoutUser = inTransaction("SELECT pg_backend_pid()");
        String count = inTransaction("SELECT count(*) FROM users");

        // assert
        assertThat(pidWithoutUser).isEqualTo(pidWithUser);
        assertThat(count).isEqualTo("0");
    }

    private void asSuperuser(String sql) throws SQLException {
        String url = "jdbc:postgresql://%s:%d/fennixs_core".formatted(postgres.getHost(), postgres.getMappedPort(5432));
        try (Connection connection = DriverManager.getConnection(url, postgres.getUsername(), postgres.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private UUID createUser() {
        String subject = UUID.randomUUID().toString();
        return new TransactionTemplate(transactionManager).execute(status -> {
            jdbc.queryForMap(
                    "SELECT set_config('app.login_iss', 'test', true), set_config('app.login_sub', ?, true)", subject);
            return jdbc.queryForObject(
                    "INSERT INTO users (issuer, subject) VALUES ('test', ?) RETURNING id", UUID.class, subject);
        });
    }

    private String inTransaction(String sql) {
        return new TransactionTemplate(transactionManager).execute(status -> jdbc.queryForObject(sql, String.class));
    }

    private static void signIn(UUID userId) {
        OidcIdToken idToken = OidcIdToken.withTokenValue("token")
                .subject("subject")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        FennixsOidcUser user = new FennixsOidcUser(new DefaultOidcUser(AuthorityUtils.NO_AUTHORITIES, idToken), userId);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(user, null));
    }
}
