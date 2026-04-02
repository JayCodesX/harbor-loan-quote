package com.jaycodesx.mortgage.quote.controller;

import com.jaycodesx.mortgage.quote.dto.QuoteCalculationRequestDto;
import com.jaycodesx.mortgage.quote.dto.QuoteCalculationResponseDto;
import com.jaycodesx.mortgage.quote.service.QuoteJobProcessor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/quotes")
public class QuoteCalculationController {

    private final QuoteJobProcessor quoteJobProcessor;

    public QuoteCalculationController(QuoteJobProcessor quoteJobProcessor) {
        this.quoteJobProcessor = quoteJobProcessor;
    }

    @PostMapping("/calculate")
    public ResponseEntity<QuoteCalculationResponseDto> calculate(
            @Valid @RequestBody QuoteCalculationRequestDto request) {
        return ResponseEntity.ok(quoteJobProcessor.calculate(request));
    }
}
