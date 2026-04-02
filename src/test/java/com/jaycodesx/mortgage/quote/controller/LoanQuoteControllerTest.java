package com.jaycodesx.mortgage.quote.controller;

import com.jaycodesx.mortgage.infrastructure.security.UserTokenAuthorizationService;
import com.jaycodesx.mortgage.quote.dto.LoanQuoteResponseDto;
import com.jaycodesx.mortgage.quote.dto.PublicLoanQuoteRequestDto;
import com.jaycodesx.mortgage.quote.dto.QuoteRefinementRequestDto;
import com.jaycodesx.mortgage.quote.service.LoanQuoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanQuoteControllerTest {

    @Mock
    private LoanQuoteService loanQuoteService;
    @Mock
    private UserTokenAuthorizationService userTokenAuthorizationService;

    @InjectMocks
    private LoanQuoteController loanQuoteController;

    @Test
    void createPublicQuoteReturnsCreatedAndBadRequest() {
        PublicLoanQuoteRequestDto request = new PublicLoanQuoteRequestDto(
                new BigDecimal("450000.00"),
                new BigDecimal("90000.00"),
                "60614",
                "CONVENTIONAL",
                "PRIMARY_RESIDENCE",
                30
        );
        LoanQuoteResponseDto responseDto = new LoanQuoteResponseDto(
                1L, null, "session-1", "COMPLETED", false, "PUBLIC", "ESTIMATED", false, false,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "60614", "CONVENTIONAL",
                "PRIMARY_RESIDENCE", 30, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                "Market Estimate", "next", null, null, null
        );
        when(loanQuoteService.createPublicQuote("session-1", request)).thenReturn(responseDto);

        ResponseEntity<?> success = loanQuoteController.createPublicQuote("session-1", request).block();
        assertThat(success.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        when(loanQuoteService.createPublicQuote("session-1", request)).thenThrow(new IllegalArgumentException("invalid"));
        ResponseEntity<?> failure = loanQuoteController.createPublicQuote("session-1", request).block();
        assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getQuoteReturnsFoundOrNotFound() {
        LoanQuoteResponseDto responseDto = new LoanQuoteResponseDto(
                1L, null, "session-1", "COMPLETED", false, "PUBLIC", "ESTIMATED", false, false,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "60614", "CONVENTIONAL",
                "PRIMARY_RESIDENCE", 30, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                "Market Estimate", "next", null, null, null
        );
        when(loanQuoteService.getQuote(1L)).thenReturn(Optional.of(responseDto));
        when(loanQuoteService.getQuote(2L)).thenReturn(Optional.empty());

        assertThat(loanQuoteController.getQuote(1L).block().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loanQuoteController.getQuote(2L).block().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void refineQuoteReturnsSuccessAndBadRequest() {
        QuoteRefinementRequestDto request = new QuoteRefinementRequestDto(
                "Jay", "Lane", "jay@jaycodesx.dev", "555-111-0101",
                new BigDecimal("125000.00"), new BigDecimal("900.00"), 735,
                new BigDecimal("24000.00"), true, false,
                true, true, true, "I agree to be contacted by Harbor Mortgage and its partners."
        );
        LoanQuoteResponseDto responseDto = new LoanQuoteResponseDto(
                1L, null, "session-1", "COMPLETED", false, "REFINED", "LEAD_READY", true, true,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "60614", "CONVENTIONAL",
                "PRIMARY_RESIDENCE", 30, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                "Prime", "next", null, null, null
        );
        when(loanQuoteService.refineQuote(1L, "session-1", request, "unknown", null)).thenReturn(responseDto);

        assertThat(loanQuoteController.refineQuote(1L, "Bearer token", "session-1", null, null, request).block().getStatusCode()).isEqualTo(HttpStatus.OK);

        when(loanQuoteService.refineQuote(2L, "session-1", request, "unknown", null)).thenThrow(new IllegalArgumentException("not found"));
        assertThat(loanQuoteController.refineQuote(2L, "Bearer token", "session-1", null, null, request).block().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void streamQuoteReturnsUpdateEvent() {
        LoanQuoteResponseDto responseDto = new LoanQuoteResponseDto(
                1L, null, "session-1", "PROCESSING", false, "PUBLIC", "REQUESTED", false, false,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "60614", "CONVENTIONAL",
                "PRIMARY_RESIDENCE", 30, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                "Market Estimate", "next", null, null, null
        );
        when(loanQuoteService.getQuote(1L)).thenReturn(Optional.of(responseDto), Optional.of(responseDto));

        ServerSentEvent<LoanQuoteResponseDto> event = loanQuoteController.streamQuote(1L).blockFirst();

        assertThat(event).isNotNull();
        assertThat(event.event()).isEqualTo("quote-update");
        assertThat(event.data()).isEqualTo(responseDto);
    }
}
