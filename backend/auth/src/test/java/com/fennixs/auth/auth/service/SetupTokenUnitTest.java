package com.fennixs.auth.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import java.util.Objects;
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

    // region isValid()
    @Test
    void givenGeneratedToken_whenIsValidWithSameToken_thenReturnTrue() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);

        // Act
        ReflectionTestUtils.invokeMethod(service, "init");

        // Assert
        assertThat(service.isValid(tokenValue(service))).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"any-token", "12345678", "11.23,5423/7*348"})
    void givenRegistrationAllowed_whenIsValid_thenReturnTrueRegardlessOfToken(String token) {
        // Arrange
        service = serviceWith(true);

        // Assert
        assertThat(service.isValid(token)).isTrue();
    }

    @Test
    void givenGeneratedToken_whenIsValidWithDifferentToken_thenReturnFalse() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);

        // Act
        ReflectionTestUtils.invokeMethod(service, "init");

        // Assert
        assertThat(service.isValid("not-the-generated-token")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void givenRegistrationNotAllowedAndBlankToken_whenIsValid_thenReturnFalse(String token) {
        // Arrange
        when(userRepository.count()).thenReturn(0L);

        // Act
        ReflectionTestUtils.invokeMethod(service, "init");

        // Assert
        assertThat(service.isValid(token)).isFalse();
    }

    @Test
    void givenUsersAlreadyExist_whenIsValid_thenReturnFalse() {
        // Arrange
        when(userRepository.count()).thenReturn(5L);

        // Act
        ReflectionTestUtils.invokeMethod(service, "init");

        // Assert
        assertThat(service.isValid("any-token")).isFalse();
    }

    @Test
    void givenTokenAlreadyConsumed_whenIsValid_thenReturnFalseEvenForPreviouslyValidToken() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);
        ReflectionTestUtils.invokeMethod(service, "init");
        String token = tokenValue(service);

        // Act
        service.consume();

        // Assert
        assertThat(tokenValue(service)).isNull();
        assertThat(service.isValid(token)).isFalse();
    }
    // endregion

    // region consume()
    @Test
    void givenGeneratedToken_whenConsume_thenTokenIsCleared() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);
        ReflectionTestUtils.invokeMethod(service, "init");

        // Act
        service.consume();

        // Assert
        assertThat(tokenValue(service)).isNull();
    }

    @Test
    void givenNoToken_whenConsume_thenRemainsNullAndDoesNotThrow() {
        // Act / Assert
        assertThatCode(() -> service.consume()).doesNotThrowAnyException();
        assertThat(tokenValue(service)).isNull();
    }

    @Test
    void givenAlreadyConsumed_whenConsumeAgain_thenRemainsNull() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);
        ReflectionTestUtils.invokeMethod(service, "init");
        service.consume();

        // Act
        service.consume();

        // Assert
        assertThat(tokenValue(service)).isNull();
    }
    // endregion

    private static String tokenValue(SetupTokenService svc) {
        return (String)
                ((AtomicReference<?>) Objects.requireNonNull(ReflectionTestUtils.getField(svc, "setupToken"))).get();
    }

    private SetupTokenService serviceWith(boolean allow) {
        var properties = new AppProperties(new AppProperties.RegistrationProperties(allow), null);
        return new SetupTokenService(userRepository, properties);
    }
}
