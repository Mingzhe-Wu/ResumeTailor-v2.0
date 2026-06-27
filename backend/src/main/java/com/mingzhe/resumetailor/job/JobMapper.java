package com.mingzhe.resumetailor.job;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * MyBatis mapper for Job database operations.
 */
@Mapper
public interface JobMapper {

    @Insert("""
        INSERT INTO jobs (
            user_id,
            title,
            company,
            location,
            salary,
            job_description,
            source_url,
            status,
            interview_time,
            priority,
            notes
        ) VALUES (
            #{userId},
            #{title},
            #{company},
            #{location},
            #{salary},
            #{jobDescription},
            #{sourceUrl},
            CAST(CASE #{status}
                WHEN 1 THEN 'SAVED'
                WHEN 2 THEN 'APPLIED'
                WHEN 3 THEN 'INTERVIEWING'
                WHEN 4 THEN 'OFFER'
                WHEN 5 THEN 'REJECTED'
                ELSE 'SAVED'
            END AS job_status),
            #{interviewTime},
            #{priority},
            #{notes}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Job job);

    @Select("""
        SELECT
            id,
            user_id,
            title,
            company,
            location,
            salary,
            job_description,
            source_url,
            CASE status::text
                WHEN 'SAVED' THEN 1
                WHEN 'APPLIED' THEN 2
                WHEN 'INTERVIEWING' THEN 3
                WHEN 'OFFER' THEN 4
                WHEN 'REJECTED' THEN 5
            END AS status,
            interview_time,
            priority,
            notes,
            created_at,
            updated_at
        FROM jobs
        WHERE id = #{id}
        """)
    Job findById(Long id);

    @Select("""
        SELECT
            id,
            user_id,
            title,
            company,
            location,
            salary,
            job_description,
            source_url,
            CASE status::text
                WHEN 'SAVED' THEN 1
                WHEN 'APPLIED' THEN 2
                WHEN 'INTERVIEWING' THEN 3
                WHEN 'OFFER' THEN 4
                WHEN 'REJECTED' THEN 5
            END AS status,
            interview_time,
            priority,
            notes,
            created_at,
            updated_at
        FROM jobs
        WHERE user_id = #{userId}
        ORDER BY created_at DESC, id DESC
        """)
    List<Job> findByUserId(Long userId);

    @Update("""
        <script>
        UPDATE jobs
        <set>
            <if test="title != null">title = #{title},</if>
            <if test="company != null">company = #{company},</if>
            <if test="location != null">location = #{location},</if>
            <if test="salary != null">salary = #{salary},</if>
            <if test="jobDescription != null">job_description = #{jobDescription},</if>
            <if test="sourceUrl != null">source_url = #{sourceUrl},</if>
            <if test="status != null">status = CAST(CASE #{status}
                WHEN 1 THEN 'SAVED'
                WHEN 2 THEN 'APPLIED'
                WHEN 3 THEN 'INTERVIEWING'
                WHEN 4 THEN 'OFFER'
                WHEN 5 THEN 'REJECTED'
            END AS job_status),</if>
            <if test="interviewTime != null">interview_time = #{interviewTime},</if>
            <if test="priority != null">priority = #{priority},</if>
            <if test="notes != null">notes = #{notes},</if>
            updated_at = NOW()
        </set>
        WHERE id = #{id}
        </script>
        """)
    int updateById(Job job);

    @Delete("""
        DELETE FROM jobs
        WHERE id = #{id}
        """)
    int deleteById(Long id);

}
