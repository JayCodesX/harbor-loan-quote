package com.jaycodesx.mortgage.infrastructure.metrics;

import com.jaycodesx.mortgage.auth.repository.AuthUserRepository;
import com.jaycodesx.mortgage.infrastructure.admin.AuthMetricsResponseDto;
import org.springframework.stereotype.Service;

@Service
public class AuthMetricsService {

    private final AuthUserRepository authUserRepository;

    public AuthMetricsService(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    public AuthMetricsResponseDto getSnapshot() {
        long totalUsers = authUserRepository.count();
        long adminUsers = authUserRepository.countByRole().stream()
                .filter(count -> "ADMIN".equalsIgnoreCase(count.getRole()))
                .mapToLong(count -> count.getCount())
                .sum();
        return new AuthMetricsResponseDto(totalUsers, adminUsers, Math.max(0, totalUsers - adminUsers));
    }
}
