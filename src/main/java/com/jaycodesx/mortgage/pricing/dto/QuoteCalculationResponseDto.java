package com.jaycodesx.mortgage.pricing.dto;

import java.math.BigDecimal;

public record QuoteCalculationResponseDto(
        Long quoteId,
        String quoteStage,
        String quoteStatus,
        BigDecimal estimatedRate,
        BigDecimal estimatedApr,
        BigDecimal financedAmount,
        BigDecimal estimatedMonthlyPayment,
        BigDecimal estimatedCashToClose,
        String qualificationTier
) {
}
