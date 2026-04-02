package com.jaycodesx.mortgage.infrastructure.admin;

import java.math.BigDecimal;

public record AdminRateSheetEntryDto(
        String productTermId,
        BigDecimal rate,
        BigDecimal price
) {
}
