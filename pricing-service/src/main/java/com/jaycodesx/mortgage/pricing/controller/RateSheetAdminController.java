package com.jaycodesx.mortgage.pricing.controller;

import com.jaycodesx.mortgage.infrastructure.security.ServiceTokenValidator;
import com.jaycodesx.mortgage.pricing.dto.RateSheetPublishRequestDto;
import com.jaycodesx.mortgage.pricing.dto.RateSheetPublishResponseDto;
import com.jaycodesx.mortgage.pricing.model.RateSheetPublication;
import com.jaycodesx.mortgage.pricing.service.RateSheetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/rate-sheets")
public class RateSheetAdminController {

    private final RateSheetService rateSheetService;
    private final ServiceTokenValidator serviceTokenValidator;

    public RateSheetAdminController(
            RateSheetService rateSheetService,
            ServiceTokenValidator serviceTokenValidator
    ) {
        this.rateSheetService = rateSheetService;
        this.serviceTokenValidator = serviceTokenValidator;
    }

    @PostMapping
    public ResponseEntity<RateSheetPublishResponseDto> publishRateSheet(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody RateSheetPublishRequestDto request
    ) {
        serviceTokenValidator.validatePricingTokenHeader(authorizationHeader);
        RateSheetPublication published = rateSheetService.publish(
                request.investorId(),
                request.effectiveAt(),
                request.expiresAt(),
                request.source(),
                request.entries()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(RateSheetPublishResponseDto.from(published));
    }
}
