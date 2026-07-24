package com.mingzhe.resumetailor.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class RedisCacheService {
    private final StringRedisTemplate stringRedisTemplate;

    public RedisCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String get(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get Redis key: {}", key, e);
            throw e;
        }
    }

    public void set(String key, String value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.error("Failed to set Redis key: {}", key, e);
            throw e;
        }
    }

    public Boolean setIfAbsent(String key, String value, Duration ttl) {
        try {
            return stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        } catch (Exception e) {
            log.error("Failed to set Redis key if absent: {}", key, e);
            throw e;
        }
    }

    public void set(String key, String value) {
        try {
            stringRedisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Failed to set Redis key without TTL: {}", key, e);
            throw e;
        }
    }

    public Boolean delete(String key) {
        try {
            return stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to delete Redis key: {}", key, e);
            throw e;
        }
    }

    public Boolean hasKey(String key) {
        try {
            return stringRedisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Failed to check Redis key: {}", key, e);
            throw e;
        }
    }

    public Long increment(String key) {
        try {
            return stringRedisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Failed to increment Redis key: {}", key, e);
            throw e;
        }
    }

    public Long increment(String key, Duration ttl) {
        try {
            Long value = stringRedisTemplate.opsForValue().increment(key);

            if (value != null && value == 1L) {
                stringRedisTemplate.expire(key, ttl);
            }

            return value;
        } catch (Exception e) {
            log.error("Failed to increment Redis key with TTL: {}", key, e);
            throw e;
        }
    }

    public Boolean expire(String key, Duration ttl) {
        try {
            return stringRedisTemplate.expire(key, ttl);
        } catch (Exception e) {
            log.error("Failed to set Redis TTL for key: {}", key, e);
            throw e;
        }
    }

    public Long getExpire(String key) {
        try {
            return stringRedisTemplate.getExpire(key);
        } catch (Exception e) {
            log.error("Failed to get Redis TTL for key: {}", key, e);
            throw e;
        }
    }
}
