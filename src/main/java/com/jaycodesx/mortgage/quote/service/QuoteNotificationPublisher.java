package com.jaycodesx.mortgage.quote.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaycodesx.mortgage.infrastructure.messaging.QuoteMessagingProperties;
import com.jaycodesx.mortgage.infrastructure.security.NotificationTokenProperties;
import com.jaycodesx.mortgage.infrastructure.security.ServiceTokenService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
@EnableConfigurationProperties(NotificationTokenProperties.class)
public class QuoteNotificationPublisher {

    private final ObjectProvider<SqsClient> sqsClientProvider;
    private final QuoteMessagingProperties properties;
    private final ObjectMapper objectMapper;
    private final ServiceTokenService serviceTokenService;
    private final NotificationTokenProperties notificationTokenProperties;

    public QuoteNotificationPublisher(
            ObjectProvider<SqsClient> sqsClientProvider,
            QuoteMessagingProperties properties,
            ObjectMapper objectMapper,
            ServiceTokenService serviceTokenService,
            NotificationTokenProperties notificationTokenProperties
    ) {
        this.sqsClientProvider = sqsClientProvider;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.serviceTokenService = serviceTokenService;
        this.notificationTokenProperties = notificationTokenProperties;
    }

    public void publish(QuoteNotificationMessage message) {
        if (!properties.enabled()) {
            return;
        }

        SqsClient sqsClient = sqsClientProvider.getIfAvailable();
        if (sqsClient == null) {
            return;
        }

        String queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(properties.notificationQueueName())
                .build()).queueUrl();
        try {
            QuoteNotificationMessage secured = message.withServiceToken(serviceTokenService.generateToken(
                    notificationTokenProperties.audience(),
                    notificationTokenProperties.scope()
            ));
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(objectMapper.writeValueAsString(secured))
                    .build());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize quote notification message", ex);
        }
    }
}
