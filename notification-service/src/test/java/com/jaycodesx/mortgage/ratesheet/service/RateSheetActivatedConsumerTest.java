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
class RateSheetActivatedConsumerTest {

    @Mock
    RateSheetConnectionRegistry registry;

    @InjectMocks
    RateSheetActivatedConsumer consumer;

    @Test
    void onRateSheetActivated_broadcastsToRegistry() {
        RateSheetActivatedMessage message = new RateSheetActivatedMessage(
                42L, "FANNIE_MAE",
                LocalDateTime.of(2026, 4, 2, 9, 0),
                LocalDateTime.of(2026, 4, 2, 23, 59)
        );
        when(registry.broadcast(message)).thenReturn(3);

        consumer.onRateSheetActivated(message);

        verify(registry).broadcast(message);
    }
}
