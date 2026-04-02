package com.jaycodesx.mortgage.infrastructure.borrower;

import com.jaycodesx.mortgage.infrastructure.security.ServiceTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@EnableConfigurationProperties(BorrowerServiceClientProperties.class)
public class BorrowerLookupClientService {

    private final RestClient restClient;
    private final BorrowerServiceClientProperties properties;
    private final ServiceTokenService serviceTokenService;

    public BorrowerLookupClientService(
            BorrowerServiceClientProperties properties,
            ServiceTokenService serviceTokenService
    ) {
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.properties = properties;
        this.serviceTokenService = serviceTokenService;
    }

    public boolean borrowerExists(Long borrowerId) {
        try {
            BorrowerExistsResponseDto response = restClient.get()
                    .uri("/borrowers/internal/{id}/exists", borrowerId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceTokenService.generateToken(
                            properties.secret(),
                            properties.audience(),
                            properties.scope()
                    ))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
                    .body(BorrowerExistsResponseDto.class);
            return response != null && response.exists();
        } catch (Exception ex) {
            return false;
        }
    }
}
