package com.jaycodesx.mortgage.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceTokenServiceTest {

    // Dev RSA-2048 private key (PKCS8 DER, base64) — test-only, not a secret
    private static final String PRIVATE_KEY_B64 =
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQClJ4b8kkiPiv/l" +
            "d5mFq1zV10Gp52PmBuEiHhbO5nl65FsbKorRfaAc6EcGu8o5Yucn+wx9Fp64oi0I" +
            "XmPpMiM7FOQ5eBuKKbwSmlBVpzbkE0xqqsO8Ar/Sw+lemFtrxDZVbQAAKhPZewbv" +
            "QCAjNYUhZwAC+BAdpryTP0Fqd62jYE5I7Pq8HJHOUDGKQXNH0JM3TY7gl5o528M8" +
            "2PHItrGJEwP5hbUfEsQ8hHQG3d2XmQ3HJUy7fXGrsyZ1yqm7YFuspsMPMGQw9IGl" +
            "baX7dKyoXOTXPhzC45LRmnPIwOgDVxru3kXYHM6lxPatVmgBl4PJd5nRSZ4YCEPLN" +
            "0SqVPCFAgMBAAECggEAVBKlItoM2hW2YsfZfTRY0/r6UFWcgzKpVMbgcX7eBHNO9a" +
            "tYc4ByC4Uq8wCR11jDt5STLPJg5jiYmBQE5GdDjAX8kEzWKCVKumWX06KFzOEOG5b" +
            "GgWUc5VwZ5q/Eij2eMyqDv5Gr/SAlzZFlSscp+HEIjnpdKdsBUCv5TwuRdwL0HWQ8" +
            "HOQ3biAZaKaC7k6XWp/rssGIDu6erVPHqsJ0UEuBbLr0ti6IjAddPQrjtyruflzZMi" +
            "y6o9qGo1zZfBDBW/F2j8ORsm6G0S/Brp98rGa74hAqtmYOcnOHsTv6cWOQBXoAVNE" +
            "A4GTKWsWZaUzms+4wiXhDVVxUkY7wRYIfQQKBgQDU8vcmgf7nbu3u75p44D7Q9mPN" +
            "FY/w4Du2Q7jU4nRvu8F6ILl7nXkvyyyJB/8Pntb0EU8W/4g3HaFgAeCEqXeDixHqbr" +
            "WWqKZ9P50qEaLLYDHUk6EY2fdsT4wK4pixFboKXpkeX3g8QvONq6hZ7a38ppyWGPhsf" +
            "Rn6KgQlCgC+bQKBgQDGivsU2ABR6TeRplTv9mPvlCT2ZZASr954GB9ab5ycW5WyHXTY" +
            "Hz+A/nkqLN0cURL3H8AFlVx9uTNJ8e5fv8WJIsWtBGbXPzi4uY63YRoAAc7EA9zCiyHb" +
            "lL+106LSmHwasyc6uagyLnj90+5y4k8Xzde9+KzuSBtQ0BaEtlVLeQKBgQDDh5SINzTn" +
            "LGQs0LT16HHFTXe2B+ZzNnGdmfFNB+IB9lRg30twxddQmy35ZO4+WYrl8D6+z0Mz2HEk" +
            "xpRxO2oRztBUgfMasyCsQIz+49KnC1y15Bg3yWv2d9QO6qFpeJmqi9HsoZy1OISJwOSEL" +
            "kljg1ikOalhdD02Bv5nRBgKwQKBgEik02d1/O/7f/yHum4uXbRsJPYOhR+BP8n7MYWabt" +
            "f63pPUGUt2a3rcISMgVSqmM2U+NbkVREkv+ScmIQzhkvoxQdijUqmRFUTph+Fq4uN7xda" +
            "MfzqjQ5kfHOH/dITPjCEMyd6/zV3HCTf+UjGwwjW4eCanFUb+FRiFeqfGOwEpAoGAANNH" +
            "UkpbIoXwaS3PKtY7hX0Gtcrc9ZmM0LhZUBFE85yaQdvArbqmNJn2ej9yMeeb09IyDsTFU" +
            "PeDBSy/YOb6gJk/qnE0a1i8zGEUJboSExm2YAD73pa2gE8vWmczy4+7bDo1AfdWiEVDbe" +
            "Hv5JiEpDOtXT9SG28XItMBIudKTpg=";

    @Test
    void generatesPricingTokenWithExpectedClaims() {
        ServiceTokenProperties properties = new ServiceTokenProperties(
                PRIVATE_KEY_B64,
                "harbor-test-key-1",
                "mortgage-loan-api",
                "pricing-service",
                "pricing:write",
                300L
        );
        ServiceTokenService service = new ServiceTokenService(properties);

        String token = service.generatePricingToken();

        Claims claims = Jwts.parser()
                .verifyWith((RSAPublicKey) service.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getIssuer()).isEqualTo("mortgage-loan-api");
        assertThat(claims.getSubject()).isEqualTo("mortgage-loan-api");
        assertThat(claims.getAudience()).contains("pricing-service");
        assertThat(claims.get("scope", String.class)).isEqualTo("pricing:write");
        assertThat(claims.get("type", String.class)).isEqualTo("service");
    }
}
