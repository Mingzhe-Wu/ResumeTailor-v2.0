package com.mingzhe.resumetailor.redis;

import com.mingzhe.resumetailor.exceptions.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
public class RateLimitService {

    private final RedisCacheService redisCacheService;

    public RateLimitService(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    public long increaseRequestCount(Long userId, String action, Duration window) {
        String key = RedisKeyConstants.rateLimitKey(userId, action, LocalDateTime.now());
        Long count = redisCacheService.increment(key, window);

        return count == null ? 0L : count;
    }

    public void checkAndIncrease(Long userId, String action, long limit, Duration window) {
        long count = increaseRequestCount(userId, action, window);

        if (count > limit) {
            throw new TooManyRequestsException("Too many requests. Please wait a moment and try again.");
        }
        log.info("Rate limit checked for userId: {}", userId);
    }

    public long getCurrentCount(Long userId, String action) {
        String key = RedisKeyConstants.rateLimitKey(userId, action, LocalDateTime.now());
        String value = redisCacheService.get(key);

        if (value == null) {
            return 0L;
        }

        return Long.parseLong(value);
    }
}
