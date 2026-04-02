package com.jaycodesx.mortgage.quote.service;

import com.jaycodesx.mortgage.infrastructure.metrics.QuoteMetricsService;
import com.jaycodesx.mortgage.pricing.service.QuotePricingService;
import com.jaycodesx.mortgage.quote.dto.QuoteCalculationRequestDto;
import com.jaycodesx.mortgage.quote.dto.QuoteCalculationResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteJobProcessorTest {

    @Mock
    private QuotePricingService quotePricingService;

    @Mock
    private QuoteMetricsService quoteMetricsService;

    @InjectMocks
    private QuoteJobProcessor quoteJobProcessor;

    @Test
    void calculatesPublicQuote() {
        QuotePricingService.QuoteDecision decision = new QuotePricingService.QuoteDecision(
                new BigDecimal("6.0500"), new BigDecimal("6.2300"), new BigDecimal("360000.00"),
                new BigDecimal("2178.44"), new BigDecimal("99450.00"), "Market Estimate"
        );
        when(quotePricingService.pricePublicQuote(any())).thenReturn(decision);

        QuoteCalculationRequestDto request = new QuoteCalculationRequestDto(
                "PUBLIC_QUOTE", 5L,
                new BigDecimal("450000.00"), new BigDecimal("90000.00"),
                "60614", "CONVENTIONAL", "PRIMARY_RESIDENCE", 30,
                null, null, null, null, null, null
        );

        QuoteCalculationResponseDto result = quoteJobProcessor.calculate(request);

        assertThat(result.quoteId()).isEqualTo(5L);
        assertThat(result.quoteStage()).isEqualTo("PUBLIC");
        assertThat(result.quoteStatus()).isEqualTo("ESTIMATED");
        assertThat(result.estimatedRate()).isEqualByComparingTo("6.0500");
        assertThat(result.qualificationTier()).isEqualTo("Market Estimate");
        verify(quoteMetricsService).recordQuoteCompleted(5L);
    }

    @Test
    void calculatesRefinedQuote() {
        QuotePricingService.QuoteDecision decision = new QuotePricingService.QuoteDecision(
                new BigDecimal("5.9500"), new BigDecimal("6.1300"), new BigDecimal("360000.00"),
                new BigDecimal("2145.00"), new BigDecimal("99500.00"), "Prime"
        );
        when(quotePricingService.priceRefinedQuote(any(), any())).thenReturn(decision);

        QuoteCalculationRequestDto request = new QuoteCalculationRequestDto(
                "REFINED_QUOTE", 5L,
                new BigDecimal("450000.00"), new BigDecimal("90000.00"),
                "60614", "CONVENTIONAL", "PRIMARY_RESIDENCE", 30,
                740, new BigDecimal("120000.00"), new BigDecimal("900.00"),
                new BigDecimal("30000.00"), true, false
        );

        QuoteCalculationResponseDto result = quoteJobProcessor.calculate(request);

        assertThat(result.quoteId()).isEqualTo(5L);
        assertThat(result.quoteStage()).isEqualTo("REFINED");
        assertThat(result.quoteStatus()).isEqualTo("LEAD_READY");
        assertThat(result.estimatedRate()).isEqualByComparingTo("5.9500");
        assertThat(result.qualificationTier()).isEqualTo("Prime");
        verify(quoteMetricsService).recordQuoteCompleted(5L);
    }
}
