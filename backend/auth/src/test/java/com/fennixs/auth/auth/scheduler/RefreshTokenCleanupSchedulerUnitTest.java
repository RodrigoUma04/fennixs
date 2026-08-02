package com.fennixs.auth.auth.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fennixs.auth.auth.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupSchedulerUnitTest {
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RefreshTokenCleanupScheduler(refreshTokenRepository);
    }

    @Test
    void whenDeleteExpiredTokens_thenDeleteAllTokensExpiredBeforeNow() {
        // Act
        Instant before = Instant.now();
        scheduler.deleteExpiredTokens();
        Instant after = Instant.now();

        // Assert
        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).deleteAllByExpiresAtBefore(thresholdCaptor.capture());
        assertThat(thresholdCaptor.getValue()).isBetween(before, after);
    }

    @Test
    void givenRepositoryThrows_whenDeleteExpiredTokens_thenExceptionIsSwallowed() {
        // Arrange
        doThrow(new RuntimeException("db unavailable"))
                .when(refreshTokenRepository)
                .deleteAllByExpiresAtBefore(any());

        // Act & Assert
        assertThatCode(scheduler::deleteExpiredTokens).doesNotThrowAnyException();
    }
}
