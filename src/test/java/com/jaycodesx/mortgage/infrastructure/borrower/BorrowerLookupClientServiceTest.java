package com.jaycodesx.mortgage.infrastructure.borrower;

import com.jaycodesx.mortgage.infrastructure.security.ServiceTokenService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BorrowerLookupClientServiceTest {

    @Test
    void borrowerExistsReturnsFalseWhenServiceIsUnreachable() {
        ServiceTokenService serviceTokenService = mock(ServiceTokenService.class);
        when(serviceTokenService.generateToken(anyString(), anyString())).thenReturn("service-token");

        BorrowerLookupClientService service = new BorrowerLookupClientService(
                new BorrowerServiceClientProperties(
                        "http://localhost:0",
                        "borrower-service",
                        "borrower:read"
                ),
                serviceTokenService
        );

        // Any connection error is caught internally and returns false
        assertThat(service.borrowerExists(42L)).isFalse();
    }
}
