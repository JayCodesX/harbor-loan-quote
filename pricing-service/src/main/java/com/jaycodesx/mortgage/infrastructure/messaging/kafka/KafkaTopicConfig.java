package com.jaycodesx.mortgage.infrastructure.messaging.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the rate-change topic on Redpanda (ADR-0052).
 *
 * <p>Only active when {@code app.kafka.enabled=true}. Spring Kafka's
 * {@code KafkaAdmin} sees this {@link NewTopic} bean and creates the topic on
 * startup if it does not exist, so there is no manual topic-provisioning step in
 * local dev.
 *
 * <p>One partition is deliberate for a single-node dev deployment. Ordering of
 * rate-sheet activations per investor matters (a later activation supersedes an
 * earlier one), and messages are keyed by investorId — with more partitions we
 * would rely on key-based partitioning to preserve per-investor order; with one
 * partition it is total. Replication factor 1 matches the single Redpanda node.
 */
@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaTopicConfig {

    @Bean
    public NewTopic rateSheetActivatedTopic(RateChangeStreamProperties properties) {
        return TopicBuilder.name(properties.topic())
                .partitions(1)
                .replicas(1)
                .build();
    }
}
