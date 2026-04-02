package com.jaycodesx.mortgage.infrastructure.messaging.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Phase 1 transport implementation. Messages are logged and dropped — no broker required.
 * Active by default (matchIfMissing = true) until a real broker adapter is wired in Phase 2.
 */
@Service
@ConditionalOnProperty(name = "app.transport.type", havingValue = "noop", matchIfMissing = true)
@EnableConfigurationProperties(TransportProperties.class)
public class NoOpMessageTransport implements MessageTransport {

    private static final Logger log = LoggerFactory.getLogger(NoOpMessageTransport.class);

    @Override
    public void publish(TransportMessage message) {
        log.debug("NoOpMessageTransport: message dropped [eventType={}, destination={}, messageId={}]",
                message.eventType(), message.destination(), message.messageId());
    }
}
