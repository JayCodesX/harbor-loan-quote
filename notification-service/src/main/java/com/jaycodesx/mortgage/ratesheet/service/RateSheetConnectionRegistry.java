package com.jaycodesx.mortgage.ratesheet.service;

import com.jaycodesx.mortgage.ratesheet.messaging.RateSheetActivatedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains open SSE connections from borrower browsers per ADR-0049.
 *
 * <p>Each active browser session maps to a {@link Sinks.Many} sink. When a
 * {@code RateSheetActivated} event arrives, {@link #broadcast(RateSheetActivatedMessage)}
 * pushes it to every registered sink. Borrowers with an active connection receive the
 * update in real time; those without one fall back to email (handled elsewhere).
 *
 * <p>Connections are registered on SSE stream subscribe and deregistered via
 * {@code doFinally} when the stream terminates (browser disconnect, server restart,
 * or timeout). The backing sink is unicast — each session ID maps to exactly one
 * active browser tab.
 */
@Service
public class RateSheetConnectionRegistry {

    private static final Logger log = LoggerFactory.getLogger(RateSheetConnectionRegistry.class);

    private final ConcurrentHashMap<String, Sinks.Many<ServerSentEvent<RateSheetActivatedMessage>>> connections =
            new ConcurrentHashMap<>();

    /**
     * Registers a new SSE connection for the given session and returns the Flux to stream.
     * If a previous connection existed for this sessionId it is replaced and completed.
     *
     * @param sessionId the borrower's session identifier
     * @return a Flux of {@link ServerSentEvent} to be returned from the SSE endpoint
     */
    public Flux<ServerSentEvent<RateSheetActivatedMessage>> register(String sessionId) {
        Sinks.Many<ServerSentEvent<RateSheetActivatedMessage>> sink =
                Sinks.many().unicast().onBackpressureBuffer();

        Sinks.Many<ServerSentEvent<RateSheetActivatedMessage>> previous = connections.put(sessionId, sink);
        if (previous != null) {
            previous.tryEmitComplete();
            log.debug("RateSheetConnectionRegistry: replaced existing connection [sessionId={}]", sessionId);
        }

        log.info("RateSheetConnectionRegistry: registered SSE connection [sessionId={}, total={}]",
                sessionId, connections.size());
        return sink.asFlux();
    }

    /**
     * Deregisters the SSE connection for the given session.
     * Called from {@code doFinally} when the Flux terminates.
     */
    public void deregister(String sessionId) {
        connections.remove(sessionId);
        log.info("RateSheetConnectionRegistry: deregistered SSE connection [sessionId={}, remaining={}]",
                sessionId, connections.size());
    }

    /**
     * Broadcasts a rate sheet activated event to all currently connected borrower sessions.
     *
     * @param message the activated rate sheet event
     * @return the number of sessions that received the event
     */
    public int broadcast(RateSheetActivatedMessage message) {
        if (connections.isEmpty()) {
            log.debug("RateSheetConnectionRegistry: broadcast skipped — no active connections");
            return 0;
        }

        ServerSentEvent<RateSheetActivatedMessage> event = ServerSentEvent
                .<RateSheetActivatedMessage>builder()
                .event("rate-alert")
                .id(String.valueOf(message.rateSheetId()))
                .data(message)
                .build();

        int delivered = 0;
        for (var entry : connections.entrySet()) {
            Sinks.EmitResult result = entry.getValue().tryEmitNext(event);
            if (result.isSuccess()) {
                delivered++;
            } else {
                log.warn("RateSheetConnectionRegistry: failed to emit to session [sessionId={}, result={}]",
                        entry.getKey(), result);
            }
        }

        log.info("RateSheetConnectionRegistry: broadcast complete [rateSheetId={}, delivered={}/{}]",
                message.rateSheetId(), delivered, connections.size());
        return delivered;
    }

    /** Returns the number of currently active SSE connections. Exposed for metrics and testing. */
    public int activeConnectionCount() {
        return connections.size();
    }
}
