package com.jaycodesx.mortgage.infrastructure.admin;

import com.jaycodesx.mortgage.infrastructure.security.ServiceTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@EnableConfigurationProperties(InternalServiceClientProperties.class)
public class AdminMetricsClientService {

    private final InternalServiceClientProperties properties;
    private final ServiceTokenService serviceTokenService;

    public AdminMetricsClientService(
            InternalServiceClientProperties properties,
            ServiceTokenService serviceTokenService
    ) {
        this.properties = properties;
        this.serviceTokenService = serviceTokenService;
    }

    public BorrowerMetricsResponseDto fetchBorrowerMetrics() {
        return get(properties.borrowerBaseUrl(), properties.borrowerSecret(), properties.borrowerAudience(), properties.borrowerScope(), BorrowerMetricsResponseDto.class);
    }

    public PricingMetricsResponseDto fetchPricingMetrics() {
        return get(properties.pricingBaseUrl(), properties.pricingSecret(), properties.pricingAudience(), properties.pricingScope(), PricingMetricsResponseDto.class);
    }

    public LeadMetricsResponseDto fetchLeadMetrics() {
        return get(properties.leadBaseUrl(), properties.leadSecret(), properties.leadAudience(), properties.leadScope(), LeadMetricsResponseDto.class);
    }

    public AuthMetricsResponseDto fetchAuthMetrics() {
        return get(properties.authBaseUrl(), properties.authSecret(), properties.authAudience(), properties.authScope(), AuthMetricsResponseDto.class);
    }

    public NotificationMetricsResponseDto fetchNotificationMetrics() {
        return get(properties.notificationBaseUrl(), properties.notificationSecret(), properties.notificationAudience(), properties.notificationScope(), NotificationMetricsResponseDto.class);
    }

    public List<BorrowerAdminResponseDto> fetchBorrowers() {
        return getList(properties.borrowerBaseUrl(), properties.borrowerSecret(), properties.borrowerAudience(), properties.borrowerScope(), "/internal/admin/borrowers");
    }

    public List<AdminPricingProductResponseDto> fetchProducts() {
        return getList(properties.pricingBaseUrl(), properties.pricingSecret(), properties.pricingAudience(), properties.pricingScope(), "/internal/admin/products");
    }

    public AdminPricingProductResponseDto createProduct(AdminPricingProductRequestDto request) {
        return RestClient.builder().baseUrl(properties.pricingBaseUrl()).build()
                .post()
                .uri("/internal/admin/products")
                .header(HttpHeaders.AUTHORIZATION, bearer(properties.pricingSecret(), properties.pricingAudience(), properties.pricingScope()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AdminPricingProductResponseDto.class);
    }

    public AdminPricingProductResponseDto updateProduct(Long id, AdminPricingProductRequestDto request) {
        return RestClient.builder().baseUrl(properties.pricingBaseUrl()).build()
                .put()
                .uri("/internal/admin/products/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(properties.pricingSecret(), properties.pricingAudience(), properties.pricingScope()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AdminPricingProductResponseDto.class);
    }

    public void deleteProduct(Long id) {
        RestClient.builder().baseUrl(properties.pricingBaseUrl()).build()
                .delete()
                .uri("/internal/admin/products/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(properties.pricingSecret(), properties.pricingAudience(), properties.pricingScope()))
                .retrieve()
                .toBodilessEntity();
    }

    private <T> T get(String baseUrl, String secret, String audience, String scope, Class<T> responseType) {
        return RestClient.builder().baseUrl(baseUrl).build()
                .get()
                .uri("/internal/admin/metrics")
                .header(HttpHeaders.AUTHORIZATION, bearer(secret, audience, scope))
                .retrieve()
                .body(responseType);
    }

    private <T> List<T> getList(String baseUrl, String secret, String audience, String scope, String uri) {
        return RestClient.builder().baseUrl(baseUrl).build()
                .get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, bearer(secret, audience, scope))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private String bearer(String secret, String audience, String scope) {
        return "Bearer " + serviceTokenService.generateToken(secret, audience, scope);
    }
}
