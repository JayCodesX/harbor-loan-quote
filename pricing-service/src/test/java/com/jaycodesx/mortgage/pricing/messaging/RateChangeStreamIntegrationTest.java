package com.jaycodesx.mortgage.pricing.messaging;

import com.jaycodesx.mortgage.infrastructure.security.OutboundServiceTokenService;
import com.jaycodesx.mortgage.infrastructure.security.ServiceTokenValidator;
import com.jaycodesx.mortgage.pricing.model.RateChangeAudit;
import com.jaycodesx.mortgage.pricing.model.RateSheetPublication;
import com.jaycodesx.mortgage.pricing.repository.RateChangeAuditRepository;
import com.jaycodesx.mortgage.pricing.service.PricingCacheService;
import com.jaycodesx.mortgage.pricing.service.RateSheetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.redpanda.RedpandaContainer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end integration test for the rate-change event stream (ADR-0052),
 * exercising the system as a whole against a real Redpanda broker and a real
 * MySQL database (both via Testcontainers) — not mocks.
 *
 * <p>This is the coverage the Mockito unit tests cannot give: real JSON
 * serialization across the wire (the {@code JsonSerializer} → {@code __TypeId__}
 * header → {@code JsonDeserializer} round trip), real topic publish/consume, and
 * idempotency enforced by the actual MySQL unique constraint rather than a stubbed
 * {@code existsByRateSheetId}. These are exactly the failure modes that "work on
 * mocks, break in prod."
 *
 * <p>Scope is kept to two scenarios. Beans outside the Kafka chain
 * (service-token security, Redis-backed cache) are mocked so the context needs no
 * RSA key, HMAC secret, or Redis container — the test stays focused on the stream.
 */
@SpringBootTest(properties = {
        "app.kafka.enabled=true",
        "app.transport.type=noop",
        "app.rabbitmq.consumer.enabled=false"
})
@Testcontainers
class RateChangeStreamIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Container
    @ServiceConnection
    static RedpandaContainer redpanda = new RedpandaContainer("redpandadata/redpanda:v24.2.7");

    // Outside the system under test — mocked so no RSA key / HMAC secret / Redis is needed.
    @MockitoBean
    ServiceTokenValidator serviceTokenValidator;
    @MockitoBean
    OutboundServiceTokenService outboundServiceTokenService;
    @MockitoBean
    PricingCacheService pricingCacheService;

    @Autowired
    RateSheetService rateSheetService;
    @Autowired
    RateChangeAuditRepository auditRepository;
    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    private static final LocalDateTime EFFECTIVE_AT = LocalDateTime.of(2026, 7, 9, 0, 0);
    private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 12, 31, 0, 0);
    private static final String TOPIC = "pricing.rate-sheet.activated";

    @BeforeEach
    void clearAudit() {
        auditRepository.deleteAll();
    }

    @Test
    void publish_flowsThroughKafkaToAuditTable() {
        // Drive the real activation path: RateSheetService -> RateChangeEventPublisher
        // -> KafkaTemplate -> topic -> RateChangeAuditListener -> MySQL.
        RateSheetPublication saved = rateSheetService.publish(
                "FANNIE_MAE", EFFECTIVE_AT, EXPIRES_AT, "it-happy-path", List.of());

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            List<RateChangeAudit> rows = auditRepository.findAll();
            assertThat(rows).hasSize(1);
            RateChangeAudit row = rows.get(0);
            assertThat(row.getRateSheetId()).isEqualTo(saved.getId());
            assertThat(row.getInvestorId()).isEqualTo("FANNIE_MAE");
            assertThat(row.getEffectiveAt()).isEqualTo(EFFECTIVE_AT);
            // Coordinates came back from the real broker, proving a genuine round trip.
            assertThat(row.getKafkaPartition()).isNotNull();
            assertThat(row.getKafkaOffset()).isNotNull();
        });
    }

    @Test
    void duplicateDelivery_isIdempotent_againstRealUniqueConstraint() {
        long rateSheetId = 999_001L;
        RateSheetActivatedMessage event = new RateSheetActivatedMessage(
                rateSheetId, "FREDDIE_MAC", EFFECTIVE_AT, EXPIRES_AT);

        // Deliver the SAME event twice, as an at-least-once broker would on redelivery.
        kafkaTemplate.send(TOPIC, event.investorId(), event);
        kafkaTemplate.send(TOPIC, event.investorId(), event);

        // pollDelay lets both records be consumed before we assert; exactly one row
        // must survive — the real unique key + the idempotency guard held the line.
        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            long count = auditRepository.findAll().stream()
                    .filter(a -> a.getRateSheetId().equals(rateSheetId))
                    .count();
            assertThat(count).isEqualTo(1L);
        });
    }
}
