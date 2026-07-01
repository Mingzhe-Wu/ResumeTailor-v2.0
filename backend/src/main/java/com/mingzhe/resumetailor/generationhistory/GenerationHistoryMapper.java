package com.mingzhe.resumetailor.generationhistory;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface GenerationHistoryMapper {

    @Insert("""
        INSERT INTO generation_history (
            user_id,
            job_id,
            resume_version_id,
            generation_method,
            prompt_template_id,
            model_name,
            status,
            error_message,
            input_token_count,
            output_token_count,
            estimated_cost_usd
        ) VALUES (
            #{userId},
            #{jobId},
            #{resumeVersionId},
            #{generationMethod},
            #{promptTemplateId},
            #{modelName},
            #{status},
            #{errorMessage},
            #{inputTokenCount},
            #{outputTokenCount},
            #{estimatedCostUsd}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GenerationHistory generationHistory);
}
