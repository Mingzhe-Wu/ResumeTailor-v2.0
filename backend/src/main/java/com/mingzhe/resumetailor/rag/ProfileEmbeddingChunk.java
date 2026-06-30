package com.mingzhe.resumetailor.rag;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents the embedded chunk information stored in database
 */
@Data
public class ProfileEmbeddingChunk {

    private Long id;

    private Long userId;

    private EmbeddingSourceType sourceType;

    private Long sourceId;

    private String contentText;

    private float[] embedding;

    private String embeddingModel;

    private EmbeddingStatus embeddingStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}