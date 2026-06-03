package com.jaycodesx.mortgage.quote.service;

import com.jaycodesx.mortgage.infrastructure.messaging.RabbitMqMessagingConfig;
import com.jaycodesx.mortgage.infrastructure.messaging.transport.MessageTransport;
import com.jaycodesx.mortgage.infrastructure.messaging.transport.TransportMessage;
import com.jaycodesx.mortgage.infrastructure.security.NotificationTokenProperties;
import com.jaycodesx.mortgage.infrastructure.security.ServiceTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(NotificationTokenProperties.class)
public class QuoteNotificationPublisher {

    private static final String EVENT_TYPE = "QUOTE_NOTIFICATION_SNAPSHOT";

    private final MessageTransport transport;
    private final ServiceTokenService serviceTokenService;
    private final NotificationTokenProperties notificationTokenProperties;

    public QuoteNotificationPublisher(
            MessageTransport transport,
            ServiceTokenService serviceTokenService,
            NotificationTokenProperties notificationTokenProperties
    ) {
        this.transport = transport;
        this.serviceTokenService = serviceTokenService;
        this.notificationTokenProperties = notificationTokenProperties;
    }

    public void publish(QuoteNotificationMessage message) {
        QuoteNotificationMessage secured = message.withServiceToken(serviceTokenService.generateToken(
                notificationTokenProperties.audience(),
                notificationTokenProperties.scope()
        ));

        TransportMessage transportMessage = TransportMessage.of(
                QuoteNotificationMessage.SCHEMA_VERSION,
                EVENT_TYPE,
                RabbitMqMessagingConfig.QUOTE_NOTIFICATION_EXCHANGE,
                secured
        );

        transport.publish(transportMessage);
    }
}
