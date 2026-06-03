package com.jaycodesx.mortgage.infrastructure.metrics;

import com.jaycodesx.mortgage.infrastructure.admin.AuthMetricsResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthMetricsControllerTest {

    @Test
    void returnsUnauthorizedWithoutHeader() {
        AuthMetricsController controller = new AuthMetricsController(mock(AuthMetricsService.class));

        ResponseEntity<AuthMetricsResponseDto> response = controller.getMetrics(null);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void returnsSnapshotWithAuthorizationHeader() {
        AuthMetricsService service = mock(AuthMetricsService.class);
        when(service.getSnapshot()).thenReturn(new AuthMetricsResponseDto(2, 1, 1));

        ResponseEntity<AuthMetricsResponseDto> response = new AuthMetricsController(service).getMetrics("Bearer token");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(new AuthMetricsResponseDto(2, 1, 1));
    }
}
