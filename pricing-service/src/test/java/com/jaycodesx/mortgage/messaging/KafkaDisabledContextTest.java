package com.jaycodesx.mortgage.messaging;

import com.jaycodesx.mortgage.infrastructure.messaging.kafka.RateChangeStreamProperties;
import com.jaycodesx.mortgage.infrastructure.security.OutboundServiceTokenService;
import com.jaycodesx.mortgage.infrastructure.security.ServiceTokenValidator;
import com.jaycodesx.mortgage.pricing.messaging.RateChangeEventPublisher;
import com.jaycodesx.mortgage.pricing.service.PricingCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for the bug where the service failed to start with
 * {@code app.kafka.enabled=false} (the default).
 *
 * <p>{@link RateChangeEventPublisher} is an unconditional bean that self-guards on
 * {@code app.kafka.enabled}, so it must always be constructable — which means
 * {@link RateChangeStreamProperties} must be registered unconditionally, not only
 * when the Kafka topic config loads. This test boots the whole context with Kafka
 * OFF and no broker, and asserts it comes up with the beans wired.
 *
 * <p>No Redpanda container is started — that is the point: the service must run
 * without the event stream.
 */
@SpringBootTest(properties = {
        "app.kafka.enabled=false",
        "app.transport.type=noop",
        "app.rabbitmq.consumer.enabled=false"
})
@Testcontainers
class KafkaDisabledContextTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @MockitoBean
    ServiceTokenValidator serviceTokenValidator;
    @MockitoBean
    OutboundServiceTokenService outboundServiceTokenService;
    @MockitoBean
    PricingCacheService pricingCacheService;

    @Autowired
    ApplicationContext context;

    @Test
    void contextStartsAndRateChangeBeansAreWiredWithKafkaDisabled() {
        // The properties bean must exist even though the (conditional) Kafka topic
        // config did not load — this is the exact wiring that was broken.
        assertThat(context.getBean(RateChangeStreamProperties.class)).isNotNull();
        assertThat(context.getBean(RateChangeStreamProperties.class).enabled()).isFalse();
        // And the publisher that depends on it is constructable.
        assertThat(context.getBean(RateChangeEventPublisher.class)).isNotNull();
    }
}
