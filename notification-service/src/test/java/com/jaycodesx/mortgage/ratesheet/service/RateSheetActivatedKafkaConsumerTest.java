package com.jaycodesx.mortgage.ratesheet.service;

import com.jaycodesx.mortgage.ratesheet.messaging.RateSheetActivatedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateSheetActivatedKafkaConsumerTest {

    @Mock
    RateSheetConnectionRegistry registry;

    @InjectMocks
    RateSheetActivatedKafkaConsumer consumer;

    @Test
    void onRateSheetActivated_broadcastsEventToConnectedSessions() {
        RateSheetActivatedMessage message = new RateSheetActivatedMessage(
                5L, "FANNIE_MAE",
                LocalDateTime.of(2026, 7, 9, 0, 0),
                LocalDateTime.of(2026, 12, 31, 0, 0));
        when(registry.broadcast(message)).thenReturn(3);

        consumer.onRateSheetActivated(message);

        verify(registry).broadcast(message);
    }
}
