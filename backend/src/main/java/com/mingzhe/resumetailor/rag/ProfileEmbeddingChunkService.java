package com.mingzhe.resumetailor.rag;

import com.mingzhe.resumetailor.skill.Skill;
import com.mingzhe.resumetailor.skill.SkillMapper;
import com.mingzhe.resumetailor.experience.Experience;
import com.mingzhe.resumetailor.experience.ExperienceMapper;
import com.mingzhe.resumetailor.profile.Profile;
import com.mingzhe.resumetailor.profile.ProfileMapper;
import com.mingzhe.resumetailor.project.Project;
import com.mingzhe.resumetailor.project.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Service
public class ProfileEmbeddingChunkService {
    private final ProfileEmbeddingChunkMapper profileEmbeddingChunkMapper;
    private final BulletChunkParser bulletChunkParser;

    private final SkillMapper skillMapper;
    private final ProfileMapper profileMapper;
    private final ExperienceMapper experienceMapper;
    private final ProjectMapper projectMapper;

    public ProfileEmbeddingChunkService(
            ProfileEmbeddingChunkMapper profileEmbeddingChunkMapper,
            BulletChunkParser bulletChunkParser,
            SkillMapper skillMapper,
            ProfileMapper profileMapper,
            ExperienceMapper experienceMapper,
            ProjectMapper projectMapper
    ) {
        this.profileEmbeddingChunkMapper = profileEmbeddingChunkMapper;
        this.bulletChunkParser = bulletChunkParser;
        this.skillMapper = skillMapper;
        this.profileMapper = profileMapper;
        this.experienceMapper = experienceMapper;
        this.projectMapper = projectMapper;
    }

    private void syncBulletChunks(
            Long userId,
            EmbeddingSourceType sourceType,
            Long sourceId,
            String bulletText
    ) {
        List<String> bullets = bulletChunkParser.parseBullets(bulletText);
        Map<String, Integer> occurrencesByHash = new HashMap<>();
        List<DesiredChunk> desiredChunks = new ArrayList<>();

        for (String bullet : bullets) {
            String contentHash = contentHash(bullet);
            int occurrence = occurrencesByHash.merge(contentHash, 1, Integer::sum) - 1;
            desiredChunks.add(new DesiredChunk(bulletChunkKey(contentHash, occurrence), bullet));
        }

        syncDesiredChunks(userId, sourceType, sourceId, desiredChunks);
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
        List<Skill> skills = skillMapper.searchSkills(profileId, null, null);
        Map<String, Set<String>> skillNamesByCategory = new TreeMap<>();

        if (skills != null) {
            for (Skill skill : skills) {
                if (skill == null || !hasText(skill.getCategory()) || !hasText(skill.getName())) {
                    continue;
                }

                String category = skill.getCategory().trim();
                String name = skill.getName().trim();
                skillNamesByCategory
                        .computeIfAbsent(category, ignored -> new TreeSet<>())
                        .add(name);
            }
        }

        List<DesiredChunk> desiredChunks = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : skillNamesByCategory.entrySet()) {
            String category = entry.getKey();
            String skillNames = String.join(", ", entry.getValue());

            if (skillNames.isBlank()) {
                continue;
            }

            desiredChunks.add(new DesiredChunk(skillChunkKey(category), category + ": " + skillNames));
        }

