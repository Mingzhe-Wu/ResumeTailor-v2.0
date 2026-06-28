package com.mingzhe.resumetailor.education;

import com.mingzhe.resumetailor.exceptions.BadRequestException;
import com.mingzhe.resumetailor.exceptions.ResourceNotFoundException;
import com.mingzhe.resumetailor.profile.Profile;
import com.mingzhe.resumetailor.profile.ProfileMapper;
import com.mingzhe.resumetailor.resume.ResumeMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for validating and managing Education records.
 */
@Service
public class EducationService {

    private final EducationMapper educationMapper;
    private final ProfileMapper profileMapper;
    private final ResumeMapper resumeMapper;

    public EducationService(EducationMapper educationMapper, ProfileMapper profileMapper, ResumeMapper resumeMapper) {
        this.educationMapper = educationMapper;
        this.profileMapper = profileMapper;
        this.resumeMapper = resumeMapper;
    }

    public Education createEducation(CreateEducationDTO request) {
        Profile profile = profileMapper.findById(request.getProfileId());
        if (profile == null) {
            throw new ResourceNotFoundException("Profile not found");
        }

        if (request.getEndDate() != null && request.getStartDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("endDate cannot be before startDate");
        }

        if (request.getGpa() != null && !isValidGpa(request.getGpa())) {
            throw new BadRequestException("gpa must be between 0.00 and 4.00");
        }

        Education education = new Education();
        education.setProfileId(request.getProfileId());
        education.setSchoolName(request.getSchoolName());
        education.setDegree(request.getDegree());
        education.setMajor(request.getMajor());
        education.setStartDate(request.getStartDate());
        education.setEndDate(request.getEndDate());
        education.setGpa(request.getGpa());
        education.setRelevantCoursework(request.getRelevantCoursework());
        education.setDescription(request.getDescription());

        educationMapper.insert(education);
        resumeMapper.markResumeDirtyByUserId(profile.getUserId());
        return education;
    }

    public List<Education> fetchEducationsByProfileId(Long profileId) {
        Profile profile = profileMapper.findById(profileId);
        if (profile == null) {
            throw new ResourceNotFoundException("Profile not found");
        }

        return educationMapper.findByProfileId(profileId);
    }

    public Education updateEducation(Long id, UpdateEducationDTO request) {
        Education existingEducation = educationMapper.findById(id);
        if (existingEducation == null) {
            throw new ResourceNotFoundException("Education not found");
        }

        LocalDate startDateToCheck = request.getStartDate() != null
                ? request.getStartDate()
                : existingEducation.getStartDate();
        LocalDate endDateToCheck = request.getEndDate() != null
                ? request.getEndDate()
                : existingEducation.getEndDate();

        if (endDateToCheck != null && startDateToCheck != null && endDateToCheck.isBefore(startDateToCheck)) {
            throw new BadRequestException("endDate cannot be before startDate");
        }

        if (request.getGpa() != null && !isValidGpa(request.getGpa())) {
            throw new BadRequestException("gpa must be between 0.00 and 4.00");
        }

        Education update = new Education();
        update.setId(id);
        update.setSchoolName(request.getSchoolName());
        update.setDegree(request.getDegree());
        update.setMajor(request.getMajor());
        update.setStartDate(request.getStartDate());
        update.setEndDate(request.getEndDate());
        update.setGpa(request.getGpa());
        update.setRelevantCoursework(request.getRelevantCoursework());
        update.setDescription(request.getDescription());

        educationMapper.updateById(update);
        Profile profile = profileMapper.findById(existingEducation.getProfileId());
        if (profile != null) {
            resumeMapper.markResumeDirtyByUserId(profile.getUserId());
        }
        return educationMapper.findById(id);
    }

    public void deleteEducation(Long id) {
        Education existingEducation = educationMapper.findById(id);
        if (existingEducation == null) {
            throw new ResourceNotFoundException("Education not found");
        }

        educationMapper.deleteById(id);
        Profile profile = profileMapper.findById(existingEducation.getProfileId());
        if (profile != null) {
            resumeMapper.markResumeDirtyByUserId(profile.getUserId());
        }
    }

    private boolean isValidGpa(BigDecimal gpa) {
        return gpa.compareTo(BigDecimal.ZERO) >= 0 && gpa.compareTo(new BigDecimal("4.000")) <= 0;
    }
}
