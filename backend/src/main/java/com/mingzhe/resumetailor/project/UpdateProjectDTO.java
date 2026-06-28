package com.mingzhe.resumetailor.project;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request body used when updating Project records.
 */
@Data
public class UpdateProjectDTO {

    @Pattern(regexp = ".*\\S.*", message = "projectName must not be blank")
    private String projectName;

    private String techStack;

    private LocalDate startDate;

    private LocalDate endDate;

    private String description;

}
