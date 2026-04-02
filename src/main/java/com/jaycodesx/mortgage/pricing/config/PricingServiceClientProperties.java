package com.jaycodesx.mortgage.pricing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pricing-service")
public record PricingServiceClientProperties(String baseUrl) {
}
