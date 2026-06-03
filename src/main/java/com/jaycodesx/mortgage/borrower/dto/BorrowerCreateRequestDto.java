package com.jaycodesx.mortgage.borrower.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BorrowerCreateRequestDto(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email @NotBlank String email,
        @Min(300) @Max(850) Integer creditScore
) {
}
