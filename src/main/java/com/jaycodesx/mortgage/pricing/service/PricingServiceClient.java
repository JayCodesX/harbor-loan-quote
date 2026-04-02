package com.jaycodesx.mortgage.pricing.service;

import com.jaycodesx.mortgage.pricing.config.PricingServiceClientProperties;
import com.jaycodesx.mortgage.pricing.dto.QuoteCalculationRequestDto;
import com.jaycodesx.mortgage.pricing.dto.QuoteCalculationResponseDto;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@EnableConfigurationProperties(PricingServiceClientProperties.class)
public class PricingServiceClient {

    private final RestClient restClient;

    public PricingServiceClient(PricingServiceClientProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    public QuoteCalculationResponseDto calculate(QuoteCalculationRequestDto request) {
        return restClient.post()
                .uri("/internal/quotes/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(QuoteCalculationResponseDto.class);
    }
}
