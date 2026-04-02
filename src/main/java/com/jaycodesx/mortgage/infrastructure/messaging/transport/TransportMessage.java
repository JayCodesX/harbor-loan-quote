package com.jaycodesx.mortgage.infrastructure.messaging.transport;

import java.util.UUID;

/**
 * Broker-agnostic message envelope. All async events in the system are published
 * as TransportMessages through the MessageTransport interface. No business code
 * references a specific broker SDK directly.
 */
public record TransportMessage(
        String messageId,
        String correlationId,
        int schemaVersion,
        String eventType,
        String destination,
        Object payload
) {
    public static TransportMessage of(int schemaVersion, String eventType, String destination, Object payload) {
        return new TransportMessage(
                UUID.randomUUID().toString(),
                null,
                schemaVersion,
                eventType,
                destination,
                payload
        );
    }

    public TransportMessage withCorrelationId(String correlationId) {
        return new TransportMessage(messageId, correlationId, schemaVersion, eventType, destination, payload);
    }
}
