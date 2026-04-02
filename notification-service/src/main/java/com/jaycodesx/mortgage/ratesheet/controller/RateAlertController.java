package com.jaycodesx.mortgage.ratesheet.controller;

import com.jaycodesx.mortgage.ratesheet.messaging.RateSheetActivatedMessage;
import com.jaycodesx.mortgage.ratesheet.service.RateSheetConnectionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * WebFlux SSE endpoint for real-time rate change alerts per ADR-0049.
 *
 * <p>A borrower browser opens a persistent SSE connection by calling
 * {@code GET /rate-alerts/stream?sessionId={sessionId}}. The connection stays open
 * until the browser disconnects or the server restarts. When a new rate sheet is activated,
 * {@link RateSheetConnectionRegistry#broadcast} pushes a {@code rate-alert} SSE event to
 * every connected session simultaneously.
 *
 * <p>Connection cleanup is guaranteed via {@code doFinally} — the registry entry is removed
 * regardless of how the Flux terminates (normal completion, error, or cancel on disconnect).
 */
@RestController
@RequestMapping("/rate-alerts")
public class RateAlertController {

    private static final Logger log = LoggerFactory.getLogger(RateAlertController.class);

    private final RateSheetConnectionRegistry registry;

    public RateAlertController(RateSheetConnectionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Opens a persistent SSE stream for the given borrower session.
     *
     * <p>The stream stays open until the browser disconnects. On reconnect, the browser
     * sends the {@code Last-Event-ID} header automatically (SSE built-in reconnection);
     * this implementation does not replay missed events — borrowers will receive only
     * events published after reconnection.
     *
     * @param sessionId the borrower's active session identifier
     * @return a Flux of rate-alert SSE events
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<RateSheetActivatedMessage>> stream(
            @RequestParam String sessionId) {
        log.info("RateAlertController: SSE stream opened [sessionId={}]", sessionId);
        return registry.register(sessionId)
                .doFinally(signal -> {
                    log.info("RateAlertController: SSE stream closed [sessionId={}, signal={}]",
                            sessionId, signal);
                    registry.deregister(sessionId);
                });
    }
}
