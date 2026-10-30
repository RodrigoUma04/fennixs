package com.fennixs.coreapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import({TestcontainersConfiguration.class, TestOidcConfiguration.class})
@SpringBootTest
class DatabaseRolesTests {
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void givenApplicationDatasource_whenQueryingCurrentUser_thenIsApplicationRole() {
        // act
        String role = jdbc.queryForObject("SELECT current_user", String.class);

        // assert
        assertThat(role).isEqualTo("fennixs_app");
    }

    @Test
    void givenApplicationRole_whenReadingSuperuserFlag_thenIsFalse() {
        // act
        Boolean superuser =
                jdbc.queryForObject("SELECT rolsuper FROM pg_roles WHERE rolname = current_user", Boolean.class);

        // assert
        assertThat(superuser).isFalse();
    }

    @Test
    void givenApplicationRole_whenReadingBypassRlsFlag_thenIsFalse() {
        // act
        Boolean bypass =
                jdbc.queryForObject("SELECT rolbypassrls FROM pg_roles WHERE rolname = current_user", Boolean.class);

        // assert
        assertThat(bypass).isFalse();
    }

    @Test
    void givenApplicationRole_whenReadingMigrationHistory_thenPermissionDenied() {
        // act
        Throwable thrown =
                catchThrowable(() -> jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history", Long.class));

        // assert
        assertThat(thrown).rootCause().hasMessageContaining("permission denied for table flyway_schema_history");
    }

    @Test
    void givenApplicationRole_whenCountingOwnedObjects_thenOwnsNone() {
        // arrange
        String ownedObjects = "SELECT count(*) FROM pg_class WHERE relowner = "
                + "(SELECT oid FROM pg_roles WHERE rolname = current_user)";

        // act
        Long owned = jdbc.queryForObject(ownedObjects, Long.class);

        // assert
        assertThat(owned).isZero();
    }
}
