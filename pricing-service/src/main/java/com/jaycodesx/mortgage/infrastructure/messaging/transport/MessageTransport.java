package com.jaycodesx.mortgage.infrastructure.messaging.transport;

/**
 * Broker-agnostic transport interface. Business publishers call publish() without
 * knowing which broker is active. The active implementation (NoOp, RabbitMQ, SQS)
 * is swapped via configuration — no business code changes at migration time.
 *
 * Phase 1: NoOpMessageTransport (no broker)
 * Phase 2: RabbitMqMessageTransport (VPS Docker Compose)
 * Phase 3: SqsMessageTransport (AWS)
 */
public interface MessageTransport {
    void publish(TransportMessage message);
}
