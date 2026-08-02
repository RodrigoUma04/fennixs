package com.fennixs.auth.auth.service;

import static com.fennixs.auth.common.crypto.HashUtils.sha256;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fennixs.auth.auth.dto.AuthTokens;
import com.fennixs.auth.auth.dto.LoginRequestDto;
import com.fennixs.auth.auth.dto.RegisterRequestDto;
import com.fennixs.auth.auth.entity.RefreshToken;
import com.fennixs.auth.auth.repository.RefreshTokenRepository;
import com.fennixs.auth.auth.service.SetupTokenService.Grant;
import com.fennixs.auth.auth.util.LoginRequestDtoObjectMother;
import com.fennixs.auth.auth.util.RegisterRequestDtoObjectMother;
import com.fennixs.auth.auth.util.TestCredentials;
import com.fennixs.auth.common.exception.AuthException;
import com.fennixs.auth.common.exception.BusinessException;
import com.fennixs.auth.config.AppProperties;
import com.fennixs.auth.config.AuthPrincipal;
import com.fennixs.auth.user.entity.Role;
import com.fennixs.auth.user.entity.User;
import com.fennixs.auth.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {
    private static final String TOKEN = RegisterRequestDtoObjectMother.DEFAULT_SETUP_TOKEN;
    private static final String EMAIL = TestCredentials.DEFAULT_EMAIL;
    private static final String PASSWORD = TestCredentials.DEFAULT_PASSWORD;
    private static final String HASH = "hashed-password";
    private static final String ACCESS_TOKEN = "signed-jwt";

    @Mock
    private SetupTokenService setupTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final AppProperties appProperties = new AppProperties(
            null,
            new AppProperties.SecurityProperties(
                    new AppProperties.SecurityProperties.JwtProperties("unit-test-secret", 900_000L, 2_592_000_000L),
                    false,
                    List.of()));

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                setupTokenService, userRepository, passwordEncoder, jwtService, appProperties, refreshTokenRepository);
    }

    // region register()
    @Test
    void givenSetupTokenGrant_whenRegister_thenUserIsSavedWithEncodedPasswordAndOwnerRole() {
        // Arrange
        when(setupTokenService.consume(TOKEN)).thenReturn(Grant.SETUP_TOKEN);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        RegisterRequestDto request = RegisterRequestDtoObjectMother.valid();

        // Act
        authService.register(request);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved).extracting("passwordHash").isEqualTo(HASH);
        assertThat(saved.getRole()).isEqualTo(Role.OWNER);
        verify(setupTokenService, never()).restore(anyString());
    }

    @Test
    void givenOpenRegistrationGrant_whenRegister_thenUserGetsUserRole() {
        // Arrange
        when(setupTokenService.consume(TOKEN)).thenReturn(Grant.OPEN_REGISTRATION);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        RegisterRequestDto request = RegisterRequestDtoObjectMother.valid();

        // Act
        authService.register(request);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void givenEmailWithMixedCaseAndWhitespace_whenRegister_thenEmailIsNormalizedForCheckAndSave() {
        // Arrange
        when(setupTokenService.consume(TOKEN)).thenReturn(Grant.SETUP_TOKEN);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        RegisterRequestDto request =
                RegisterRequestDtoObjectMother.createRegisterRequestDto(TOKEN, "  User@Fennixs.COM  ", PASSWORD);

        // Act
        authService.register(request);

        // Assert
        verify(userRepository).existsByEmail(EMAIL);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void givenDeniedGrant_whenRegister_thenThrowForbiddenAndNothingPersisted() {
        // Arrange
        when(setupTokenService.consume(TOKEN)).thenReturn(Grant.DENIED);
        RegisterRequestDto request = RegisterRequestDtoObjectMother.valid();

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid or missing setup token")
                .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).saveAndFlush(any());
        verify(setupTokenService, never()).restore(anyString());
    }

    @Test
    void givenDuplicateEmail_whenRegister_thenThrowConflictAndTokenIsRestored() {
        // Arrange
        when(setupTokenService.consume(TOKEN)).thenReturn(Grant.SETUP_TOKEN);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);
        RegisterRequestDto request = RegisterRequestDtoObjectMother.valid();

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A resource with the provided data already exists")
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(userRepository, never()).saveAndFlush(any());
        verify(setupTokenService).restore(TOKEN);
    }

    @Test
    void givenSaveFails_whenRegister_thenPropagateExceptionAndTokenIsRestored() {
        // Arrange
        when(setupTokenService.consume(TOKEN)).thenReturn(Grant.SETUP_TOKEN);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        RegisterRequestDto request = RegisterRequestDtoObjectMother.valid();

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(DataIntegrityViolationException.class);
        verify(setupTokenService).restore(TOKEN);
    }
    // endregion

    // region login()
    @Test
    void givenValidCredentials_whenLogin_thenReturnTokensAndPersistHashedRefreshToken() {
        // Arrange
        User user =
                User.builder().email(EMAIL).passwordHash(HASH).role(Role.USER).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(jwtService.generateAccessToken(any(AuthPrincipal.class))).thenReturn(ACCESS_TOKEN);
        LoginRequestDto request = LoginRequestDtoObjectMother.valid();

        // Act
        AuthTokens tokens = authService.login(request);

        // Assert
        assertThat(tokens.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(tokens.refreshToken()).isNotBlank();

        ArgumentCaptor<AuthPrincipal> principalCaptor = ArgumentCaptor.forClass(AuthPrincipal.class);
        verify(jwtService).generateAccessToken(principalCaptor.capture());
        assertThat(principalCaptor.getValue().role()).isEqualTo(Role.USER);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(sha256(tokens.refreshToken()));
    }

    @Test
    void givenEmailWithMixedCaseAndWhitespace_whenLogin_thenEmailIsNormalizedForLookup() {
        // Arrange
        User user =
                User.builder().email(EMAIL).passwordHash(HASH).role(Role.USER).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(jwtService.generateAccessToken(any(AuthPrincipal.class))).thenReturn(ACCESS_TOKEN);
        LoginRequestDto request = LoginRequestDtoObjectMother.createLoginRequestDto("  User@Fennixs.COM  ", PASSWORD);

        // Act
        authService.login(request);

        // Assert
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void givenWrongPassword_whenLogin_thenThrowUnauthorizedWithGenericMessageAndIssueNoTokens() {
        // Arrange
        User user =
                User.builder().email(EMAIL).passwordHash(HASH).role(Role.USER).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(false);
        LoginRequestDto request = LoginRequestDtoObjectMother.valid();

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid credentials")
                .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void givenUnknownEmail_whenLogin_thenThrowSameGenericUnauthorizedMessageAsWrongPassword() {
        // Arrange
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        LoginRequestDto request = LoginRequestDtoObjectMother.valid();

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid credentials")
                .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
    // endregion

    // region refresh()
    @Test
    void givenValidRefreshToken_whenRefresh_thenRotateTokenAndReturnNewTokens() {
        // Arrange
        String rawToken = "raw-refresh-token";
        User user =
                User.builder().email(EMAIL).passwordHash(HASH).role(Role.USER).build();
        RefreshToken stored = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(rawToken))
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        when(refreshTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(any(AuthPrincipal.class))).thenReturn(ACCESS_TOKEN);

        // Act
        AuthTokens tokens = authService.refresh(rawToken);

        // Assert
        assertThat(tokens.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(tokens.refreshToken()).isNotEqualTo(rawToken);
        verify(refreshTokenRepository).delete(stored);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void givenExpiredRefreshToken_whenRefresh_thenDeleteItAndThrowUnauthorizedWithoutIssuingTokens() {
        // Arrange
        String rawToken = "raw-refresh-token";
        User user =
                User.builder().email(EMAIL).passwordHash(HASH).role(Role.USER).build();
        RefreshToken expired = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(rawToken))
                .expiresAt(Instant.now().minus(1, ChronoUnit.SECONDS))
                .build();
        when(refreshTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(expired));

        // Act & Assert
        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(AuthException.class)
                .hasMessage("Refresh token expired")
                .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(refreshTokenRepository).delete(expired);
        verify(refreshTokenRepository, never()).save(any());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void givenUnknownRefreshToken_whenRefresh_thenThrowUnauthorized() {
        // Arrange
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.refresh("unknown-token"))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid refresh token")
                .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void givenNullRefreshToken_whenRefresh_thenThrowUnauthorizedWithoutTouchingRepository() {
        // Act & Assert
        assertThatThrownBy(() -> authService.refresh(null))
                .isInstanceOf(AuthException.class)
                .hasMessage("Refresh token missing")
                .satisfies(ex -> assertThat(((AuthException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
    }
    // endregion

    // region logout()
    @Test
    void givenValidRefreshToken_whenLogout_thenDeleteIt() {
        // Arrange
        String rawToken = "raw-refresh-token";
        RefreshToken stored = RefreshToken.builder()
                .tokenHash(sha256(rawToken))
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        when(refreshTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(stored));

        // Act
        authService.logout(rawToken);

        // Assert
        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    void givenUnknownRefreshToken_whenLogout_thenNoOpWithoutError() {
        // Arrange
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        authService.logout("unknown-token");
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void givenNullRefreshToken_whenLogout_thenNoOpWithoutTouchingRepository() {
        // Act & Assert
        authService.logout(null);
        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
    }
    // endregion
}
