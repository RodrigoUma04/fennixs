package com.fennixs.auth.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fennixs.auth.auth.dto.RegisterRequestDto;
import com.fennixs.auth.auth.util.RegisterRequestDtoObjectMother;
import com.fennixs.auth.common.exception.AuthException;
import com.fennixs.auth.common.exception.BusinessException;
import com.fennixs.auth.user.entity.User;
import com.fennixs.auth.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {
    private static final String TOKEN = RegisterRequestDtoObjectMother.DEFAULT_SETUP_TOKEN;
    private static final String EMAIL = RegisterRequestDtoObjectMother.DEFAULT_EMAIL;
    private static final String PASSWORD = RegisterRequestDtoObjectMother.DEFAULT_PASSWORD;
    private static final String HASH = "hashed-password";

    @Mock
    private SetupTokenService setupTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // region register()
    @Test
    void givenValidRequest_whenRegister_thenUserIsSavedWithEncodedPassword() {
        // Arrange
        when(setupTokenService.tryConsume(TOKEN)).thenReturn(true);
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
        verify(setupTokenService, never()).restore(anyString());
    }

    @Test
    void givenInvalidSetupToken_whenRegister_thenThrowForbiddenAndNothingPersisted() {
        // Arrange
        when(setupTokenService.tryConsume(TOKEN)).thenReturn(false);
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
        when(setupTokenService.tryConsume(TOKEN)).thenReturn(true);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);
        RegisterRequestDto request = RegisterRequestDtoObjectMother.valid();

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("An account with this email already exists")
                .satisfies(
                        ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(userRepository, never()).saveAndFlush(any());
        verify(setupTokenService).restore(TOKEN);
    }

    @Test
    void givenSaveFails_whenRegister_thenPropagateExceptionAndTokenIsRestored() {
        // Arrange
        when(setupTokenService.tryConsume(TOKEN)).thenReturn(true);
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
}
