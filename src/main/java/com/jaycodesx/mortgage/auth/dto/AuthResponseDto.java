package com.jaycodesx.mortgage.auth.dto;

public record AuthResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn,
        String email,
        String role
) {
}
