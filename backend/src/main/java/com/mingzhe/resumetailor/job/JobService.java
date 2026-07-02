package com.mingzhe.resumetailor.job;

import com.mingzhe.resumetailor.exceptions.BadRequestException;
import com.mingzhe.resumetailor.exceptions.ResourceNotFoundException;
import com.mingzhe.resumetailor.resume.ResumeMapper;
import com.mingzhe.resumetailor.user.User;
import com.mingzhe.resumetailor.user.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages independent job records. Updating a job invalidates only that job's
 * generated resumes because the job description is job-local input.
 */
@Service
public class JobService {

    private static final int JOB_TITLE_MAX_LENGTH = 100;
    private static final int JOB_COMPANY_MAX_LENGTH = 100;
    private static final int JOB_SOURCE_URL_MAX_LENGTH = 500;
    private static final Pattern LINKEDIN_CURRENT_JOB_URL_PATTERN = Pattern.compile(
            "^https://www\\.linkedin\\.com/jobs/search-results/\\?currentJobId=(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    private final JobMapper jobMapper;
    private final UserMapper userMapper;
    private final ResumeMapper resumeMapper;

    public JobService(JobMapper jobMapper, UserMapper userMapper, ResumeMapper resumeMapper) {
        this.jobMapper = jobMapper;
        this.userMapper = userMapper;
        this.resumeMapper = resumeMapper;
    }

    public Job createJob(CreateJobDTO request) {
        Integer status = request.getStatus() == null ? 1 : request.getStatus();
        validateStatus(status);

        Integer priority = request.getPriority() == null ? 0 : request.getPriority();
        validatePriority(priority);

        User user = userMapper.findById(request.getUserId());
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        Job job = new Job();
        job.setUserId(request.getUserId());
        job.setTitle(request.getTitle());
        job.setCompany(request.getCompany());
        job.setLocation(request.getLocation());
        job.setSalary(request.getSalary());
        job.setJobDescription(request.getJobDescription());
        job.setSourceUrl(request.getSourceUrl());
        job.setStatus(status);
        job.setInterviewTime(request.getInterviewTime());
        job.setPriority(priority);
        job.setNotes(request.getNotes());

        jobMapper.insert(job);
        return job;
    }

    public Job importJobForAuthenticatedUser(ImportJobDTO request, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new BadRequestException("Authenticated user is required.");
        }

        User user = userMapper.findByEmail(userEmail);
        if (user == null) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }

        CreateJobDTO createJobRequest = new CreateJobDTO();
        createJobRequest.setUserId(user.getId());
        createJobRequest.setTitle(limitLength(request.getTitle(), JOB_TITLE_MAX_LENGTH));
        createJobRequest.setCompany(resolveImportedCompany(request.getCompany()));
        createJobRequest.setSourceUrl(limitLength(normalizeSourceUrl(request.getSourceUrl()), JOB_SOURCE_URL_MAX_LENGTH));
        createJobRequest.setJobDescription(request.getDescription());

        return createJob(createJobRequest);
    }

    public List<Job> fetchJobsByUserId(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        return jobMapper.findByUserId(userId);
    }

    public List<Job> searchJobs(SearchJobDTO request) {
        User user = userMapper.findById(request.getUserId());
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        if (request.getStatus() != null) {
            validateStatus(request.getStatus());
        }

        return jobMapper.searchByUserIdAndKeyword(
                request.getUserId(),
                request.getKeyword(),
                request.getStatus()
        );
    }

    public Job updateJob(Long id, UpdateJobDTO request) {
        Job existingJob = jobMapper.findById(id);
        if (existingJob == null) {
            throw new ResourceNotFoundException("Job not found");
        }

        if (request.getStatus() != null) {
            validateStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            validatePriority(request.getPriority());
        }

        Job update = new Job();
        update.setId(id);
        update.setTitle(request.getTitle());
        update.setCompany(request.getCompany());
        update.setLocation(request.getLocation());
        update.setSalary(request.getSalary());
        update.setJobDescription(request.getJobDescription());
        update.setSourceUrl(request.getSourceUrl());
        update.setStatus(request.getStatus());
        update.setInterviewTime(request.getInterviewTime());
        update.setPriority(request.getPriority());
        update.setNotes(request.getNotes());

        jobMapper.updateById(update);
        // Job edits change only the selected job's tailoring target, so other
        // jobs and their resume versions stay untouched.
        resumeMapper.markResumeDirtyByJobId(id);
        return jobMapper.findById(id);
    }

    public void deleteJob(Long id) {
        Job existingJob = jobMapper.findById(id);
        if (existingJob == null) {
            throw new ResourceNotFoundException("Job not found");
        }

        jobMapper.deleteById(id);
    }

    private void validateStatus(Integer status) {
        try {
            JobStatus.fromCode(status);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private void validatePriority(Integer priority) {
        if (priority < 0) {
            throw new BadRequestException("priority must be greater than or equal to 0");
        }
    }

    private String limitLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String normalizeSourceUrl(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        Matcher matcher = LINKEDIN_CURRENT_JOB_URL_PATTERN.matcher(trimmed);
        return matcher.find() ? "https://www.linkedin.com/jobs/view/" + matcher.group(1) : trimmed;
    }

    private String resolveImportedCompany(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }

        return limitLength(value, JOB_COMPANY_MAX_LENGTH);
    }

}
