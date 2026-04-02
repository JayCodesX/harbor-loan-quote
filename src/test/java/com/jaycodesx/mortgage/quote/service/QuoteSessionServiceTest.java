package com.jaycodesx.mortgage.quote.service;

import com.jaycodesx.mortgage.quote.dto.PublicLoanQuoteRequestDto;
import com.jaycodesx.mortgage.quote.dto.QuoteRefinementRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteSessionServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private QuoteSessionService quoteSessionService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        quoteSessionService = new QuoteSessionService(redisTemplate);
    }

    @Test
    void resolvesSessionIdOrCreatesOne() {
        assertThat(quoteSessionService.resolveSessionId("session-1")).isEqualTo("session-1");
        assertThat(quoteSessionService.resolveSessionId(null)).isNotBlank();
    }

    @Test
    void createsStableFingerprints() {
        PublicLoanQuoteRequestDto request = new PublicLoanQuoteRequestDto(
                new BigDecimal("450000.00"), new BigDecimal("90000.00"), "60614", "CONVENTIONAL", "PRIMARY_RESIDENCE", 30
        );
        String first = quoteSessionService.fingerprintPublicQuote("session-1", request);
        String second = quoteSessionService.fingerprintPublicQuote("session-1", request);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }

    @Test
    void remembersAndFindsQuoteIds() {
        when(valueOperations.get("quote:fingerprint:fp-1")).thenReturn("12");

        quoteSessionService.rememberQuote("fp-1", 12L, "COMPLETED");
        Optional<Long> result = quoteSessionService.findQuoteId("fp-1");

        assertThat(result).contains(12L);
        verify(valueOperations).set(eq("quote:fingerprint:fp-1"), eq("12"), any());
        verify(valueOperations).set(eq("quote:status:12"), eq("COMPLETED"), any());
    }

    @Test
    void createsRefineFingerprint() {
        QuoteRefinementRequestDto request = new QuoteRefinementRequestDto(
                "Jay", "Lane", "jay@jaycodesx.dev", "555-111-0101",
                new BigDecimal("120000.00"), new BigDecimal("900.00"), 740,
                new BigDecimal("30000.00"), true, false,
                true, true, true, "I agree to be contacted by Harbor Mortgage and its partners."
        );

        String fingerprint = quoteSessionService.fingerprintRefinedQuote("session-1", 5L, request);

        assertThat(fingerprint).hasSize(64);
    }
}
