package com.mingzhe.resumetailor.job;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request body used when updating Job records.
 */
@Data
public class UpdateJobDTO {

    @Pattern(regexp = ".*\\S.*", message = "title must not be blank")
    private String title;

    @Pattern(regexp = ".*\\S.*", message = "company must not be blank")
    private String company;

    private String location;

    private String salary;

    private String jobDescription;

    private String sourceUrl;

    private Integer status;

    private LocalDateTime interviewTime;

    private Integer priority;

    private String notes;

}