        syncDesiredChunks(userId, EmbeddingSourceType.SKILL, profileId, desiredChunks);
    }

    @Transactional
    public void syncAllProfileChunks(Long userId) {
        // Reconcile every source at generation time, but preserve unchanged
        // logical chunks so READY embeddings survive across RAG generations.
        Profile profile = profileMapper.findByUserId(userId);
        if (profile == null || profile.getId() == null) {
            profileEmbeddingChunkMapper.deleteRagProfileChunksByUserId(userId);
            return;
        }

        profileEmbeddingChunkMapper.deleteOrphanExperienceChunks(userId);
        profileEmbeddingChunkMapper.deleteOrphanProjectChunks(userId);

        List<Experience> experiences = experienceMapper.findByProfileId(profile.getId());
        if (experiences != null) {
            for (Experience experience : experiences) {
                syncBulletChunks(
                        userId,
                        EmbeddingSourceType.EXPERIENCE,
                        experience.getId(),
                        experience.getDescription()
                );
            }
        }

        List<Project> projects = projectMapper.findByProfileId(profile.getId());
        if (projects != null) {
            for (Project project : projects) {
                syncBulletChunks(
                        userId,
                        EmbeddingSourceType.PROJECT,
                        project.getId(),
                        project.getDescription()
                );
            }
        }

        syncSkillChunks(userId, profile.getId());
    }

    private void syncDesiredChunks(
            Long userId,
            EmbeddingSourceType sourceType,
            Long sourceId,
            List<DesiredChunk> desiredChunks
    ) {
        List<ProfileEmbeddingChunk> existingChunks = profileEmbeddingChunkMapper.findByUserAndSource(
                userId,
                sourceType,
                sourceId
        );
        if (existingChunks == null) {
            existingChunks = List.of();
        }

        Map<String, Deque<ProfileEmbeddingChunk>> existingByKey = indexExistingChunks(existingChunks, sourceType);
        Set<Long> retainedIds = new HashSet<>();

        for (DesiredChunk desired : desiredChunks) {
            Deque<ProfileEmbeddingChunk> candidates = existingByKey.get(desired.chunkKey());
            ProfileEmbeddingChunk existing = candidates == null ? null : candidates.pollFirst();

            if (existing == null) {
                insertPendingChunk(userId, sourceType, sourceId, desired);
                continue;
            }

            retainedIds.add(existing.getId());
            if (!desired.chunkKey().equals(existing.getChunkKey())) {
                profileEmbeddingChunkMapper.setChunkKeyIfMissing(existing.getId(), desired.chunkKey());
            }

            if (!desired.contentText().equals(existing.getContentText())) {
                profileEmbeddingChunkMapper.updateContentAndMarkPending(
                        existing.getId(),
                        desired.chunkKey(),
                        desired.contentText()
                );
            }
        }

        for (ProfileEmbeddingChunk existing : existingChunks) {
            if (!retainedIds.contains(existing.getId())) {
                profileEmbeddingChunkMapper.deleteById(existing.getId());
            }
        }
    }

    private Map<String, Deque<ProfileEmbeddingChunk>> indexExistingChunks(
            List<ProfileEmbeddingChunk> existingChunks,
            EmbeddingSourceType sourceType
    ) {
        Map<String, Deque<ProfileEmbeddingChunk>> existingByKey = new LinkedHashMap<>();
        Map<String, Integer> bulletOccurrencesByHash = new HashMap<>();

        for (ProfileEmbeddingChunk existing : existingChunks) {
            String key = existing.getChunkKey();
            if (!hasText(key)) {
                key = legacyChunkKey(existing, sourceType, bulletOccurrencesByHash);
            }
            if (!hasText(key)) {
                continue;
            }
            existingByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>()).add(existing);
        }

        return existingByKey;
    }

    private String legacyChunkKey(
            ProfileEmbeddingChunk existing,
            EmbeddingSourceType sourceType,
            Map<String, Integer> bulletOccurrencesByHash
    ) {
        if (!hasText(existing.getContentText())) {
            return null;
        }

        if (sourceType == EmbeddingSourceType.EXPERIENCE || sourceType == EmbeddingSourceType.PROJECT) {
            String hash = contentHash(existing.getContentText());
            int occurrence = bulletOccurrencesByHash.merge(hash, 1, Integer::sum) - 1;
            return bulletChunkKey(hash, occurrence);
        }

        if (sourceType == EmbeddingSourceType.SKILL) {
            int separatorIndex = existing.getContentText().lastIndexOf(": ");
            if (separatorIndex > 0) {
                return skillChunkKey(existing.getContentText().substring(0, separatorIndex).trim());
            }
        }

        return null;
    }

    private void insertPendingChunk(
            Long userId,
            EmbeddingSourceType sourceType,
            Long sourceId,
            DesiredChunk desired
    ) {
        ProfileEmbeddingChunk chunk = new ProfileEmbeddingChunk();
        chunk.setUserId(userId);
        chunk.setSourceType(sourceType);
        chunk.setSourceId(sourceId);
        chunk.setChunkKey(desired.chunkKey());
        chunk.setContentText(desired.contentText());
        chunk.setEmbeddingStatus(EmbeddingStatus.PENDING);
        profileEmbeddingChunkMapper.insert(chunk);
    }

    private String bulletChunkKey(String contentHash, int occurrence) {
        return "bullet:" + contentHash + ":" + occurrence;
    }

    private String skillChunkKey(String category) {
        return "skill:" + contentHash(category.trim());
    }

    private String contentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record DesiredChunk(String chunkKey, String contentText) {
    }

}
