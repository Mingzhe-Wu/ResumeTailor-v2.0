package com.mingzhe.resumetailor.redis;

import com.mingzhe.resumetailor.exceptions.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AiQuotaService {

    private static final long DAILY_AI_CALL_LIMIT = 100;

    private final RedisCacheService redisCacheService;

    public AiQuotaService(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    public void checkAndIncreaseDailyUsage(Long userId) {
        long count = increaseDailyUsage(userId);

        if (count > DAILY_AI_CALL_LIMIT) {
            throw new TooManyRequestsException("Daily AI usage limit reached. Please try again tomorrow.");
        }
    }

    public long getDailyUsage(Long userId) {
        String key = RedisKeyConstants.dailyAiQuotaKey(userId, LocalDate.now());
        String value = redisCacheService.get(key);

        if (value == null) {
            return 0L;
        }

        return Long.parseLong(value);
    }

    public long getDailyRemaining(Long userId) {
        String key = RedisKeyConstants.dailyAiQuotaKey(userId, LocalDate.now());
        String value = redisCacheService.get(key);
        if (value == null) {
            return DAILY_AI_CALL_LIMIT;
        }

        return Math.max(0, DAILY_AI_CALL_LIMIT - Long.parseLong(value));
    }

    public long increaseDailyUsage(Long userId) {
        String key = RedisKeyConstants.dailyAiQuotaKey(userId, LocalDate.now());
        Duration ttl = durationUntilEndOfDay();

        Long count = redisCacheService.increment(key, ttl);

        return count == null ? 0L : count;
    }

    private Duration durationUntilEndOfDay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDay = now.toLocalDate().plusDays(1).atStartOfDay();

        return Duration.between(now, endOfDay);
    }

    public long getDailyLimit() {
        return DAILY_AI_CALL_LIMIT;
    }
}
