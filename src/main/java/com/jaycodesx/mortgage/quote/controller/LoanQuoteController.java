package com.jaycodesx.mortgage.quote.controller;

import com.jaycodesx.mortgage.infrastructure.security.UserTokenAuthorizationService;
import com.jaycodesx.mortgage.quote.dto.LoanQuoteResponseDto;
import com.jaycodesx.mortgage.quote.dto.PublicLoanQuoteRequestDto;
import com.jaycodesx.mortgage.quote.dto.QuoteRefinementRequestDto;
import com.jaycodesx.mortgage.quote.service.LoanQuoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@RestController
@RequestMapping("/loan-quotes")
public class LoanQuoteController {

    private final LoanQuoteService loanQuoteService;
    private final UserTokenAuthorizationService userTokenAuthorizationService;

    public LoanQuoteController(LoanQuoteService loanQuoteService, UserTokenAuthorizationService userTokenAuthorizationService) {
        this.loanQuoteService = loanQuoteService;
        this.userTokenAuthorizationService = userTokenAuthorizationService;
    }

    @PostMapping("/public")
    public Mono<ResponseEntity<?>> createPublicQuote(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody PublicLoanQuoteRequestDto request
    ) {
        try {
            LoanQuoteResponseDto quote = loanQuoteService.createPublicQuote(sessionId, request);
            return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(quote));
        } catch (IllegalArgumentException ex) {
            return Mono.just(ResponseEntity.badRequest().body(ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<LoanQuoteResponseDto>> getQuote(@PathVariable Long id) {
        return Mono.just(
                loanQuoteService.getQuote(id)
                        .map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.notFound().build())
        );
    }

    @GetMapping(path = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<LoanQuoteResponseDto>> streamQuote(@PathVariable Long id) {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(2))
                .map(tick -> loanQuoteService.getQuote(id))
                .takeUntil(optionalQuote -> optionalQuote
                        .map(quote -> !"QUEUED".equalsIgnoreCase(quote.processingStatus())
                                && !"PROCESSING".equalsIgnoreCase(quote.processingStatus())
                                && !"PENDING".equalsIgnoreCase(quote.processingStatus()))
                        .orElse(true))
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .map(quote -> ServerSentEvent.<LoanQuoteResponseDto>builder()
                        .event("quote-update")
                        .id(String.valueOf(quote.id()))
                        .data(quote)
                        .build());
    }

    @PostMapping("/{id}/refine")
    public Mono<ResponseEntity<?>> refineQuote(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @Valid @RequestBody QuoteRefinementRequestDto request
    ) {
        try {
            userTokenAuthorizationService.requireAuthenticatedUser(authorizationHeader);
            String ipAddress = resolveIpAddress(forwardedFor);
            LoanQuoteResponseDto refinedQuote = loanQuoteService.refineQuote(id, sessionId, request, ipAddress, userAgent);
            return Mono.just(ResponseEntity.ok(refinedQuote));
        } catch (IllegalArgumentException ex) {
            HttpStatus status = ex.getMessage().contains("token") ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
            return Mono.just(ResponseEntity.status(status).body(ex.getMessage()));
        }
    }

    private String resolveIpAddress(String forwardedFor) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return "unknown";
    }
}
