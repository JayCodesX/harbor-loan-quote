package com.jaycodesx.mortgage.infrastructure.admin;

public record AdminPartnerResponseDto(
        Long id,
        String displayName,
        String companyName,
        String email,
        String phone,
        String stateCode,
        String countyName,
        String city,
        String specialty,
        String licenseNumber,
        String nmlsId,
        Integer rankingScore,
        Integer responseSlaHours,
        String languages,
        String websiteUrl,
        boolean active
) {}
