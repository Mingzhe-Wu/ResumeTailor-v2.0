package com.mingzhe.resumetailor.job;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for user-initiated browser extension job imports.
 */
@Data
public class ImportJobDTO {

    @NotBlank(message = "title is required")
    private String title;

    private String company;

    private String sourceUrl;

    @NotBlank(message = "description is required")
    private String description;

}
