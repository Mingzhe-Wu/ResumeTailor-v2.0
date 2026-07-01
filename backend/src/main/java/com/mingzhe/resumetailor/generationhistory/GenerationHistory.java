package com.mingzhe.resumetailor.generationhistory;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GenerationHistory {
    private Long id;
    private Long userId;
    private Long jobId;
    private Long resumeVersionId;
    private GenerationMethod generationMethod;
    private Long promptTemplateId;
    private String modelName;
    private GenerationStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
