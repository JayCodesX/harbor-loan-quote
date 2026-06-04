package com.jaycodesx.mortgage.consent.dto;

/**
 * Consent captured at an authentication surface (sign-in / registration), where there is
 * no specific loan quote yet. Booleans are nullable so a partially-filled payload still
 * records a deterministic GRANTED/REVOKED entry per consent type.
 */
public record AuthConsentRequestDto(
        String sourceSurface,
        Boolean termsOfService,
        Boolean privacyNotice,
        Boolean marketingEmail
) {
}
