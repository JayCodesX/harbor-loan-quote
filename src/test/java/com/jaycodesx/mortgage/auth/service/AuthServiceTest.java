package com.jaycodesx.mortgage.auth.service;

import com.jaycodesx.mortgage.auth.dto.AuthLoginRequestDto;
import com.jaycodesx.mortgage.auth.dto.AuthRegisterRequestDto;
import com.jaycodesx.mortgage.auth.model.AuthUser;
import com.jaycodesx.mortgage.auth.repository.AuthUserRepository;
import com.jaycodesx.mortgage.infrastructure.security.UserTokenProperties;
import com.jaycodesx.mortgage.infrastructure.security.UserTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Mock
    private AuthUserRepository authUserRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        UserTokenProperties properties = new UserTokenProperties(
                "internal",
                "test-user-token-secret-12345678901234567890",
                "auth-service",
                "mortgage-loan-api",
                null,
                900
        );
        authService = new AuthService(authUserRepository, new UserTokenService(properties), properties);
    }

    @Test
    void registersNewUser() {
        when(authUserRepository.existsByEmail("jay@jaycodesx.dev")).thenReturn(false);
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(new AuthRegisterRequestDto("jay@jaycodesx.dev", "StrongPass123"));

        assertThat(response.email()).isEqualTo("jay@jaycodesx.dev");
        assertThat(response.accessToken()).isNotBlank();
    }

    @Test
    void rejectsDuplicateEmail() {
        when(authUserRepository.existsByEmail("jay@jaycodesx.dev")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new AuthRegisterRequestDto("jay@jaycodesx.dev", "StrongPass123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already in use");
    }

    @Test
    void logsInKnownUser() {
        AuthUser user = new AuthUser();
        user.setEmail("jay@jaycodesx.dev");
        user.setRole("USER");
        user.setPasswordHash(PASSWORD_ENCODER.encode("StrongPass123"));
        when(authUserRepository.findByEmail("jay@jaycodesx.dev")).thenReturn(Optional.of(user));

        var response = authService.login(new AuthLoginRequestDto("jay@jaycodesx.dev", "StrongPass123"));

        assertThat(response.email()).isEqualTo("jay@jaycodesx.dev");
        assertThat(response.accessToken()).isNotBlank();
    }
}
