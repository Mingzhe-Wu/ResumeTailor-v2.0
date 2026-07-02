package com.mingzhe.resumetailor.job;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Browser-extension entry point for importing the visible active job page.
 */
@RestController
@RequestMapping("/api/jobs")
public class JobImportController {

    private final JobService jobService;

    public JobImportController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/import")
    public ResponseEntity<Job> importJob(
            @RequestBody @Valid ImportJobDTO request,
            Authentication authentication
    ) {
        String userEmail = authentication == null ? null : authentication.getName();
        Job importedJob = jobService.importJobForAuthenticatedUser(request, userEmail);
        return ResponseEntity.status(201).body(importedJob);
    }
}
