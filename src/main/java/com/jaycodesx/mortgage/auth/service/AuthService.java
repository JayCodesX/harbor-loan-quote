package com.jaycodesx.mortgage.auth.service;

import com.jaycodesx.mortgage.auth.dto.AuthLoginRequestDto;
import com.jaycodesx.mortgage.auth.dto.AuthRegisterRequestDto;
import com.jaycodesx.mortgage.auth.dto.AuthResponseDto;
import com.jaycodesx.mortgage.auth.model.AuthUser;
import com.jaycodesx.mortgage.auth.repository.AuthUserRepository;
import com.jaycodesx.mortgage.infrastructure.security.UserTokenProperties;
import com.jaycodesx.mortgage.infrastructure.security.UserTokenService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final UserTokenService userTokenService;
    private final UserTokenProperties userTokenProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            AuthUserRepository authUserRepository,
            UserTokenService userTokenService,
            UserTokenProperties userTokenProperties
    ) {
        this.authUserRepository = authUserRepository;
        this.userTokenService = userTokenService;
        this.userTokenProperties = userTokenProperties;
    }

    public AuthResponseDto register(AuthRegisterRequestDto request) {
        if (authUserRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }

        AuthUser user = new AuthUser();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        AuthUser savedUser = authUserRepository.save(user);
        return toResponse(savedUser);
    }

    public AuthResponseDto login(AuthLoginRequestDto request) {
        AuthUser user = authUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return toResponse(user);
    }

    private AuthResponseDto toResponse(AuthUser user) {
        return new AuthResponseDto(
                userTokenService.generateToken(user),
                "Bearer",
                userTokenProperties.ttlSeconds(),
                user.getEmail(),
                user.getRole()
        );
    }
}
