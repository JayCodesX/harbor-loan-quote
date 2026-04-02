package com.jaycodesx.mortgage.ratesheet.service;

import com.jaycodesx.mortgage.ratesheet.messaging.RateSheetActivatedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateSheetConnectionRegistryTest {

    private RateSheetConnectionRegistry registry;

    private static final LocalDateTime EFFECTIVE = LocalDateTime.of(2026, 4, 2, 9, 0);
    private static final LocalDateTime EXPIRES = LocalDateTime.of(2026, 4, 2, 23, 59);

    @BeforeEach
    void setUp() {
        registry = new RateSheetConnectionRegistry();
    }

    @Test
    void register_returnsFluxThatReceivesBroadcastEvents() {
        Flux<ServerSentEvent<RateSheetActivatedMessage>> stream = registry.register("session-1");

        RateSheetActivatedMessage message = new RateSheetActivatedMessage(42L, "FANNIE_MAE", EFFECTIVE, EXPIRES);

        List<ServerSentEvent<RateSheetActivatedMessage>> received = new ArrayList<>();
        stream.subscribe(received::add);

        registry.broadcast(message);

        assertThat(received).hasSize(1);
        assertThat(received.get(0).event()).isEqualTo("rate-alert");
        assertThat(received.get(0).id()).isEqualTo("42");
        assertThat(received.get(0).data()).isEqualTo(message);
    }

    @Test
    void broadcast_deliversToAllActiveSessions() {
        List<ServerSentEvent<RateSheetActivatedMessage>> received1 = new ArrayList<>();
        List<ServerSentEvent<RateSheetActivatedMessage>> received2 = new ArrayList<>();

        registry.register("session-a").subscribe(received1::add);
        registry.register("session-b").subscribe(received2::add);

        RateSheetActivatedMessage message = new RateSheetActivatedMessage(99L, "FREDDIE_MAC", EFFECTIVE, EXPIRES);
        int delivered = registry.broadcast(message);

        assertThat(delivered).isEqualTo(2);
        assertThat(received1).hasSize(1);
        assertThat(received2).hasSize(1);
    }

    @Test
    void deregister_removesSessionFromRegistry() {
        registry.register("session-x").subscribe();
        assertThat(registry.activeConnectionCount()).isEqualTo(1);

        registry.deregister("session-x");

        assertThat(registry.activeConnectionCount()).isZero();
    }

    @Test
    void broadcast_returnsZero_whenNoConnectionsActive() {
        RateSheetActivatedMessage message = new RateSheetActivatedMessage(1L, "FANNIE_MAE", EFFECTIVE, EXPIRES);
        int delivered = registry.broadcast(message);
        assertThat(delivered).isZero();
    }

    @Test
    void register_replacesExistingConnection_forSameSession() {
        registry.register("session-1").subscribe();
        assertThat(registry.activeConnectionCount()).isEqualTo(1);

        registry.register("session-1").subscribe();
        assertThat(registry.activeConnectionCount()).isEqualTo(1);
    }

    @Test
    void stream_completesCleanly_afterDeregister() {
        Flux<ServerSentEvent<RateSheetActivatedMessage>> stream = registry.register("session-sse");

        StepVerifier.create(stream.take(1))
                .then(() -> {
                    RateSheetActivatedMessage message =
                            new RateSheetActivatedMessage(7L, "FANNIE_MAE", EFFECTIVE, EXPIRES);
                    registry.broadcast(message);
                })
                .expectNextMatches(event -> "rate-alert".equals(event.event()))
                .verifyComplete();
    }
}
