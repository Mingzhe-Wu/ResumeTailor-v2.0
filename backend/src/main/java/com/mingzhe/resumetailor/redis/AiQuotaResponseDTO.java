package com.mingzhe.resumetailor.redis;

import lombok.Data;

@Data
public class AiQuotaResponseDTO {
    private long used;
    private long limit;
    private long remaining;
}
