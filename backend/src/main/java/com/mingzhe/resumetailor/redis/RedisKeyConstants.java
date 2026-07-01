package com.mingzhe.resumetailor.redis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    private static final DateTimeFormatter DAILY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    public static String dailyAiQuotaKey(Long userId, LocalDate date) {
        return "ai:quota:user:" + userId + ":daily:" + DAILY_FORMATTER.format(date);
    }

    public static String dailyAiTokenKey(Long userId, LocalDate date) {
        return "ai:quota:user:" + userId + ":tokens:" + DAILY_FORMATTER.format(date);
    }

    public static String activePromptKey(String promptType) {
        return "prompt:active:" + promptType;
    }

    public static String enabledSkillKeywordsKey() {
        return "skill-keywords:enabled";
    }

    public static String generationStatusKey(Long jobId, String method) {
        return "generation:job:" + jobId + ":method:" + method + ":status";
    }

    public static String generationErrorKey(Long jobId, String method) {
        return "generation:job:" + jobId + ":method:" + method + ":error";
    }

    public static String generationLockKey(Long userId, Long jobId, String method) {
        return "lock:resume-generate:user:" + userId + ":job:" + jobId + ":method:" + method;
    }

    private static final DateTimeFormatter MINUTE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    public static String rateLimitKey(Long userId, String action, LocalDateTime time) {
        return "rate:user:" + userId + ":" + action + ":" + MINUTE_FORMATTER.format(time);
    }
}
