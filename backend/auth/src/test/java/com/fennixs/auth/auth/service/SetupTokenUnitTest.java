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

import com.fennixs.auth.auth.service.SetupTokenService.Grant;
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

    // region consume()
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"any-token", "12345678", "11.23,5423/7*348"})
    void givenRegistrationAllowed_whenConsume_thenReturnOpenRegistrationRegardlessOfToken(String token) {
        // Arrange
        service = serviceWith(true);

        // Assert
        assertThat(service.consume(token)).isEqualTo(Grant.OPEN_REGISTRATION);
    }

    @Test
    void givenGeneratedToken_whenConsumeWithSameToken_thenReturnSetupTokenAndTokenIsClaimed() {
        // Arrange
        String token = initWithGeneratedToken();

        // Act
        Grant result = service.consume(token);

        // Assert
        assertThat(result).isEqualTo(Grant.SETUP_TOKEN);
        assertThat(tokenValue(service)).isNull();
    }

    @Test
    void givenGeneratedToken_whenConsumeTwice_thenSecondAttemptIsDenied() {
        // Arrange
        String token = initWithGeneratedToken();

        // Act
        Grant first = service.consume(token);
        Grant second = service.consume(token);

        // Assert
        assertThat(first).isEqualTo(Grant.SETUP_TOKEN);
        assertThat(second).isEqualTo(Grant.DENIED);
    }

    @Test
    void givenGeneratedToken_whenConsumeWithDifferentToken_thenReturnDeniedAndTokenKept() {
        // Arrange
        String token = initWithGeneratedToken();

        // Act
        Grant result = service.consume("not-the-generated-token");

        // Assert
        assertThat(result).isEqualTo(Grant.DENIED);
        assertThat(tokenValue(service)).isEqualTo(token);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void givenRegistrationNotAllowedAndBlankToken_whenConsume_thenReturnDenied(String token) {
        // Arrange
        initWithGeneratedToken();

        // Assert
        assertThat(service.consume(token)).isEqualTo(Grant.DENIED);
    }

    @Test
    void givenUsersAlreadyExist_whenConsume_thenReturnDenied() {
        // Arrange
        when(userRepository.count()).thenReturn(5L);
        ReflectionTestUtils.invokeMethod(service, "init");

        // Assert
        assertThat(service.consume("any-token")).isEqualTo(Grant.DENIED);
    }

    @Test
    void givenGeneratedToken_whenManyThreadsRaceToConsume_thenExactlyOneWins() throws InterruptedException {
        // Arrange
        String token = initWithGeneratedToken();
        int contenders = 64;
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(contenders);
        AtomicInteger grantedSetupToken = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(contenders);

        // Act: release all threads at once so they genuinely contend on the same token
        for (int i = 0; i < contenders; i++) {
            pool.execute(() -> {
                try {
                    ready.await();
                    if (service.consume(token) == Grant.SETUP_TOKEN) {
                        grantedSetupToken.incrementAndGet();
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
        assertThat(grantedSetupToken.get()).isEqualTo(1);
        assertThat(tokenValue(service)).isNull();
    }
    // endregion

    // region restore()
    @Test
    void givenClaimedToken_whenRestore_thenTokenIsConsumableAgain() {
        // Arrange
        String token = initWithGeneratedToken();
        service.consume(token);

        // Act
        service.restore(token);

        // Assert
        assertThat(tokenValue(service)).isEqualTo(token);
        assertThat(service.consume(token)).isEqualTo(Grant.SETUP_TOKEN);
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
