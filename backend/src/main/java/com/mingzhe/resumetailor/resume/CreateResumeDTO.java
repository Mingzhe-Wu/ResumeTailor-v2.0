package com.mingzhe.resumetailor.resume;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body used when creating Resume records.
 */
@Data
public class CreateResumeDTO {

    @NotNull(message = "jobId is required")
    private Long jobId;

    private Integer matchScore;

    @NotBlank(message = "generatedContent is required")
    private String generatedContent;

}
