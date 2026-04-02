package com.jaycodesx.mortgage.pricing.dto;

import com.jaycodesx.mortgage.pricing.model.RateSheetPublication;

import java.time.LocalDateTime;

public record RateSheetPublishResponseDto(
        Long id,
        String investorId,
        String status,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt,
        LocalDateTime importedAt
) {
    public static RateSheetPublishResponseDto from(RateSheetPublication p) {
        return new RateSheetPublishResponseDto(
                p.getId(),
                p.getInvestorId(),
                p.getStatus(),
                p.getEffectiveAt(),
                p.getExpiresAt(),
                p.getImportedAt()
        );
    }
}
