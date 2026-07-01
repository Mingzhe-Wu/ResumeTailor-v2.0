package com.mingzhe.resumetailor.generationhistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GenerationHistoryService {

    private static final Logger log = LoggerFactory.getLogger(GenerationHistoryService.class);

    private final GenerationHistoryMapper generationHistoryMapper;
    private final GenerationCostService generationCostService;

    public GenerationHistoryService(
            GenerationHistoryMapper generationHistoryMapper,
            GenerationCostService generationCostService
    ) {
        this.generationHistoryMapper = generationHistoryMapper;
        this.generationCostService = generationCostService;
    }

    public void recordSuccess(
            Long userId,
            Long jobId,
            Long resumeVersionId,
            GenerationMethod generationMethod,
            Long promptTemplateId,
            String modelName,
            Integer inputTokenCount,
            Integer outputTokenCount
    ) {
        record(
                userId,
                jobId,
                resumeVersionId,
                generationMethod,
                promptTemplateId,
                modelName,
                GenerationStatus.SUCCESS,
                null,
                inputTokenCount,
                outputTokenCount
        );
    }

    public void recordFailure(
            Long userId,
            Long jobId,
            GenerationMethod generationMethod,
            Long promptTemplateId,
            String modelName,
            String errorMessage
    ) {
        record(
                userId,
                jobId,
                null,
                generationMethod,
                promptTemplateId,
                modelName,
                GenerationStatus.FAILED,
                errorMessage,
                null,
                null
        );
    }

    private void record(
            Long userId,
            Long jobId,
            Long resumeVersionId,
            GenerationMethod generationMethod,
            Long promptTemplateId,
            String modelName,
            GenerationStatus status,
            String errorMessage,
            Integer inputTokenCount,
            Integer outputTokenCount
    ) {
        if (userId == null || generationMethod == null || status == null) {
            log.warn("Skipping generation history record due to missing required fields: userId={}, method={}, status={}",
                    userId, generationMethod, status);
            return;
        }

        try {
            GenerationHistory generationHistory = new GenerationHistory();
            generationHistory.setUserId(userId);
            generationHistory.setJobId(jobId);
            generationHistory.setResumeVersionId(resumeVersionId);
            generationHistory.setGenerationMethod(generationMethod);
            generationHistory.setPromptTemplateId(promptTemplateId);
            generationHistory.setModelName(modelName);
            generationHistory.setStatus(status);
            generationHistory.setErrorMessage(errorMessage);
            generationHistory.setInputTokenCount(inputTokenCount);
            generationHistory.setOutputTokenCount(outputTokenCount);
            generationHistory.setEstimatedCostUsd(
                    generationCostService.estimateCostUsd(inputTokenCount, outputTokenCount)
            );

            generationHistoryMapper.insert(generationHistory);
        } catch (Exception ex) {
            log.warn("Failed to record generation history: {}", ex.getMessage());
        }
    }
}
