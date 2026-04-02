package com.jaycodesx.mortgage.pricing.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RateSheetPublishRequestDto(
        String investorId,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt,
        String source,
        List<RateSheetEntryRequest> entries
) {
}
