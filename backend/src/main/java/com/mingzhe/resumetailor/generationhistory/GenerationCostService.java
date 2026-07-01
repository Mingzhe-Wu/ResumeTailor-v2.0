package com.mingzhe.resumetailor.generationhistory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class GenerationCostService {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    private final BigDecimal inputPricePerMillion;
    private final BigDecimal outputPricePerMillion;

    public GenerationCostService(
            @Value("${ai.pricing.input-per-million:0}") BigDecimal inputPricePerMillion,
            @Value("${ai.pricing.output-per-million:0}") BigDecimal outputPricePerMillion
    ) {
        this.inputPricePerMillion = inputPricePerMillion;
        this.outputPricePerMillion = outputPricePerMillion;
    }

    public BigDecimal estimateCostUsd(Integer inputTokens, Integer outputTokens) {
        if (inputTokens == null && outputTokens == null) {
            return null;
        }

        BigDecimal inputCost = calculateTokenCost(inputTokens, inputPricePerMillion);
        BigDecimal outputCost = calculateTokenCost(outputTokens, outputPricePerMillion);

        return inputCost
                .add(outputCost)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTokenCost(Integer tokenCount, BigDecimal pricePerMillion) {
        if (tokenCount == null || pricePerMillion == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(tokenCount)
                .multiply(pricePerMillion)
                .divide(ONE_MILLION, 12, RoundingMode.HALF_UP);
    }
}
