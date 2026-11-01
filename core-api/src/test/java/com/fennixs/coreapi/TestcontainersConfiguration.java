package com.fennixs.coreapi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @SuppressWarnings({"resource", "OctalInteger"})
    @Bean
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18"))
                .withCopyFileToContainer(
                        MountableFile.forHostPath("../docker/init-db.sh", 0755),
                        "/docker-entrypoint-initdb.d/init-db.sh")
                .withEnv("KEYCLOAK_DB_NAME", "fennixs_keycloak")
                .withEnv("KEYCLOAK_DB_USER", "fennixs_keycloak")
                .withEnv("KEYCLOAK_DB_PASSWORD", "test")
                .withEnv("CORE_DB_NAME", "fennixs_core")
                .withEnv("CORE_MIGRATOR_USER", "fennixs_migrator")
                .withEnv("CORE_MIGRATOR_PASSWORD", "test")
                .withEnv("CORE_APP_USER", "fennixs_app")
                .withEnv("CORE_APP_PASSWORD", "test");
    }

    @Bean
    DynamicPropertyRegistrar coreDatabase(PostgreSQLContainer postgres) {
        return registry -> {
            String url =
                    "jdbc:postgresql://%s:%d/fennixs_core".formatted(postgres.getHost(), postgres.getMappedPort(5432));
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username", () -> "fennixs_app");
            registry.add("spring.datasource.password", () -> "test");
            registry.add("spring.flyway.user", () -> "fennixs_migrator");
            registry.add("spring.flyway.password", () -> "test");
        };
    }
}
