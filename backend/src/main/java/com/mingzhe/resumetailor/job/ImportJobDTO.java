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

    private String location;

    private String salary;

    private String sourceUrl;

    private Integer status;

    @NotBlank(message = "description is required")
    private String description;

}
