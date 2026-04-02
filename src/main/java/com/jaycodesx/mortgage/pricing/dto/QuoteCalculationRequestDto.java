package com.jaycodesx.mortgage.pricing.dto;

import java.math.BigDecimal;

public record QuoteCalculationRequestDto(
        String jobType,
        Long quoteId,
        BigDecimal homePrice,
        BigDecimal downPayment,
        String zipCode,
        String loanProgram,
        String propertyUse,
        Integer termYears,
        Integer creditScore,
        BigDecimal annualIncome,
        BigDecimal monthlyDebts,
        BigDecimal cashReserves,
        Boolean firstTimeBuyer,
        Boolean vaEligible
) {
}
