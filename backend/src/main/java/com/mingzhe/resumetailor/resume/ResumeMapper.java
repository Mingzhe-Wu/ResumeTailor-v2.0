package com.mingzhe.resumetailor.resume;

import org.apache.ibatis.annotations.*;

/**
 * MyBatis mapper for Resume database operations.
 */
@Mapper
public interface ResumeMapper {

    @Insert("""
        INSERT INTO resume_versions (
            job_id,
            match_score,
            generated_content
        ) VALUES (
            #{jobId},
            #{matchScore},
            #{generatedContent}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Resume resume);

    @Select("""
        SELECT
            id,
            job_id,
            match_score,
            generated_content,
            need_generate,
            created_at,
            updated_at
        FROM resume_versions
        WHERE id = #{id}
        """)
    Resume findById(Long id);

    @Select("""
        SELECT
            id,
            job_id,
            match_score,
            generated_content,
            need_generate,
            created_at,
            updated_at
        FROM resume_versions
        WHERE job_id = #{jobId}
        ORDER BY created_at DESC, id DESC
        LIMIT 1
        """)
    Resume findByJobId(Long jobId);

    @Update("""
        <script>
        UPDATE resume_versions
        <set>
            <if test="matchScore != null">match_score = #{matchScore},</if>
            <if test="generatedContent != null">generated_content = #{generatedContent},</if>
            <if test="needGenerate != null">need_generate = #{needGenerate},</if>
            updated_at = NOW()
        </set>
        WHERE id = #{id}
        </script>
        """)
    int updateById(Resume resume);

    @Update("""
        UPDATE resume_versions
        SET need_generate = TRUE,
            updated_at = NOW()
        WHERE job_id = #{jobId}
        """)
    int markResumeDirtyByJobId(Long jobId);

    @Update("""
        UPDATE resume_versions rv
        SET need_generate = TRUE,
            updated_at = NOW()
        FROM jobs j
        WHERE rv.job_id = j.id
          AND j.user_id = #{userId}
        """)
    int markResumeDirtyByUserId(Long userId);

    @Delete("""
        DELETE FROM resume_versions
        WHERE id = #{id}
        """)
    int deleteById(Long id);
}
