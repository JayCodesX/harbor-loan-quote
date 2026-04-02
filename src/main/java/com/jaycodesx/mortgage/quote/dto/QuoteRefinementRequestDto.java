package com.jaycodesx.mortgage.quote.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record QuoteRefinementRequestDto(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotNull @Positive BigDecimal annualIncome,
        @NotNull @Min(0) BigDecimal monthlyDebts,
        @NotNull @Min(300) @Max(850) Integer creditScore,
        @NotNull @Min(0) BigDecimal cashReserves,
        @NotNull Boolean firstTimeBuyer,
        @NotNull Boolean vaEligible,
        // Consent fields — captured at the moment of lead submission for TCPA compliance
        @NotNull Boolean tcpaConsent,
        Boolean emailOptIn,
        @NotNull Boolean leadShareConsent,
        @NotBlank String consentLanguage
) {
}
