package com.mingzhe.resumetailor.prompttemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingzhe.resumetailor.exceptions.BadRequestException;
import com.mingzhe.resumetailor.redis.RedisCacheService;
import com.mingzhe.resumetailor.redis.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class PromptTemplateService {

    private static final Duration EFFECTIVE_PROMPT_CACHE_TTL = Duration.ofMinutes(10);

    private final PromptTemplateMapper promptTemplateMapper;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;

    public PromptTemplateService(
            PromptTemplateMapper promptTemplateMapper,
            RedisCacheService redisCacheService,
            ObjectMapper objectMapper
    ) {
        this.promptTemplateMapper = promptTemplateMapper;
        this.redisCacheService = redisCacheService;
        this.objectMapper = objectMapper;
    }

    public String findActiveTemplate(PromptTemplateType type) {
        PromptTemplate promptTemplate = promptTemplateMapper.findActiveByType(type.name());
        if (promptTemplate == null || promptTemplate.getContent() == null || promptTemplate.getContent().isBlank()) {
            throw new IllegalStateException("No active default prompt template found for type: " + type);
        }
        return promptTemplate.getContent();
    }

    public PromptTemplate getEffectivePrompt(Long userId, PromptTemplateType type) {
        validateUserId(userId);
        validateType(type);

        String cacheKey = RedisKeyConstants.effectivePromptKey(userId, type.name());
        PromptTemplate cachedPrompt = readEffectivePromptFromCache(cacheKey);
        if (cachedPrompt != null) {
            log.info("Effective prompt cache hit for key={}", cacheKey);
            return cachedPrompt;
        }

        log.info("Effective prompt cache miss for key={}", cacheKey);
        PromptTemplate promptTemplate = promptTemplateMapper.findEffectivePromptByType(userId, type.name());
        if (promptTemplate == null) {
            throw new IllegalStateException("No prompt template found for type: " + type);
        }

        writeEffectivePromptToCache(cacheKey, promptTemplate);
        return promptTemplate;
    }

    public String getEffectivePromptContent(Long userId, PromptTemplateType type) {
        PromptTemplate promptTemplate = getEffectivePrompt(userId, type);
        if (promptTemplate.getContent() == null || promptTemplate.getContent().isBlank()) {
            throw new IllegalStateException("Prompt template content is blank for type: " + type);
        }
        return promptTemplate.getContent();
    }

    public PromptTemplate saveUserPrompt(Long userId, PromptTemplateType type, String content) {
        validateUserId(userId);
        validateType(type);
        validateContent(type, content);

        PromptTemplate promptTemplate = new PromptTemplate();
        promptTemplate.setUserId(userId);
        promptTemplate.setType(type.name());
        promptTemplate.setName(type.name() + " Resume Prompt");
        promptTemplate.setVersion(1);
        promptTemplate.setContent(content);
        promptTemplate.setActive(true);

        int updatedRows = promptTemplateMapper.updateUserPrompt(promptTemplate);
        if (updatedRows == 0) {
            promptTemplateMapper.insertUserPrompt(promptTemplate);
        }

        evictEffectivePromptCache(userId, type);
        return promptTemplateMapper.findUserPromptByType(userId, type.name());
    }

    public void resetUserPrompt(Long userId, PromptTemplateType type) {
        validateUserId(userId);
        validateType(type);

        promptTemplateMapper.deleteUserPrompt(userId, type.name());
        evictEffectivePromptCache(userId, type);
    }

    private PromptTemplate readEffectivePromptFromCache(String cacheKey) {
        String cachedJson;
        try {
            cachedJson = redisCacheService.get(cacheKey);
        } catch (Exception e) {
            log.warn("Failed to read effective prompt cache for key={}. Falling back to DB.", cacheKey, e);
            return null;
        }

        if (cachedJson == null || cachedJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(cachedJson, PromptTemplate.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize effective prompt cache for key={}. Deleting bad cache entry.", cacheKey, e);
            try {
                redisCacheService.delete(cacheKey);
            } catch (Exception deleteException) {
                log.warn("Failed to delete bad effective prompt cache for key={}.", cacheKey, deleteException);
            }
            return null;
        }
    }

    private void writeEffectivePromptToCache(String cacheKey, PromptTemplate promptTemplate) {
        try {
            String json = objectMapper.writeValueAsString(promptTemplate);
            redisCacheService.set(cacheKey, json, EFFECTIVE_PROMPT_CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize effective prompt for key={}. Returning DB result without cache.", cacheKey, e);
        } catch (Exception e) {
            log.warn("Failed to write effective prompt cache for key={}. Returning DB result.", cacheKey, e);
        }
    }

    private void evictEffectivePromptCache(Long userId, PromptTemplateType type) {
        String cacheKey = RedisKeyConstants.effectivePromptKey(userId, type.name());
        try {
            redisCacheService.delete(cacheKey);
        } catch (Exception e) {
            log.warn("Failed to evict effective prompt cache for key={}.", cacheKey, e);
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User id is required.");
        }
    }

    private void validateType(PromptTemplateType type) {
        if (type == null) {
            throw new BadRequestException("Prompt template type is required.");
        }
    }

    private void validateContent(PromptTemplateType type, String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Prompt content is required.");
        }

        if (type == PromptTemplateType.NORMAL) {
            requirePlaceholder(content, "{{roleFocus}}");
            requirePlaceholder(content, "{{targetJob}}");
            requirePlaceholder(content, "{{candidateProfile}}");
            requirePlaceholder(content, "{{experiences}}");
            requirePlaceholder(content, "{{educations}}");
            requirePlaceholder(content, "{{projects}}");
            requirePlaceholder(content, "{{skills}}");
        }

        if (type == PromptTemplateType.RAG) {
            requirePlaceholder(content, "{{roleFocus}}");
            requirePlaceholder(content, "{{targetJob}}");
            requirePlaceholder(content, "{{resumeContext}}");
        }
    }

    private void requirePlaceholder(String content, String placeholder) {
        if (!content.contains(placeholder)) {
            throw new BadRequestException("Prompt content must include " + placeholder + ".");
        }
    }

}
