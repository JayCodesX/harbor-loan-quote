package com.jaycodesx.mortgage.pricing.messaging;

import com.jaycodesx.mortgage.infrastructure.messaging.RabbitMqMessagingConfig;
import com.jaycodesx.mortgage.infrastructure.messaging.transport.MessageTransport;
import com.jaycodesx.mortgage.infrastructure.messaging.transport.TransportMessage;
import com.jaycodesx.mortgage.pricing.model.RateSheetPublication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Publishes a {@link RateSheetActivatedMessage} whenever a rate sheet is activated.
 *
 * <p>Called by {@link com.jaycodesx.mortgage.pricing.service.RateSheetService} immediately
 * after a new {@link RateSheetPublication} is saved and the cache is evicted. Downstream
 * consumers (notification-service) use this event to notify borrowers per ADR-0048.
 *
 * <p>No business code references a specific broker. The active {@link MessageTransport}
 * implementation is selected at startup via {@code app.transport.type} configuration.
 */
@Service
public class RateSheetActivatedPublisher {

    private static final Logger log = LoggerFactory.getLogger(RateSheetActivatedPublisher.class);
    private static final String EVENT_TYPE = "RATE_SHEET_ACTIVATED";
    private static final int SCHEMA_VERSION = 1;

    private final MessageTransport transport;

    public RateSheetActivatedPublisher(MessageTransport transport) {
        this.transport = transport;
    }

    public void publish(RateSheetPublication publication) {
        RateSheetActivatedMessage payload = new RateSheetActivatedMessage(
                publication.getId(),
                publication.getInvestorId(),
                publication.getEffectiveAt(),
                publication.getExpiresAt()
        );
        TransportMessage message = TransportMessage.of(
                SCHEMA_VERSION,
                EVENT_TYPE,
                RabbitMqMessagingConfig.RATE_SHEET_EXCHANGE,
                payload
        );
        log.info("Publishing RateSheetActivated [rateSheetId={}, investorId={}]",
                publication.getId(), publication.getInvestorId());
        transport.publish(message);
    }
}
