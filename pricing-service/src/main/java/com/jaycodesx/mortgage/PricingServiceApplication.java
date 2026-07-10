package com.jaycodesx.mortgage;

import com.jaycodesx.mortgage.infrastructure.messaging.kafka.RateChangeStreamProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
// RateChangeStreamProperties must be registered unconditionally: the always-on
// RateChangeEventPublisher depends on it and self-guards on its `enabled` flag,
// so the service must start even when app.kafka.enabled=false (the default).
@EnableConfigurationProperties(RateChangeStreamProperties.class)
public class PricingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PricingServiceApplication.class, args);
    }
}
