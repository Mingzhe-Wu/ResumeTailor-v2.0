package com.mingzhe.resumetailor.redis;

import com.mingzhe.resumetailor.exceptions.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiQuotaServiceTest {

    @Mock
    private RedisCacheService redisCacheService;

    private AiQuotaService aiQuotaService;

    @BeforeEach
    void setUp() {
        aiQuotaService = new AiQuotaService(redisCacheService, 2);
    }

    @Test
    void allowsCallsThroughLimitAndRejectsNextCall() {
        when(redisCacheService.increment(anyString(), any(Duration.class)))
                .thenReturn(1L, 2L, 3L);

        assertDoesNotThrow(() -> aiQuotaService.checkAndIncreaseDailyUsage(7L));
        assertDoesNotThrow(() -> aiQuotaService.checkAndIncreaseDailyUsage(7L));
        assertThrows(
                TooManyRequestsException.class,
                () -> aiQuotaService.checkAndIncreaseDailyUsage(7L)
        );
    }

    @Test
    void reportsRemainingDailyQuotaWithoutGoingNegative() {
        when(redisCacheService.get(anyString())).thenReturn("1", "4");

        assertEquals(1L, aiQuotaService.getDailyRemaining(7L));
        assertEquals(0L, aiQuotaService.getDailyRemaining(7L));
    }
}
