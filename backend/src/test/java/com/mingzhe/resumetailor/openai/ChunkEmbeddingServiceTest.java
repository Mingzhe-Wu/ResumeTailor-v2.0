package com.mingzhe.resumetailor.openai;

import com.mingzhe.resumetailor.rag.EmbeddingStatus;
import com.mingzhe.resumetailor.rag.ProfileEmbeddingChunk;
import com.mingzhe.resumetailor.rag.ProfileEmbeddingChunkMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChunkEmbeddingServiceTest {

    @Test
    void warmRunDoesNotCallEmbeddingApiWhenNoChunksArePending() {
        ProfileEmbeddingChunkMapper mapper = mock(ProfileEmbeddingChunkMapper.class);
        ChunkEmbeddingService service = spy(new ChunkEmbeddingService(mapper));
        when(mapper.findByUserIdAndStatus(7L, EmbeddingStatus.PENDING)).thenReturn(List.of());

        service.embedPendingChunksByUserId(7L);

        verify(mapper).markIncompatibleReadyChunksPending(7L, "text-embedding-3-small");
        verify(service, never()).createEmbedding(anyString());
        verify(mapper, never()).updateById(any(ProfileEmbeddingChunk.class));
    }

    @Test
    void embedsAndPersistsOnlyPendingChunks() {
        ProfileEmbeddingChunkMapper mapper = mock(ProfileEmbeddingChunkMapper.class);
        ChunkEmbeddingService service = spy(new ChunkEmbeddingService(mapper));
        ProfileEmbeddingChunk pending = new ProfileEmbeddingChunk();
        pending.setId(42L);
        pending.setContentText("Built a service");
        pending.setEmbeddingStatus(EmbeddingStatus.PENDING);
        when(mapper.findByUserIdAndStatus(7L, EmbeddingStatus.PENDING)).thenReturn(List.of(pending));
        doReturn(new float[]{0.1f, 0.2f}).when(service).createEmbedding("Built a service");

        service.embedPendingChunksByUserId(7L);

        ArgumentCaptor<ProfileEmbeddingChunk> update = ArgumentCaptor.forClass(ProfileEmbeddingChunk.class);
        verify(mapper).updateById(update.capture());
        assertEquals(42L, update.getValue().getId());
        assertEquals(EmbeddingStatus.READY, update.getValue().getEmbeddingStatus());
        assertEquals("text-embedding-3-small", update.getValue().getEmbeddingModel());
        assertArrayEquals(new float[]{0.1f, 0.2f}, update.getValue().getEmbedding());
    }
}
