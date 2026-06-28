package com.mingzhe.resumetailor.resume;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body used when updating Resume records.
 */
@Data
public class UpdateResumeDTO {

    private Integer matchScore;

    @Pattern(regexp = ".*\\S.*", message = "generatedContent must not be blank")
    private String generatedContent;
}
