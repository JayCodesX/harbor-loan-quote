package com.jaycodesx.mortgage.ratesheet.controller;

import com.jaycodesx.mortgage.ratesheet.messaging.RateSheetActivatedMessage;
import com.jaycodesx.mortgage.ratesheet.service.RateSheetConnectionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateAlertControllerTest {

    @Mock
    RateSheetConnectionRegistry registry;

    @InjectMocks
    RateAlertController controller;

    @Test
    void stream_registersSessionAndReturnsFlux() {
        Sinks.Many<ServerSentEvent<RateSheetActivatedMessage>> sink =
                Sinks.many().unicast().onBackpressureBuffer();
        when(registry.register("session-abc")).thenReturn(sink.asFlux());

        Flux<ServerSentEvent<RateSheetActivatedMessage>> stream = controller.stream("session-abc");

        RateSheetActivatedMessage message = new RateSheetActivatedMessage(
                5L, "FANNIE_MAE",
                LocalDateTime.of(2026, 4, 2, 9, 0),
                LocalDateTime.of(2026, 4, 2, 23, 59)
        );
        ServerSentEvent<RateSheetActivatedMessage> event = ServerSentEvent
                .<RateSheetActivatedMessage>builder()
                .event("rate-alert")
                .id("5")
                .data(message)
                .build();

        StepVerifier.create(stream.take(1))
                .then(() -> sink.tryEmitNext(event))
                .expectNextMatches(e -> "rate-alert".equals(e.event()) && e.data() != null)
                .verifyComplete();

        verify(registry).register("session-abc");
        verify(registry).deregister("session-abc");
    }
}
