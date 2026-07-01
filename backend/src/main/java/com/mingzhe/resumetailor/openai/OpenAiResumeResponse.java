package com.mingzhe.resumetailor.openai;

import lombok.Data;

@Data
public class OpenAiResumeResponse {
    private String content;
    private Integer inputTokenCount;
    private Integer outputTokenCount;
}
