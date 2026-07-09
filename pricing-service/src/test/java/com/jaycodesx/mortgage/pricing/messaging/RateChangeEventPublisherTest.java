package com.jaycodesx.mortgage.pricing.messaging;

import com.jaycodesx.mortgage.infrastructure.messaging.kafka.RateChangeStreamProperties;
import com.jaycodesx.mortgage.pricing.model.RateSheetPublication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateChangeEventPublisherTest {

    private static final String TOPIC = "pricing.rate-sheet.activated";
    private static final LocalDateTime EFFECTIVE_AT = LocalDateTime.of(2026, 7, 9, 0, 0);
    private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 12, 31, 0, 0);

    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    private RateChangeEventPublisher publisherWith(boolean enabled) {
        return new RateChangeEventPublisher(kafkaTemplate,
                new RateChangeStreamProperties(enabled, TOPIC));
    }

    @Test
    void publish_whenStreamDisabled_doesNotTouchKafka() {
        publisherWith(false).publish(
                new RateSheetPublication("FANNIE_MAE", EFFECTIVE_AT, EXPIRES_AT, "s"));

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publish_whenEnabled_sendsKeyedByInvestorWithMappedPayload() {
        // Return an incomplete future: the fire-and-forget callback registers but
        // never fires, so the test needs no SendResult/RecordMetadata (the latter is
        // final and awkward to stub). We assert on the send call itself.
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(new CompletableFuture<SendResult<String, Object>>());

        publisherWith(true).publish(
                new RateSheetPublication("FREDDIE_MAC", EFFECTIVE_AT, EXPIRES_AT, "s"));

        // Keyed by investorId (per-investor ordering) with the mapped event payload.
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("FREDDIE_MAC"), payloadCaptor.capture());

        RateSheetActivatedMessage payload = (RateSheetActivatedMessage) payloadCaptor.getValue();
        assertThat(payload.investorId()).isEqualTo("FREDDIE_MAC");
        assertThat(payload.effectiveAt()).isEqualTo(EFFECTIVE_AT);
        assertThat(payload.expiresAt()).isEqualTo(EXPIRES_AT);
    }
}
