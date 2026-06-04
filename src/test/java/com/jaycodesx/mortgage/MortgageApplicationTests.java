package com.jaycodesx.mortgage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class MortgageApplicationTests {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    // Provide ephemeral secrets so no key material is committed to the test resources.
    @DynamicPropertySource
    static void secrets(DynamicPropertyRegistry registry) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            String privateKey = Base64.getEncoder()
                    .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
            registry.add("app.service-token.private-key", () -> privateKey);
            registry.add("app.user-token.secret",
                    () -> "test-" + UUID.randomUUID() + UUID.randomUUID());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate ephemeral test secrets", e);
        }
    }

    @Test
    void contextLoads() {
    }

    @Test
    void mainMethodIsPublicStaticAndAvailable() throws Exception {
        Method mainMethod = MortgageApplication.class.getDeclaredMethod("main", String[].class);

        assertThat(Modifier.isPublic(mainMethod.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(mainMethod.getModifiers())).isTrue();
    }
}
