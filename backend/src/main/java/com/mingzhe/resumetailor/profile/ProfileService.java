package com.mingzhe.resumetailor.profile;

import com.mingzhe.resumetailor.exceptions.BadRequestException;
import com.mingzhe.resumetailor.exceptions.ResourceNotFoundException;
import com.mingzhe.resumetailor.resume.ResumeMapper;
import org.springframework.stereotype.Service;

/**
 * Manages the single master profile per user. Any profile-level change invalidates
 * all generated resumes because every job-specific resume depends on it.
 */
@Service
public class ProfileService {
    private final ProfileMapper profileMapper;
    private final ResumeMapper resumeMapper;

    public ProfileService(ProfileMapper profileMapper, ResumeMapper resumeMapper) {
        this.profileMapper = profileMapper;
        this.resumeMapper = resumeMapper;
    }

    public Profile createProfile(CreateProfileDTO profile) {
        Profile existingProfile = profileMapper.findByUserId(profile.getUserId());
        if (existingProfile != null) {
            throw new BadRequestException("Profile already exists for this user");
        }

        Profile profileEntity = new Profile();
        profileEntity.setUserId(profile.getUserId());
        profileEntity.setFullName(profile.getFullName());
        profileEntity.setPhone(profile.getPhone());
        profileEntity.setContactEmail(profile.getContactEmail());
        profileEntity.setLinkedinUrl(profile.getLinkedinUrl());
        profileEntity.setGithubUrl(profile.getGithubUrl());
        profileEntity.setLocation(profile.getLocation());
        profileEntity.setSummary(profile.getSummary());
        profileMapper.insert(profileEntity);
        // A new/changed master profile makes every resume for this user stale.
        resumeMapper.markResumeDirtyByUserId(profile.getUserId());
        return profileEntity;
    }

    public Profile fetchProfile(Long userId) {
        return profileMapper.findByUserId(userId);
    }

    public Profile updateProfile(Long userId, UpdateProfileDTO request) {
        Profile existingProfile = profileMapper.findByUserId(userId);
        if (existingProfile == null) {
            throw new ResourceNotFoundException("Profile not found");
        }

        Profile update = new Profile();
        update.setUserId(userId);

        update.setFullName(request.getFullName());
        update.setPhone(request.getPhone());
        update.setContactEmail(request.getContactEmail());
        update.setLinkedinUrl(request.getLinkedinUrl());
        update.setGithubUrl(request.getGithubUrl());
        update.setLocation(request.getLocation());
        update.setSummary(request.getSummary());

        profileMapper.updateById(update);
        // Profile changes affect all jobs, not a subset, because the app has one
        // master resume/profile per user.
        resumeMapper.markResumeDirtyByUserId(userId);
        return profileMapper.findByUserId(userId);
    }

    public void deleteProfile(Long userId) {
        Profile existingProfile = profileMapper.findByUserId(userId);
        if (existingProfile == null) {
            throw new ResourceNotFoundException("Profile not found");
        }

        profileMapper.deleteById(userId);
    }

}
