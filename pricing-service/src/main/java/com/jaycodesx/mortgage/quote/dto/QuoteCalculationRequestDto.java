package com.jaycodesx.mortgage.quote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record QuoteCalculationRequestDto(
        @NotBlank String jobType,
        @NotNull Long quoteId,
        @NotNull @Positive BigDecimal homePrice,
        @NotNull @PositiveOrZero BigDecimal downPayment,
        @NotBlank String zipCode,
        @NotBlank String loanProgram,
        @NotBlank String propertyUse,
        @NotNull @Positive Integer termYears,
        // Borrower financial inputs — required for REFINED_QUOTE, null for PUBLIC_QUOTE
        Integer creditScore,
        BigDecimal annualIncome,
        BigDecimal monthlyDebts,
        BigDecimal cashReserves,
        Boolean firstTimeBuyer,
        Boolean vaEligible
) {
}
