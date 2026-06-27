package com.mingzhe.resumetailor.job;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents Job data in the application.
 */
@Data
public class Job {

    private Long id;

    private Long userId;

    private String title;

    private String company;

    private String location;

    private String salary;

    private String jobDescription;

    private String sourceUrl;

    private Integer status;

    private LocalDateTime interviewTime;

    private Integer priority;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
