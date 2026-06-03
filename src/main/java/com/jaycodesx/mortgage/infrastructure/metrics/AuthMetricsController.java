package com.jaycodesx.mortgage.infrastructure.metrics;

import com.jaycodesx.mortgage.infrastructure.admin.AuthMetricsResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/admin/metrics")
public class AuthMetricsController {

    private final AuthMetricsService authMetricsService;

    public AuthMetricsController(AuthMetricsService authMetricsService) {
        this.authMetricsService = authMetricsService;
    }

    @GetMapping
    public ResponseEntity<AuthMetricsResponseDto> getMetrics(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authMetricsService.getSnapshot());
    }
}
