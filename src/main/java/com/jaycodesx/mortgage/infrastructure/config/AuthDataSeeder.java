package com.jaycodesx.mortgage.infrastructure.config;

import com.jaycodesx.mortgage.auth.model.AuthUser;
import com.jaycodesx.mortgage.auth.repository.AuthUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
public class AuthDataSeeder implements CommandLineRunner {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final AuthUserRepository authUserRepository;

    public AuthDataSeeder(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    @Override
    public void run(String... args) {
        seedUser("admin@jaycodesx.dev", "ADMIN");
        seedUser("jay@jaycodesx.dev", "USER");
    }

    private void seedUser(String email, String role) {
        if (authUserRepository.existsByEmail(email)) {
            return;
        }

        AuthUser user = new AuthUser();
        user.setEmail(email);
        user.setPasswordHash(PASSWORD_ENCODER.encode("StrongPass123!"));
        user.setRole(role);
        authUserRepository.save(user);
    }
}
