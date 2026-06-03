package com.jaycodesx.mortgage.infrastructure.metrics;

import com.jaycodesx.mortgage.auth.repository.AuthUserRepository;
import com.jaycodesx.mortgage.auth.repository.AuthUserRoleCount;
import com.jaycodesx.mortgage.infrastructure.admin.AuthMetricsResponseDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthMetricsServiceTest {

    @Test
    void returnsAuthUserCounts() {
        AuthUserRepository repository = mock(AuthUserRepository.class);
        when(repository.count()).thenReturn(3L);
        when(repository.countByRole()).thenReturn(List.of(
                count("ADMIN", 1),
                count("USER", 2)
        ));

        AuthMetricsResponseDto snapshot = new AuthMetricsService(repository).getSnapshot();

        assertThat(snapshot.totalUsers()).isEqualTo(3);
        assertThat(snapshot.adminUsers()).isEqualTo(1);
        assertThat(snapshot.standardUsers()).isEqualTo(2);
    }

    private AuthUserRoleCount count(String role, long total) {
        return new AuthUserRoleCount() {
            @Override
            public String getRole() {
                return role;
            }

            @Override
            public long getCount() {
                return total;
            }
        };
    }
}
