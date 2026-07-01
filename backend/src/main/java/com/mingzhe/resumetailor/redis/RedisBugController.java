package com.mingzhe.resumetailor.redis;

import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/redis/debug")
// Temporary Redis debug/test controller. Do not use these endpoints as formal application APIs.
public class RedisBugController {

    private final RedisCacheService redisCacheService;
    private final AiQuotaService aiQuotaService;
    private final RateLimitService rateLimitService;

    public RedisBugController(RedisCacheService redisCacheService, AiQuotaService aiQuotaService, RateLimitService rateLimitService) {
        this.redisCacheService = redisCacheService;
        this.aiQuotaService = aiQuotaService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/quota")
    public String getQuota(@RequestParam Long userId) {
        long used = aiQuotaService.getDailyUsage(userId);
        long limit = aiQuotaService.getDailyLimit();
        long remaining = aiQuotaService.getDailyRemaining(userId);

        return used + " / " + limit + ", remaining: " + remaining;
    }

    @PostMapping("/rate-limit/check")
    public String checkRateLimit(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "resume-generate") String action,
            @RequestParam(defaultValue = "3") long limit,
            @RequestParam(defaultValue = "60") long windowSeconds
    ) {
        rateLimitService.checkAndIncrease(
                userId,
                action,
                limit,
                Duration.ofSeconds(windowSeconds)
        );

        long count = rateLimitService.getCurrentCount(userId, action);

        return "allowed, count: " + count + " / " + limit;
    }

    @PostMapping("/set")
    public String set(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(defaultValue = "300") long ttlSeconds
    ) {
        redisCacheService.set(key, value, Duration.ofSeconds(ttlSeconds));
        return "OK";
    }

    @GetMapping("/get")
    public String get(@RequestParam String key) {
        String value = redisCacheService.get(key);
        return value == null ? "(nil)" : value;
    }

    @PostMapping("/increment")
    public Long increment(
            @RequestParam String key,
            @RequestParam(defaultValue = "300") long ttlSeconds
    ) {
        return redisCacheService.increment(key, Duration.ofSeconds(ttlSeconds));
    }

    @GetMapping("/ttl")
    public Long ttl(@RequestParam String key) {
        return redisCacheService.getExpire(key);
    }

    @DeleteMapping("/delete")
    public Boolean delete(@RequestParam String key) {
        return redisCacheService.delete(key);
    }


}
