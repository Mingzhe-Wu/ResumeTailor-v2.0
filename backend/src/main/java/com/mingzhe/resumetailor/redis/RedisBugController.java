package com.mingzhe.resumetailor.redis;

import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/test/redis")
public class RedisBugController {

    private final RedisCacheService redisCacheService;

    public RedisBugController(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
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
