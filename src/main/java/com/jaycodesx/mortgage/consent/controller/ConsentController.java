package com.jaycodesx.mortgage.consent.controller;

import com.jaycodesx.mortgage.consent.dto.AuthConsentRequestDto;
import com.jaycodesx.mortgage.consent.service.ConsentAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Records consent captured at authentication surfaces (sign-in / registration).
 * Lead-submission consent is captured separately during quote refinement.
 */
@RestController
@RequestMapping("/consents")
public class ConsentController {

    private final ConsentAuditLogService consentAuditLogService;

    public ConsentController(ConsentAuditLogService consentAuditLogService) {
        this.consentAuditLogService = consentAuditLogService;
    }

    @PostMapping
    public ResponseEntity<Void> recordAuthConsents(
            @RequestBody AuthConsentRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        consentAuditLogService.recordAuthConsents(request, ip, userAgent);
        return ResponseEntity.accepted().build();
    }
}
