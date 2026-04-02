package com.jaycodesx.mortgage.pricing.messaging;

import com.jaycodesx.mortgage.infrastructure.messaging.RabbitMqMessagingConfig;
import com.jaycodesx.mortgage.infrastructure.messaging.transport.MessageTransport;
import com.jaycodesx.mortgage.infrastructure.messaging.transport.TransportMessage;
import com.jaycodesx.mortgage.pricing.model.RateSheetPublication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateSheetActivatedPublisherTest {

    @Mock
    MessageTransport transport;

    @InjectMocks
    RateSheetActivatedPublisher publisher;

    private static final LocalDateTime EFFECTIVE_AT = LocalDateTime.of(2026, 4, 2, 9, 0);
    private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 4, 2, 23, 59);

    @Test
    void publish_delegatesToTransportWithCorrectEventType() {
        RateSheetPublication publication = new RateSheetPublication(
                "FANNIE_MAE", EFFECTIVE_AT, EXPIRES_AT, "fannie-mae-morning");

        publisher.publish(publication);

        ArgumentCaptor<TransportMessage> captor = ArgumentCaptor.forClass(TransportMessage.class);
        verify(transport).publish(captor.capture());

        TransportMessage sent = captor.getValue();
        assertThat(sent.eventType()).isEqualTo("RATE_SHEET_ACTIVATED");
        assertThat(sent.destination()).isEqualTo(RabbitMqMessagingConfig.RATE_SHEET_EXCHANGE);
        assertThat(sent.schemaVersion()).isEqualTo(1);
        assertThat(sent.messageId()).isNotNull();
    }

    @Test
    void publish_payloadContainsInvestorIdAndDates() {
        RateSheetPublication publication = new RateSheetPublication(
                "FREDDIE_MAC", EFFECTIVE_AT, EXPIRES_AT, "freddie-mac-afternoon");

        publisher.publish(publication);

        ArgumentCaptor<TransportMessage> captor = ArgumentCaptor.forClass(TransportMessage.class);
        verify(transport).publish(captor.capture());

        RateSheetActivatedMessage payload = (RateSheetActivatedMessage) captor.getValue().payload();
        assertThat(payload.investorId()).isEqualTo("FREDDIE_MAC");
        assertThat(payload.effectiveAt()).isEqualTo(EFFECTIVE_AT);
        assertThat(payload.expiresAt()).isEqualTo(EXPIRES_AT);
    }
}
