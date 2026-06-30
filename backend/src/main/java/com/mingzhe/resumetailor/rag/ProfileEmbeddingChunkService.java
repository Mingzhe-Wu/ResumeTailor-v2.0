package com.mingzhe.resumetailor.rag;

import com.mingzhe.resumetailor.skill.Skill;
import com.mingzhe.resumetailor.skill.SkillMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProfileEmbeddingChunkService {
    private final ProfileEmbeddingChunkMapper profileEmbeddingChunkMapper;
    private final BulletChunkParser bulletChunkParser;

    private final SkillMapper skillMapper;

    public ProfileEmbeddingChunkService(ProfileEmbeddingChunkMapper profileEmbeddingChunkMapper, BulletChunkParser bulletChunkParser, SkillMapper skillMapper) {
        this.profileEmbeddingChunkMapper = profileEmbeddingChunkMapper;
        this.bulletChunkParser = bulletChunkParser;
        this.skillMapper = skillMapper;
    }

    // Inserting or updating bullet chunks for given user and source
    private void syncBulletChunks(
            Long userId,
            EmbeddingSourceType sourceType,
            Long sourceId,
            String bulletText
    ) {
        // If current source already embedded, delete the current embedding for data integrity
        profileEmbeddingChunkMapper.deleteByUserAndSource(userId, sourceType, sourceId);

        // Parse bullet point by *
        List<String> bullets = bulletChunkParser.parseBullets(bulletText);

        for (String bullet : bullets) {
            ProfileEmbeddingChunk chunk = new ProfileEmbeddingChunk();
            chunk.setUserId(userId);
            chunk.setSourceType(sourceType);
            chunk.setSourceId(sourceId);
            chunk.setContentText(bullet);
            chunk.setEmbeddingStatus(EmbeddingStatus.PENDING);

            profileEmbeddingChunkMapper.insert(chunk);
        }
    }

    @Transactional
    public void syncExperienceChunks(Long userId, Long experienceId, String bulletText) {
        syncBulletChunks(userId, EmbeddingSourceType.EXPERIENCE, experienceId, bulletText);
    }

    @Transactional
    public void syncProjectChunks(Long userId, Long projectId, String bulletText) {
        syncBulletChunks(userId, EmbeddingSourceType.PROJECT, projectId, bulletText);
    }

    @Transactional
    public void deleteExperienceChunks(Long userId, Long experienceId) {
        profileEmbeddingChunkMapper.deleteByUserAndSource(
                userId,
                EmbeddingSourceType.EXPERIENCE,
                experienceId
        );
    }

    @Transactional
    public void deleteProjectChunks(Long userId, Long projectId) {
        profileEmbeddingChunkMapper.deleteByUserAndSource(
                userId,
                EmbeddingSourceType.PROJECT,
                projectId
        );
    }

    @Transactional
    public void syncSkillChunks(Long userId, Long profileId) {
        profileEmbeddingChunkMapper.deleteByUserAndSource(
                userId,
                EmbeddingSourceType.SKILL,
                profileId
        );

        List<Skill> skills = skillMapper.searchSkills(profileId, null, null);

        if (skills == null || skills.isEmpty()) {
            return;
        }

        Map<String, List<Skill>> skillsByCategory = skills.stream()
                .filter(skill -> skill.getCategory() != null && !skill.getCategory().isBlank())
                .filter(skill -> skill.getName() != null && !skill.getName().isBlank())
                .collect(Collectors.groupingBy(Skill::getCategory));

        for (Map.Entry<String, List<Skill>> entry : skillsByCategory.entrySet()) {
            String category = entry.getKey();

            String skillNames = entry.getValue().stream()
                    .map(Skill::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));

            if (skillNames.isBlank()) {
                continue;
            }

            ProfileEmbeddingChunk chunk = new ProfileEmbeddingChunk();
            chunk.setUserId(userId);
            chunk.setSourceType(EmbeddingSourceType.SKILL);
            chunk.setSourceId(profileId);
            chunk.setContentText(category + ": " + skillNames);
            chunk.setEmbeddingStatus(EmbeddingStatus.PENDING);

            profileEmbeddingChunkMapper.insert(chunk);
        }
    }

}
