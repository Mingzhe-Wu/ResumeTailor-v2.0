package com.mingzhe.resumetailor.rag;

import com.mingzhe.resumetailor.experience.ExperienceMapper;
import com.mingzhe.resumetailor.profile.ProfileMapper;
import com.mingzhe.resumetailor.project.ProjectMapper;
import com.mingzhe.resumetailor.skill.Skill;
import com.mingzhe.resumetailor.skill.SkillMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileEmbeddingChunkServiceTest {

    @Mock
    private ProfileEmbeddingChunkMapper chunkMapper;
    @Mock
    private SkillMapper skillMapper;
    @Mock
    private ProfileMapper profileMapper;
    @Mock
    private ExperienceMapper experienceMapper;
    @Mock
    private ProjectMapper projectMapper;

    private ProfileEmbeddingChunkService service;

    @BeforeEach
    void setUp() {
        service = new ProfileEmbeddingChunkService(
                chunkMapper,
                new BulletChunkParser(),
                skillMapper,
                profileMapper,
                experienceMapper,
                projectMapper
        );
    }

    @Test
    void preservesReadyExperienceChunksWhenOnlyBulletOrderChanges() {
        long userId = 7L;
        long experienceId = 11L;
        when(chunkMapper.findByUserAndSource(userId, EmbeddingSourceType.EXPERIENCE, experienceId))
                .thenReturn(List.of());

        service.syncExperienceChunks(userId, experienceId, "* Built APIs\n* Improved latency");

        List<ProfileEmbeddingChunk> existing = capturedInsertedChunks(2);
        markReady(existing);
        clearInvocations(chunkMapper);
        when(chunkMapper.findByUserAndSource(userId, EmbeddingSourceType.EXPERIENCE, experienceId))
                .thenReturn(existing);

        service.syncExperienceChunks(userId, experienceId, "* Improved latency\n* Built APIs");

        verify(chunkMapper, never()).insert(any(ProfileEmbeddingChunk.class));
        verify(chunkMapper, never()).updateContentAndMarkPending(anyLong(), any(), any());
        verify(chunkMapper, never()).deleteById(anyLong());
        verify(chunkMapper, never()).setChunkKeyIfMissing(anyLong(), any());
    }

    @Test
    void onlyRebuildsTheChangedExperienceBullet() {
        long userId = 7L;
        long experienceId = 12L;
        when(chunkMapper.findByUserAndSource(userId, EmbeddingSourceType.EXPERIENCE, experienceId))
                .thenReturn(List.of());

        service.syncExperienceChunks(userId, experienceId, "* Kept bullet\n* Old bullet");

        List<ProfileEmbeddingChunk> existing = capturedInsertedChunks(2);
        markReady(existing);
        Long oldBulletId = existing.stream()
                .filter(chunk -> "Old bullet".equals(chunk.getContentText()))
                .findFirst()
                .orElseThrow()
                .getId();

        clearInvocations(chunkMapper);
        when(chunkMapper.findByUserAndSource(userId, EmbeddingSourceType.EXPERIENCE, experienceId))
                .thenReturn(existing);

        service.syncExperienceChunks(userId, experienceId, "* Kept bullet\n* New bullet");

        ArgumentCaptor<ProfileEmbeddingChunk> inserted = ArgumentCaptor.forClass(ProfileEmbeddingChunk.class);
        verify(chunkMapper).insert(inserted.capture());
        assertEquals("New bullet", inserted.getValue().getContentText());
        assertEquals(EmbeddingStatus.PENDING, inserted.getValue().getEmbeddingStatus());
        verify(chunkMapper).deleteById(oldBulletId);
        verify(chunkMapper, never()).updateContentAndMarkPending(anyLong(), any(), any());
    }

    @Test
    void updatesOnlyTheChangedSkillCategory() {
        long userId = 7L;
        long profileId = 21L;
        when(skillMapper.searchSkills(profileId, null, null)).thenReturn(List.of(
                skill("Java", "Spring"),
                skill("Java", "Java"),
                skill("Cloud", "AWS")
        ));
        when(chunkMapper.findByUserAndSource(userId, EmbeddingSourceType.SKILL, profileId))
                .thenReturn(List.of());

        service.syncSkillChunks(userId, profileId);

        List<ProfileEmbeddingChunk> existing = capturedInsertedChunks(2);
        markReady(existing);
        ProfileEmbeddingChunk javaChunk = existing.stream()
                .filter(chunk -> chunk.getContentText().startsWith("Java: "))
                .findFirst()
                .orElseThrow();

        clearInvocations(chunkMapper, skillMapper);
        when(skillMapper.searchSkills(profileId, null, null)).thenReturn(List.of(
                skill("Java", "Spring"),
                skill("Java", "Hibernate"),
                skill("Java", "Java"),
                skill("Cloud", "AWS")
        ));
        when(chunkMapper.findByUserAndSource(userId, EmbeddingSourceType.SKILL, profileId))
                .thenReturn(existing);

        service.syncSkillChunks(userId, profileId);

        verify(chunkMapper).updateContentAndMarkPending(
                javaChunk.getId(),
                javaChunk.getChunkKey(),
                "Java: Hibernate, Java, Spring"
        );
        verify(chunkMapper, never()).insert(any(ProfileEmbeddingChunk.class));
        verify(chunkMapper, never()).deleteById(anyLong());
    }

    @Test
    void backfillsLegacyChunkKeyWithoutInvalidatingReadyEmbedding() {
        long userId = 7L;
        long projectId = 31L;
        ProfileEmbeddingChunk legacy = chunk(101L, null, "Stable bullet");
        legacy.setEmbeddingStatus(EmbeddingStatus.READY);
        legacy.setEmbedding(new float[]{1.0f});
        legacy.setEmbeddingModel("text-embedding-3-small");
        when(chunkMapper.findByUserAndSource(userId, EmbeddingSourceType.PROJECT, projectId))
                .thenReturn(List.of(legacy));

        service.syncProjectChunks(userId, projectId, "* Stable bullet");

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(chunkMapper).setChunkKeyIfMissing(eq(legacy.getId()), key.capture());
        assertNotNull(key.getValue());
        verify(chunkMapper, never()).updateContentAndMarkPending(anyLong(), any(), any());
        verify(chunkMapper, never()).insert(any(ProfileEmbeddingChunk.class));
        verify(chunkMapper, never()).deleteById(anyLong());
    }

    private List<ProfileEmbeddingChunk> capturedInsertedChunks(int expectedCount) {
        ArgumentCaptor<ProfileEmbeddingChunk> captor = ArgumentCaptor.forClass(ProfileEmbeddingChunk.class);
        verify(chunkMapper, times(expectedCount)).insert(captor.capture());
        return new ArrayList<>(captor.getAllValues());
    }

    private void markReady(List<ProfileEmbeddingChunk> chunks) {
        long id = 100L;
        for (ProfileEmbeddingChunk chunk : chunks) {
            chunk.setId(id++);
            chunk.setEmbeddingStatus(EmbeddingStatus.READY);
            chunk.setEmbedding(new float[]{1.0f});
            chunk.setEmbeddingModel("text-embedding-3-small");
        }
    }

    private ProfileEmbeddingChunk chunk(Long id, String chunkKey, String contentText) {
        ProfileEmbeddingChunk chunk = new ProfileEmbeddingChunk();
        chunk.setId(id);
        chunk.setChunkKey(chunkKey);
        chunk.setContentText(contentText);
        return chunk;
    }

    private Skill skill(String category, String name) {
        Skill skill = new Skill();
        skill.setCategory(category);
        skill.setName(name);
        return skill;
    }
}
