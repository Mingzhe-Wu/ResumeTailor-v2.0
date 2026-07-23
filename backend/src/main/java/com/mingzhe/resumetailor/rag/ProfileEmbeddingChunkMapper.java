package com.mingzhe.resumetailor.rag;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProfileEmbeddingChunkMapper {

    @Insert("""
        INSERT INTO profile_embedding_chunks (
            user_id,
            source_type,
            source_id,
            chunk_key,
            content_text,
            embedding_model,
            embedding_status
        ) VALUES (
            #{userId},
            CAST(#{sourceType} AS embedding_source_type),
            #{sourceId},
            #{chunkKey},
            #{contentText},
            #{embeddingModel},
            CAST(#{embeddingStatus} AS embedding_status)
        )
        ON CONFLICT DO NOTHING
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProfileEmbeddingChunk chunk);

    @Select("""
        SELECT
            id,
            user_id,
            source_type,
            source_id,
            chunk_key,
            content_text,
            embedding,
            embedding_model,
            embedding_status,
            created_at,
            updated_at
        FROM profile_embedding_chunks
        WHERE id = #{id}
        """)
    @Results(id = "ProfileEmbeddingChunkResultMap", value = {
            @Result(column = "embedding", property = "embedding",
                    typeHandler = PgVectorTypeHandler.class)
    })
    ProfileEmbeddingChunk findById(Long id);

    @Select("""
        SELECT
            id,
            user_id,
            source_type,
            source_id,
            chunk_key,
            content_text,
            embedding,
            embedding_model,
            embedding_status,
            created_at,
            updated_at
        FROM profile_embedding_chunks
        WHERE user_id = #{userId}
        """)
    @ResultMap("ProfileEmbeddingChunkResultMap")
    List<ProfileEmbeddingChunk> findByUserId(Long userId);

    @Select("""
        SELECT
            id,
            user_id,
            source_type,
            source_id,
            chunk_key,
            content_text,
            embedding,
            embedding_model,
            embedding_status,
            created_at,
            updated_at
        FROM profile_embedding_chunks
        WHERE user_id = #{userId}
          AND embedding_status = CAST(#{embeddingStatus} AS embedding_status)
        """)
    @ResultMap("ProfileEmbeddingChunkResultMap")
    List<ProfileEmbeddingChunk> findByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("embeddingStatus") EmbeddingStatus embeddingStatus
    );

    @Select("""
        SELECT
            id,
            user_id,
            source_type,
            source_id,
            chunk_key,
            content_text
        FROM profile_embedding_chunks
        WHERE user_id = #{userId}
          AND source_type = CAST(#{sourceType} AS embedding_source_type)
          AND source_id = #{sourceId}
        ORDER BY id
        """)
    @ResultMap("ProfileEmbeddingChunkResultMap")
    List<ProfileEmbeddingChunk> findByUserAndSource(
            @Param("userId") Long userId,
            @Param("sourceType") EmbeddingSourceType sourceType,
            @Param("sourceId") Long sourceId
    );

    @Delete("""
        DELETE FROM profile_embedding_chunks
        WHERE user_id = #{userId}
          AND source_type = CAST(#{sourceType} AS embedding_source_type)
          AND source_id = #{sourceId}
        """)
    int deleteByUserAndSource(
            @Param("userId") Long userId,
            @Param("sourceType") EmbeddingSourceType sourceType,
            @Param("sourceId") Long sourceId
    );

    @Delete("""
        DELETE FROM profile_embedding_chunks
        WHERE id = #{id}
        """)
    int deleteById(Long id);

    @Delete("""
        DELETE FROM profile_embedding_chunks chunk
        WHERE chunk.user_id = #{userId}
          AND chunk.source_type = CAST('EXPERIENCE' AS embedding_source_type)
          AND NOT EXISTS (
              SELECT 1
              FROM experiences experience
              JOIN profiles profile ON profile.id = experience.profile_id
              WHERE experience.id = chunk.source_id
                AND profile.user_id = chunk.user_id
          )
        """)
    int deleteOrphanExperienceChunks(Long userId);

    @Delete("""
        DELETE FROM profile_embedding_chunks chunk
        WHERE chunk.user_id = #{userId}
          AND chunk.source_type = CAST('PROJECT' AS embedding_source_type)
          AND NOT EXISTS (
              SELECT 1
              FROM projects project
              JOIN profiles profile ON profile.id = project.profile_id
              WHERE project.id = chunk.source_id
                AND profile.user_id = chunk.user_id
          )
        """)
    int deleteOrphanProjectChunks(Long userId);

    @Delete("""
        DELETE FROM profile_embedding_chunks
        WHERE user_id = #{userId}
          AND source_type IN (
              CAST('EXPERIENCE' AS embedding_source_type),
              CAST('PROJECT' AS embedding_source_type),
              CAST('SKILL' AS embedding_source_type)
          )
        """)
    int deleteRagProfileChunksByUserId(Long userId);

    @Update("""
        UPDATE profile_embedding_chunks
        SET chunk_key = #{chunkKey}
        WHERE id = #{id}
          AND chunk_key IS NULL
        """)
    int setChunkKeyIfMissing(@Param("id") Long id, @Param("chunkKey") String chunkKey);

    @Update("""
        UPDATE profile_embedding_chunks
        SET chunk_key = #{chunkKey},
            content_text = #{contentText},
            embedding = NULL,
            embedding_model = NULL,
            embedding_status = CAST('PENDING' AS embedding_status),
            updated_at = NOW()
        WHERE id = #{id}
        """)
    int updateContentAndMarkPending(
            @Param("id") Long id,
            @Param("chunkKey") String chunkKey,
            @Param("contentText") String contentText
    );

    @Update("""
        UPDATE profile_embedding_chunks
        SET embedding = NULL,
            embedding_model = NULL,
            embedding_status = CAST('PENDING' AS embedding_status),
            updated_at = NOW()
        WHERE user_id = #{userId}
          AND embedding_status = CAST('READY' AS embedding_status)
          AND (
              embedding IS NULL
              OR embedding_model IS NULL
              OR embedding_model <> #{embeddingModel}
          )
        """)
    int markIncompatibleReadyChunksPending(
            @Param("userId") Long userId,
            @Param("embeddingModel") String embeddingModel
    );

    @Update("""
        <script>
        UPDATE profile_embedding_chunks
        <set>
            <if test="embedding != null">
                embedding = #{embedding, typeHandler=com.mingzhe.resumetailor.rag.PgVectorTypeHandler},
            </if>
            <if test="embeddingModel != null">
                embedding_model = #{embeddingModel},
            </if>
            <if test="embeddingStatus != null">
                embedding_status = CAST(#{embeddingStatus} AS embedding_status),
            </if>
            updated_at = NOW()
        </set>
        WHERE id = #{id}
        </script>
        """)
    int updateById(ProfileEmbeddingChunk chunk);

    @Select("""
        SELECT
            id,
            user_id,
            source_type,
            source_id,
            content_text,
            embedding <=> #{queryEmbedding, typeHandler=com.mingzhe.resumetailor.rag.PgVectorTypeHandler} AS distance
        FROM profile_embedding_chunks
        WHERE user_id = #{userId}
          AND embedding_status = CAST('READY' AS embedding_status)
          AND embedding IS NOT NULL
        ORDER BY embedding <=> #{queryEmbedding, typeHandler=com.mingzhe.resumetailor.rag.PgVectorTypeHandler}
        LIMIT #{topK}
        """)
    @Results(id = "RetrievedChunkResultMap", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "source_type", property = "sourceType"),
            @Result(column = "source_id", property = "sourceId"),
            @Result(column = "content_text", property = "contentText"),
            @Result(column = "distance", property = "distance")
    })
    List<RetrievedChunkDTO> findTopKReadyChunksByEmbedding(
            @Param("userId") Long userId,
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("topK") int topK
    );

    @Select("""
        SELECT
            id,
            user_id,
            source_type,
            source_id,
            content_text,
            embedding <=> #{queryEmbedding, typeHandler=com.mingzhe.resumetailor.rag.PgVectorTypeHandler} AS distance
        FROM profile_embedding_chunks
        WHERE user_id = #{userId}
          AND source_type = CAST('SKILL' AS public."embedding_source_type")
          AND embedding_status = CAST('READY' AS public."embedding_status")
          AND embedding IS NOT NULL
        ORDER BY embedding <=> #{queryEmbedding, typeHandler=com.mingzhe.resumetailor.rag.PgVectorTypeHandler}
        LIMIT #{topK}
        """)
    @ResultMap("RetrievedChunkResultMap")
    List<RetrievedChunkDTO> findTopKReadySkillChunksByEmbedding(
            @Param("userId") Long userId,
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("topK") int topK
    );

    @Select("""
        SELECT
            id,
            user_id,
            source_type,
            source_id,
            content_text,
            embedding <=> #{queryEmbedding, typeHandler=com.mingzhe.resumetailor.rag.PgVectorTypeHandler} AS distance
        FROM profile_embedding_chunks
        WHERE user_id = #{userId}
          AND source_type IN (
              CAST('EXPERIENCE' AS public."embedding_source_type"),
              CAST('PROJECT' AS public."embedding_source_type")
          )
          AND embedding_status = CAST('READY' AS public."embedding_status")
          AND embedding IS NOT NULL
        ORDER BY embedding <=> #{queryEmbedding, typeHandler=com.mingzhe.resumetailor.rag.PgVectorTypeHandler}
        LIMIT #{topK}
        """)
    @ResultMap("RetrievedChunkResultMap")
    List<RetrievedChunkDTO> findTopKReadyExperienceAndProjectChunksByEmbedding(
            @Param("userId") Long userId,
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("topK") int topK
    );
}
