package com.fennixs.auth.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fennixs.auth.config.AppProperties;
import com.fennixs.auth.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SetupTokenUnitTest {
    @Mock
    private UserRepository userRepository;

    private SetupTokenService service;

    @BeforeEach
    void setup() {
        service = serviceWith(false);
    }

    // region init()
    @Test
    void givenNoUsers_whenInit_thenSetupTokenIsGenerated() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);

        // Act
        ReflectionTestUtils.invokeMethod(service, "init");

        // Assert
        assertThat(tokenValue(service)).isNotBlank();
    }

    @Test
    void givenExistingUsers_whenInit_thenTokenIsNotGenerated() {
        // Arrange
        when(userRepository.count()).thenReturn(1L);

        // Act
        ReflectionTestUtils.invokeMethod(service, "init");

        // Assert
        assertThat(tokenValue(service)).isNull();
    }
    // endregion

    // region tryConsume()
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"any-token", "12345678", "11.23,5423/7*348"})
    void givenRegistrationAllowed_whenTryConsume_thenReturnTrueRegardlessOfToken(String token) {
        // Arrange
        service = serviceWith(true);

        // Assert
        assertThat(service.tryConsume(token)).isTrue();
    }

    @Test
    void givenGeneratedToken_whenTryConsumeWithSameToken_thenReturnTrueAndTokenIsClaimed() {
        // Arrange
        String token = initWithGeneratedToken();

        // Act
        boolean result = service.tryConsume(token);

        // Assert
        assertThat(result).isTrue();
        assertThat(tokenValue(service)).isNull();
    }

    @Test
    void givenGeneratedToken_whenTryConsumeTwice_thenSecondAttemptReturnsFalse() {
        // Arrange
        String token = initWithGeneratedToken();

        // Act
        boolean first = service.tryConsume(token);
        boolean second = service.tryConsume(token);

        // Assert
        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void givenGeneratedToken_whenTryConsumeWithDifferentToken_thenReturnFalseAndTokenKept() {
        // Arrange
        String token = initWithGeneratedToken();

        // Act
        boolean result = service.tryConsume("not-the-generated-token");

        // Assert
        assertThat(result).isFalse();
        assertThat(tokenValue(service)).isEqualTo(token);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void givenRegistrationNotAllowedAndBlankToken_whenTryConsume_thenReturnFalse(String token) {
        // Arrange
        initWithGeneratedToken();

        // Assert
        assertThat(service.tryConsume(token)).isFalse();
    }

    @Test
    void givenUsersAlreadyExist_whenTryConsume_thenReturnFalse() {
        // Arrange
        when(userRepository.count()).thenReturn(5L);
        ReflectionTestUtils.invokeMethod(service, "init");

        // Assert
        assertThat(service.tryConsume("any-token")).isFalse();
    }

    @Test
    void givenGeneratedToken_whenManyThreadsRaceToConsume_thenExactlyOneSucceeds() throws InterruptedException {
        // Arrange
        String token = initWithGeneratedToken();
        int contenders = 64;
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(contenders);
        AtomicInteger successes = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(contenders);

        // Act: release all threads at once so they genuinely contend on the same token
        for (int i = 0; i < contenders; i++) {
            pool.execute(() -> {
                try {
                    ready.await();
                    if (service.tryConsume(token)) {
                        successes.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });
        }
        ready.countDown();
        boolean completed = finished.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        // Assert
        assertThat(completed).isTrue();
        assertThat(successes.get()).isEqualTo(1);
        assertThat(tokenValue(service)).isNull();
    }
    // endregion

    // region restore()
    @Test
    void givenClaimedToken_whenRestore_thenTokenIsConsumableAgain() {
        // Arrange
        String token = initWithGeneratedToken();
        service.tryConsume(token);

        // Act
        service.restore(token);

        // Assert
        assertThat(tokenValue(service)).isEqualTo(token);
        assertThat(service.tryConsume(token)).isTrue();
    }

    @Test
    void givenRegistrationAllowed_whenRestore_thenNoTokenIsSet() {
        // Arrange
        service = serviceWith(true);

        // Act
        service.restore("bogus-token");

        // Assert
        assertThat(tokenValue(service)).isNull();
    }
    // endregion

    private String initWithGeneratedToken() {
        when(userRepository.count()).thenReturn(0L);
        ReflectionTestUtils.invokeMethod(service, "init");
        return tokenValue(service);
    }

    private static String tokenValue(SetupTokenService svc) {
        return (String)
                ((AtomicReference<?>) Objects.requireNonNull(ReflectionTestUtils.getField(svc, "setupToken"))).get();
    }

    private SetupTokenService serviceWith(boolean allow) {
        var properties = new AppProperties(new AppProperties.RegistrationProperties(allow), null);
        return new SetupTokenService(userRepository, properties);
    }
}
