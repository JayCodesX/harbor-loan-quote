package com.jaycodesx.mortgage.directory.dto;

import java.math.BigDecimal;

public record DirectoryAgentResponseDto(
        Long id,
        String stateCode,
        String countyName,
        String firstName,
        String lastName,
        String companyName,
        String email,
        String phone,
        String licenseNumber,
        String nmlsId,
        String specialty,
        Integer avgResponseHours,
        BigDecimal rating
) {}
