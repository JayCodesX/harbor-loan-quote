package com.jaycodesx.mortgage.quote.controller;

import com.jaycodesx.mortgage.infrastructure.security.UserTokenAuthorizationService;
import com.jaycodesx.mortgage.quote.dto.LoanQuoteResponseDto;
import com.jaycodesx.mortgage.quote.dto.QuoteRefinementRequestDto;
import com.jaycodesx.mortgage.quote.service.LoanQuoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/borrower/quotes")
public class BorrowerQuoteController {

    private final LoanQuoteService loanQuoteService;
    private final UserTokenAuthorizationService userTokenAuthorizationService;

    public BorrowerQuoteController(LoanQuoteService loanQuoteService, UserTokenAuthorizationService userTokenAuthorizationService) {
        this.loanQuoteService = loanQuoteService;
        this.userTokenAuthorizationService = userTokenAuthorizationService;
    }

    @GetMapping
    public Mono<ResponseEntity<?>> listQuotes(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            String userId = userTokenAuthorizationService.extractSubject(authorizationHeader);
            List<LoanQuoteResponseDto> quotes = loanQuoteService.getQuotesByUserId(userId);
            return Mono.just(ResponseEntity.ok(quotes));
        } catch (IllegalArgumentException ex) {
            HttpStatus status = ex.getMessage().contains("token") || ex.getMessage().contains("Missing") ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
            return Mono.just(ResponseEntity.status(status).body(ex.getMessage()));
        }
    }

    @GetMapping("/current")
    public Mono<ResponseEntity<?>> getCurrentQuote(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            String userId = userTokenAuthorizationService.extractSubject(authorizationHeader);
            return Mono.just(loanQuoteService.getCurrentQuoteByUserId(userId)
                    .map(q -> (ResponseEntity<?>) ResponseEntity.ok(q))
                    .orElseGet(() -> ResponseEntity.notFound().build()));
        } catch (IllegalArgumentException ex) {
            HttpStatus status = ex.getMessage().contains("token") || ex.getMessage().contains("Missing") ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
            return Mono.just(ResponseEntity.status(status).body(ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<?>> getQuote(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            userTokenAuthorizationService.requireAuthenticatedUser(authorizationHeader);
            return Mono.just(loanQuoteService.getQuote(id)
                    .map(q -> (ResponseEntity<?>) ResponseEntity.ok(q))
                    .orElseGet(() -> ResponseEntity.notFound().build()));
        } catch (IllegalArgumentException ex) {
            HttpStatus status = ex.getMessage().contains("token") || ex.getMessage().contains("Missing") ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
            return Mono.just(ResponseEntity.status(status).body(ex.getMessage()));
        }
    }

    @PostMapping("/attach-session")
    public Mono<ResponseEntity<?>> attachSession(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        try {
            String userId = userTokenAuthorizationService.extractSubject(authorizationHeader);
            List<LoanQuoteResponseDto> quotes = loanQuoteService.attachSessionToUser(sessionId, userId);
            return Mono.just(quotes.isEmpty()
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.ok(quotes));
        } catch (IllegalArgumentException ex) {
            HttpStatus status = ex.getMessage().contains("token") || ex.getMessage().contains("Missing") ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
            return Mono.just(ResponseEntity.status(status).body(ex.getMessage()));
        }
    }

    @PostMapping("/{id}/refine")
    public Mono<ResponseEntity<?>> refineQuote(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody QuoteRefinementRequestDto request
    ) {
        try {
            String userId = userTokenAuthorizationService.extractSubject(authorizationHeader);
            LoanQuoteResponseDto refinedQuote = loanQuoteService.refineQuote(id, sessionId, userId, request);
            return Mono.just(ResponseEntity.ok(refinedQuote));
        } catch (IllegalArgumentException ex) {
            HttpStatus status = ex.getMessage().contains("token") || ex.getMessage().contains("Missing") ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
            return Mono.just(ResponseEntity.status(status).body(ex.getMessage()));
        }
    }

    @PostMapping("/refine-latest")
    public Mono<ResponseEntity<?>> refineLatestQuote(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody QuoteRefinementRequestDto request
    ) {
        try {
            String userId = userTokenAuthorizationService.extractSubject(authorizationHeader);
            LoanQuoteResponseDto refinedQuote = loanQuoteService.refineLatestQuote(sessionId, userId, request);
            return Mono.just(ResponseEntity.ok(refinedQuote));
        } catch (IllegalArgumentException ex) {
            HttpStatus status = ex.getMessage().contains("token") || ex.getMessage().contains("Missing") ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
            return Mono.just(ResponseEntity.status(status).body(ex.getMessage()));
        }
    }
}
