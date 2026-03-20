package com.jaycodesx.mortgage.directory.dto;

import java.math.BigDecimal;

public record DirectoryLenderResponseDto(
        Long id,
        String stateCode,
        String countyName,
        String institutionName,
        String contactName,
        String email,
        String phone,
        String licenseNumber,
        String nmlsId,
        String loanTypes,
        Integer minCreditScore,
        Integer avgSlaHours,
        BigDecimal rating
) {}
