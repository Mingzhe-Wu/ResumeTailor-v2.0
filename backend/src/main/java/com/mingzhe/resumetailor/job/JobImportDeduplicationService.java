package com.mingzhe.resumetailor.job;

import com.mingzhe.resumetailor.redis.RedisCacheService;
import com.mingzhe.resumetailor.redis.RedisKeyConstants;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class JobImportDeduplicationService {

    static final Duration DEDUPLICATION_WINDOW = Duration.ofMinutes(1);

    private final RedisCacheService redisCacheService;

    public JobImportDeduplicationService(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    public boolean tryAcquire(Long userId, String company, String title) {
        String key = buildKey(userId, company, title);
        return Boolean.TRUE.equals(
                redisCacheService.setIfAbsent(key, "1", DEDUPLICATION_WINDOW)
        );
    }

    public void release(Long userId, String company, String title) {
        redisCacheService.delete(buildKey(userId, company, title));
    }

    private String buildKey(Long userId, String company, String title) {
        String identity = normalize(company) + "\n" + normalize(title);
        String fingerprint = sha256(identity);
        return RedisKeyConstants.jobImportDeduplicationKey(userId, fingerprint);
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(
                value == null ? "" : value,
                Normalizer.Form.NFKC
        );
        return normalized.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
