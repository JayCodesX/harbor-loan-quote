package com.jaycodesx.mortgage.quote.controller;

import com.jaycodesx.mortgage.infrastructure.security.ServiceTokenValidator;
import com.jaycodesx.mortgage.quote.dto.QuoteCalculationRequestDto;
import com.jaycodesx.mortgage.quote.dto.QuoteCalculationResponseDto;
import com.jaycodesx.mortgage.quote.service.QuoteJobProcessor;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/quotes")
public class QuoteCalculationController {

    private final QuoteJobProcessor quoteJobProcessor;
    private final ServiceTokenValidator serviceTokenValidator;

    public QuoteCalculationController(QuoteJobProcessor quoteJobProcessor, ServiceTokenValidator serviceTokenValidator) {
        this.quoteJobProcessor = quoteJobProcessor;
        this.serviceTokenValidator = serviceTokenValidator;
    }

    @PostMapping("/calculate")
    public ResponseEntity<QuoteCalculationResponseDto> calculate(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody QuoteCalculationRequestDto request) {
        serviceTokenValidator.validatePricingTokenHeader(authorization);
        return ResponseEntity.ok(quoteJobProcessor.calculate(request));
    }
}
