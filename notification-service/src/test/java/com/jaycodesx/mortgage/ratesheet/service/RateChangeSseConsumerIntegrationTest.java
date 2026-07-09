package com.jaycodesx.mortgage.ratesheet.service;

import com.jaycodesx.mortgage.infrastructure.security.ServiceTokenValidator;
import com.jaycodesx.mortgage.ratesheet.messaging.RateSheetActivatedMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.redpanda.RedpandaContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Cross-service integration test for the SSE rate-change consumer (ADR-0052).
 *
 * <p>The specific go-live risk this covers: pricing-service tags every message with
 * its OWN class in the {@code __TypeId__} header
 * ({@code com.jaycodesx.mortgage.pricing.messaging.RateSheetActivatedMessage}), a
 * class notification-service does not have. The consumer's {@code JsonDeserializer}
 * is configured with {@code use.type.headers=false} + a local default type so it
 * ignores that header and deserializes into notification-service's own record.
 * A unit test cannot exercise that config; this test produces a message carrying
 * the foreign type header to a real broker and asserts the consumer deserializes
 * and broadcasts it correctly.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "app.kafka.enabled=true",
                "app.rabbitmq.consumer.enabled=false",
                "app.messaging.enabled=false",
                "app.messaging.consumer-enabled=false"
        })
@Testcontainers
class RateChangeSseConsumerIntegrationTest {

    private static final String TOPIC = "pricing.rate-sheet.activated";
    private static final String PRICING_TYPE_ID =
            "com.jaycodesx.mortgage.pricing.messaging.RateSheetActivatedMessage";

    @Container
    @ServiceConnection
    static RedpandaContainer redpanda = new RedpandaContainer("redpandadata/redpanda:v24.2.7");

    // Capture what the consumer broadcasts; also avoids needing an RSA key at startup.
    @MockBean
    RateSheetConnectionRegistry registry;
    @MockBean
    ServiceTokenValidator serviceTokenValidator;

    @Autowired
    KafkaListenerEndpointRegistry endpointRegistry;

    @Test
    void consumesMessageTaggedWithPricingTypeHeader_deserializesLocally_andBroadcasts() throws Exception {
        // The consumer reads from 'latest', so wait until it is assigned before
        // producing, or the message would land before its start offset.
        for (MessageListenerContainer container : endpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }

        String json = "{\"rateSheetId\":7,\"investorId\":\"FANNIE_MAE\","
                + "\"effectiveAt\":[2026,7,9,0,0],\"expiresAt\":[2026,12,31,0,0]}";

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, redpanda.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()))) {

            ProducerRecord<String, String> record =
                    new ProducerRecord<>(TOPIC, "FANNIE_MAE", json);
            // The foreign type header the consumer must IGNORE.
            record.headers().add("__TypeId__", PRICING_TYPE_ID.getBytes(StandardCharsets.UTF_8));
            producer.send(record).get();
        }

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            ArgumentCaptor<RateSheetActivatedMessage> captor =
                    ArgumentCaptor.forClass(RateSheetActivatedMessage.class);
            verify(registry, atLeastOnce()).broadcast(captor.capture());
            RateSheetActivatedMessage received = captor.getValue();
            assertThat(received.rateSheetId()).isEqualTo(7L);
            assertThat(received.investorId()).isEqualTo("FANNIE_MAE");
        });
    }
}
