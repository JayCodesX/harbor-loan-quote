package com.jaycodesx.mortgage.quote.service;

import com.jaycodesx.mortgage.infrastructure.metrics.QuoteMetricsService;
import com.jaycodesx.mortgage.pricing.service.QuotePricingService;
import com.jaycodesx.mortgage.quote.dto.QuoteCalculationRequestDto;
import com.jaycodesx.mortgage.quote.dto.QuoteCalculationResponseDto;
import com.jaycodesx.mortgage.quote.dto.QuoteRefinementRequestDto;
import org.springframework.stereotype.Service;

@Service
public class QuoteJobProcessor {

    private final QuotePricingService quotePricingService;
    private final QuoteMetricsService quoteMetricsService;

    public QuoteJobProcessor(
            QuotePricingService quotePricingService,
            QuoteMetricsService quoteMetricsService
    ) {
        this.quotePricingService = quotePricingService;
        this.quoteMetricsService = quoteMetricsService;
    }

    public QuoteCalculationResponseDto calculate(QuoteCalculationRequestDto request) {
        PricingScenario scenario = new PricingScenario(
                request.quoteId(),
                request.homePrice(),
                request.downPayment(),
                request.zipCode(),
                request.loanProgram(),
                request.propertyUse(),
                request.termYears()
        );

        if ("PUBLIC_QUOTE".equals(request.jobType())) {
            QuotePricingService.QuoteDecision decision = quotePricingService.pricePublicQuote(scenario);
            quoteMetricsService.recordQuoteCompleted(request.quoteId());
            return new QuoteCalculationResponseDto(
                    request.quoteId(),
                    "PUBLIC",
                    "ESTIMATED",
                    decision.estimatedRate(),
                    decision.estimatedApr(),
                    decision.financedAmount(),
                    decision.estimatedMonthlyPayment(),
                    decision.estimatedCashToClose(),
                    decision.qualificationTier()
            );
        }

        // REFINED_QUOTE — only financial inputs matter for pricing; identity fields are not used
        QuoteRefinementRequestDto refinement = new QuoteRefinementRequestDto(
                "", "", "calc@internal", "",
                request.annualIncome(),
                request.monthlyDebts(),
                request.creditScore(),
                request.cashReserves(),
                request.firstTimeBuyer(),
                request.vaEligible()
        );

        QuotePricingService.QuoteDecision decision = quotePricingService.priceRefinedQuote(scenario, refinement);
        quoteMetricsService.recordQuoteCompleted(request.quoteId());
        return new QuoteCalculationResponseDto(
                request.quoteId(),
                "REFINED",
                "LEAD_READY",
                decision.estimatedRate(),
                decision.estimatedApr(),
                decision.financedAmount(),
                decision.estimatedMonthlyPayment(),
                decision.estimatedCashToClose(),
                decision.qualificationTier()
        );
    }
}
