package com.mingzhe.resumetailor.skill;

import com.mingzhe.resumetailor.exceptions.BadRequestException;
import com.mingzhe.resumetailor.exceptions.ResourceNotFoundException;
import com.mingzhe.resumetailor.profile.Profile;
import com.mingzhe.resumetailor.profile.ProfileMapper;
import com.mingzhe.resumetailor.rag.ProfileEmbeddingChunkService;
import com.mingzhe.resumetailor.resume.ResumeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Business logic for validating and managing Skill records.
 */
@Service
public class SkillService {

    private static final Logger log = LoggerFactory.getLogger(SkillService.class);

    private final SkillMapper skillMapper;
    private final ProfileMapper profileMapper;
    private final ResumeMapper resumeMapper;

    private final ProfileEmbeddingChunkService profileEmbeddingChunkService;

    public SkillService(SkillMapper skillMapper, ProfileMapper profileMapper, ResumeMapper resumeMapper, ProfileEmbeddingChunkService profileEmbeddingChunkService) {
        this.skillMapper = skillMapper;
        this.profileMapper = profileMapper;
        this.resumeMapper = resumeMapper;
        this.profileEmbeddingChunkService = profileEmbeddingChunkService;
    }

    public Skill createSkill(CreateSkillDTO request) {
        Profile profile = profileMapper.findById(request.getProfileId());
        if (profile == null) {
            throw new ResourceNotFoundException("Profile not found");
        }

        Skill skill = new Skill();
        skill.setProfileId(request.getProfileId());
        skill.setCategory(request.getCategory());
        skill.setName(request.getName());

        if (skillMapper.insert(skill) == 0) {
            throw new BadRequestException("Skill already exists.");
        }
        resumeMapper.markResumeDirtyByUserId(profile.getUserId());

        profileEmbeddingChunkService.syncSkillChunks(profile.getUserId(), profile.getId());
        return skill;
    }

    public List<Skill> fetchSkillsByProfileId(Long profileId) {
        Profile profile = profileMapper.findById(profileId);
        if (profile == null) {
            throw new ResourceNotFoundException("Profile not found");
        }

        return skillMapper.findByProfileId(profileId);
    }

    public List<Skill> searchSkills(SearchSkillDTO request) {
        Profile profile = profileMapper.findById(request.getProfileId());
        if (profile == null) {
            throw new ResourceNotFoundException("Profile not found");
        }

        return skillMapper.searchSkills(
                request.getProfileId(),
                request.getName(),
                request.getCategory()
        );
    }

    public List<String> fetchCategoriesByProfileId(Long profileId) {
        Profile profile = profileMapper.findById(profileId);
        if (profile == null) {
            throw new ResourceNotFoundException("Profile not found");
        }

        return skillMapper.findCategoriesByProfileId(profileId);
    }

    public Skill updateSkill(Long id, UpdateSkillDTO request) {
        Skill existingSkill = skillMapper.findById(id);
        if (existingSkill == null) {
            throw new ResourceNotFoundException("Skill not found");
        }

        Skill update = new Skill();
        update.setId(id);
        update.setCategory(request.getCategory());
        update.setName(request.getName());

        try{
            skillMapper.updateById(update);
        } catch (DataIntegrityViolationException e) {
            log.warn("Failed to update skill because it already exists, skillId={}", id, e);
            throw new BadRequestException("Skill already exists.");
        }

        Profile profile = profileMapper.findById(existingSkill.getProfileId());
        if (profile != null) {
            resumeMapper.markResumeDirtyByUserId(profile.getUserId());
            profileEmbeddingChunkService.syncSkillChunks(profile.getUserId(), profile.getId());
        }
        return skillMapper.findById(id);
    }

    public void deleteSkill(Long id) {
        Skill existingSkill = skillMapper.findById(id);
        if (existingSkill == null) {
            throw new ResourceNotFoundException("Skill not found");
        }

        skillMapper.deleteById(id);
        Profile profile = profileMapper.findById(existingSkill.getProfileId());
        if (profile != null) {
            resumeMapper.markResumeDirtyByUserId(profile.getUserId());
            profileEmbeddingChunkService.syncSkillChunks(profile.getUserId(), profile.getId());
        }
    }

    public SkillImportResponseDTO importSkillsFromCsv(Long profileId, MultipartFile file) {
        if (profileId == null) {
            throw new BadRequestException("profileId is required");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CSV file must not be empty");
        }

        Profile profile = profileMapper.findById(profileId);
        if (profile == null) {
            throw new ResourceNotFoundException("Profile not found");
        }

        int successCount = 0;
        int failedCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;

            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    continue;
                }

                String[] columns = line.split(",", -1);
                if (columns.length > 2) {
                    failedCount++;
                    continue;
                }

                String category = columns[0].trim();
                String name = columns.length > 1 ? columns[1].trim() : "";

                if (category.isEmpty() && name.isEmpty()) {
                    continue;
                }
                if (name.isEmpty()) {
                    failedCount++;
                    continue;
                }

                Skill skill = new Skill();
                skill.setProfileId(profileId);
                skill.setCategory(category.isEmpty() ? null : category);
                skill.setName(name);

                try {
                    if (skillMapper.insert(skill) == 0) {
                        throw new BadRequestException("Skill already exists.");
                    }
                    successCount++;
                } catch (BadRequestException ex) {
                    throw ex;
                } catch (RuntimeException ex) {
                    log.warn("Failed to import skill from CSV line for profileId={}, category={}, name={}",
                            profileId, category, name, ex);
                    failedCount++;
                }
            }
        } catch (IOException ex) {
            log.warn("Failed to read skill CSV file for profileId={}", profileId, ex);
            throw new BadRequestException("Failed to read CSV file");
        }

        if (successCount > 0) {
            resumeMapper.markResumeDirtyByUserId(profile.getUserId());

            profileEmbeddingChunkService.syncSkillChunks(
                    profile.getUserId(),
                    profile.getId()
            );
        }

        return new SkillImportResponseDTO(successCount, failedCount);
    }

}
