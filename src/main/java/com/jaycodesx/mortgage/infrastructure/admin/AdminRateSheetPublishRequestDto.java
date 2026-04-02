package com.jaycodesx.mortgage.infrastructure.admin;

import java.time.LocalDateTime;
import java.util.List;

public record AdminRateSheetPublishRequestDto(
        String investorId,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt,
        String source,
        List<AdminRateSheetEntryDto> entries
) {
}
