package com.mingzhe.resumetailor.redis;

import com.mingzhe.resumetailor.exceptions.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private RateLimitService rateLimitService;

    @Test
    void allowsRequestsThroughLimitAndRejectsNextRequest() {
        when(redisCacheService.increment(anyString(), any(Duration.class)))
                .thenReturn(1L, 2L, 3L);

        Duration window = Duration.ofMinutes(1);
        assertDoesNotThrow(() -> rateLimitService.checkAndIncrease(9L, "resume-generate", 2, window));
        assertDoesNotThrow(() -> rateLimitService.checkAndIncrease(9L, "resume-generate", 2, window));
        assertThrows(
                TooManyRequestsException.class,
                () -> rateLimitService.checkAndIncrease(9L, "resume-generate", 2, window)
        );
    }
}
