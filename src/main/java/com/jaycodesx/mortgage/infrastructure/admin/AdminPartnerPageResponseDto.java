package com.jaycodesx.mortgage.infrastructure.admin;

import java.util.List;

public record AdminPartnerPageResponseDto(
        List<AdminPartnerResponseDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {}
