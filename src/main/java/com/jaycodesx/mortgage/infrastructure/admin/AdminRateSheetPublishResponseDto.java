package com.jaycodesx.mortgage.infrastructure.admin;

import java.time.LocalDateTime;

public record AdminRateSheetPublishResponseDto(
        Long id,
        String investorId,
        String status,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt,
        LocalDateTime importedAt
) {
}
