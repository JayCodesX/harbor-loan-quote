package com.jaycodesx.mortgage.infrastructure.config;

import com.jaycodesx.mortgage.auth.model.AuthUser;
import com.jaycodesx.mortgage.auth.repository.AuthUserRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthDataSeederTest {

    @Test
    void seedsAdminAndDemoUserWhenMissing() throws Exception {
        AuthUserRepository repository = mock(AuthUserRepository.class);
        when(repository.existsByEmail("admin@jaycodesx.dev")).thenReturn(false);
        when(repository.existsByEmail("jay@jaycodesx.dev")).thenReturn(false);

        AuthDataSeeder seeder = new AuthDataSeeder(repository);
        seeder.run();

        verify(repository, times(2)).save(any(AuthUser.class));
    }

    @Test
    void skipsSeedWhenUsersAlreadyExist() throws Exception {
        AuthUserRepository repository = mock(AuthUserRepository.class);
        when(repository.existsByEmail("admin@jaycodesx.dev")).thenReturn(true);
        when(repository.existsByEmail("jay@jaycodesx.dev")).thenReturn(true);

        AuthDataSeeder seeder = new AuthDataSeeder(repository);
        seeder.run();

        verify(repository, times(0)).save(any(AuthUser.class));
    }
}
