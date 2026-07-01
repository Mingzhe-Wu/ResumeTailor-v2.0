package com.mingzhe.resumetailor.redis;

import com.mingzhe.resumetailor.exceptions.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/redis/quota")
@CrossOrigin(origins = "http://localhost:5173")
public class RedisQuotaController {

    private final AiQuotaService aiQuotaService;

    public RedisQuotaController(AiQuotaService aiQuotaService) {
        this.aiQuotaService = aiQuotaService;
    }

    @GetMapping("/today")
    public ResponseEntity<AiQuotaResponseDTO> getTodayQuota(
            @RequestParam(required = false) Long userId
    ) {
        if (userId == null) {
            throw new BadRequestException("userId is required.");
        }

        AiQuotaResponseDTO response = new AiQuotaResponseDTO();
        response.setUsed(aiQuotaService.getDailyUsage(userId));
        response.setLimit(aiQuotaService.getDailyLimit());
        response.setRemaining(aiQuotaService.getDailyRemaining(userId));

        return ResponseEntity.ok(response);
    }
}
