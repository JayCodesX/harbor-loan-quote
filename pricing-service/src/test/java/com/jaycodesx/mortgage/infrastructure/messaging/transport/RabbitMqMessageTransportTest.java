package com.jaycodesx.mortgage.infrastructure.messaging.transport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMqMessageTransportTest {

    @Mock
    RabbitTemplate rabbitTemplate;

    @InjectMocks
    RabbitMqMessageTransport transport;

    @Test
    void publish_delegatesToRabbitTemplate() {
        TransportMessage message = TransportMessage.of(
                1,
                "RATE_SHEET_ACTIVATED",
                "rate-sheet.events",
                new Object()
        );

        transport.publish(message);

        verify(rabbitTemplate).convertAndSend(
                message.destination(),
                message.eventType(),
                message.payload()
        );
    }

    @Test
    void publish_usesDestinationAsExchange_andEventTypeAsRoutingKey() {
        Object payload = new Object();
        TransportMessage message = TransportMessage.of(1, "MY_EVENT", "my-exchange", payload);

        transport.publish(message);

        verify(rabbitTemplate).convertAndSend("my-exchange", "MY_EVENT", payload);
    }
}
