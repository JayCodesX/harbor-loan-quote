package com.jaycodesx.mortgage.quote.service;

import com.jaycodesx.mortgage.lead.model.MortgageLead;
import com.jaycodesx.mortgage.lead.repository.MortgageLeadRepository;
import com.jaycodesx.mortgage.infrastructure.metrics.QuoteMetricsService;
import com.jaycodesx.mortgage.pricing.dto.QuoteCalculationRequestDto;
import com.jaycodesx.mortgage.pricing.dto.QuoteCalculationResponseDto;
import com.jaycodesx.mortgage.pricing.service.PricingServiceClient;
import com.jaycodesx.mortgage.quote.dto.LoanQuoteResponseDto;
import com.jaycodesx.mortgage.quote.dto.PublicLoanQuoteRequestDto;
import com.jaycodesx.mortgage.quote.dto.QuoteRefinementRequestDto;
import com.jaycodesx.mortgage.quote.model.BorrowerQuoteProfile;
import com.jaycodesx.mortgage.quote.model.LoanQuote;
import com.jaycodesx.mortgage.quote.repository.BorrowerQuoteProfileRepository;
import com.jaycodesx.mortgage.quote.repository.LoanQuoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanQuoteServiceTest {

    @Mock
    private LoanQuoteRepository loanQuoteRepository;

    @Mock
    private BorrowerQuoteProfileRepository borrowerQuoteProfileRepository;

    @Mock
    private MortgageLeadRepository mortgageLeadRepository;

    @Mock
    private QuoteSessionService quoteSessionService;

    @Mock
    private PricingServiceClient pricingServiceClient;

    @Mock
    private QuoteMetricsService quoteMetricsService;

    @Mock
    private QuoteNotificationPublisher quoteNotificationPublisher;

    @InjectMocks
    private LoanQuoteService loanQuoteService;

    @Test
    void createPublicQuoteCallsPricingAndReturnsCompleted() throws Exception {
        PublicLoanQuoteRequestDto request = new PublicLoanQuoteRequestDto(
                new BigDecimal("450000.00"),
                new BigDecimal("90000.00"),
                "60614",
                "conventional",
                "primary_residence",
                30
        );
        when(quoteSessionService.resolveSessionId("session-1")).thenReturn("session-1");
        when(quoteSessionService.fingerprintPublicQuote("session-1", request)).thenReturn("fp-1");
        when(quoteSessionService.findQuoteId("fp-1")).thenReturn(Optional.empty());
        when(loanQuoteRepository.save(any(LoanQuote.class))).thenAnswer(invocation -> {
            LoanQuote quote = invocation.getArgument(0);
            java.lang.reflect.Field idField = LoanQuote.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(quote, 10L);
            return quote;
        });
        QuoteCalculationResponseDto pricingResult = new QuoteCalculationResponseDto(
                10L, "PUBLIC", "ESTIMATED",
                new BigDecimal("6.0500"), new BigDecimal("6.2300"), new BigDecimal("360000.00"),
                new BigDecimal("2178.44"), new BigDecimal("99450.00"), "Market Estimate"
        );
        when(pricingServiceClient.calculate(any(QuoteCalculationRequestDto.class))).thenReturn(pricingResult);

        LoanQuoteResponseDto result = loanQuoteService.createPublicQuote("session-1", request);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.sessionId()).isEqualTo("session-1");
        assertThat(result.processingStatus()).isEqualTo("COMPLETED");
        assertThat(result.quoteStatus()).isEqualTo("ESTIMATED");
        assertThat(result.estimatedRate()).isEqualByComparingTo("6.0500");
        verify(pricingServiceClient).calculate(any(QuoteCalculationRequestDto.class));
        verify(quoteMetricsService).recordQuoteStarted(10L, "session-1");
        verify(quoteNotificationPublisher).publish(any(QuoteNotificationMessage.class));
    }

    @Test
    void createPublicQuoteReturnsDuplicateWhenFingerprintMatches() throws Exception {
        PublicLoanQuoteRequestDto request = new PublicLoanQuoteRequestDto(
                new BigDecimal("450000.00"),
                new BigDecimal("90000.00"),
                "60614",
                "CONVENTIONAL",
                "PRIMARY_RESIDENCE",
                30
        );
        LoanQuote quote = buildQuote();
        quote.setSessionId("session-1");
        quote.setProcessingStatus("COMPLETED");
        when(quoteSessionService.resolveSessionId("session-1")).thenReturn("session-1");
        when(quoteSessionService.fingerprintPublicQuote("session-1", request)).thenReturn("fp-1");
        when(quoteSessionService.findQuoteId("fp-1")).thenReturn(Optional.of(5L));
        when(loanQuoteRepository.findById(5L)).thenReturn(Optional.of(quote));
        when(borrowerQuoteProfileRepository.findByLoanQuoteId(5L)).thenReturn(Optional.empty());
        when(mortgageLeadRepository.findByLoanQuoteId(5L)).thenReturn(Optional.empty());

        LoanQuoteResponseDto result = loanQuoteService.createPublicQuote("session-1", request);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.duplicate()).isTrue();
        assertThat(result.processingStatus()).isEqualTo("COMPLETED");
        verify(quoteMetricsService).recordQuoteDeduped();
    }

    @Test
    void getQuoteIncludesProfileAndLead() throws Exception {
        LoanQuote quote = buildQuote();
        BorrowerQuoteProfile profile = new BorrowerQuoteProfile();
        profile.setLoanQuoteId(5L);
        MortgageLead lead = new MortgageLead();
        lead.setLoanQuoteId(5L);
        lead.setLeadStatus("NEW");
        lead.setLeadSource("PUBLIC_QUOTE_FUNNEL");
        when(loanQuoteRepository.findById(5L)).thenReturn(Optional.of(quote));
        when(borrowerQuoteProfileRepository.findByLoanQuoteId(5L)).thenReturn(Optional.of(profile));
        when(mortgageLeadRepository.findByLoanQuoteId(5L)).thenReturn(Optional.of(lead));

        Optional<LoanQuoteResponseDto> result = loanQuoteService.getQuote(5L);

        assertThat(result).isPresent();
        assertThat(result.get().borrowerProfileCaptured()).isTrue();
        assertThat(result.get().lead()).isNotNull();
        assertThat(result.get().sessionId()).isEqualTo("session-5");
    }

    @Test
    void refineQuoteCallsPricingAndReturnsCompleted() throws Exception {
        LoanQuote quote = buildQuote();
        BorrowerQuoteProfile savedProfile = new BorrowerQuoteProfile();
        java.lang.reflect.Field profileId = BorrowerQuoteProfile.class.getDeclaredField("id");
        profileId.setAccessible(true);
        profileId.set(savedProfile, 8L);
        QuoteRefinementRequestDto request = new QuoteRefinementRequestDto(
                "Jay", "Stone", "jay@jaycodesx.dev", "555-111-0101",
                new BigDecimal("140000.00"), new BigDecimal("900.00"), 760,
                new BigDecimal("45000.00"), true, false
        );
        when(quoteSessionService.resolveSessionId("session-5")).thenReturn("session-5");
        when(quoteSessionService.fingerprintRefinedQuote("session-5", 5L, request)).thenReturn("refine-1");
        when(quoteSessionService.findQuoteId("refine-1")).thenReturn(Optional.empty());
        when(loanQuoteRepository.findById(5L)).thenReturn(Optional.of(quote));
        when(loanQuoteRepository.save(any(LoanQuote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(borrowerQuoteProfileRepository.findByLoanQuoteId(5L)).thenReturn(Optional.empty());
        when(borrowerQuoteProfileRepository.save(any(BorrowerQuoteProfile.class))).thenReturn(savedProfile);
        when(mortgageLeadRepository.findByLoanQuoteId(5L)).thenReturn(Optional.empty());
        when(mortgageLeadRepository.save(any(MortgageLead.class))).thenAnswer(invocation -> invocation.getArgument(0));
        QuoteCalculationResponseDto pricingResult = new QuoteCalculationResponseDto(
                5L, "REFINED", "LEAD_READY",
                new BigDecimal("5.9500"), new BigDecimal("6.1300"), new BigDecimal("360000.00"),
                new BigDecimal("2145.00"), new BigDecimal("99500.00"), "Prime"
        );
        when(pricingServiceClient.calculate(any(QuoteCalculationRequestDto.class))).thenReturn(pricingResult);

        LoanQuoteResponseDto response = loanQuoteService.refineQuote(5L, "session-5", request);

        assertThat(response.processingStatus()).isEqualTo("COMPLETED");
        assertThat(response.quoteStatus()).isEqualTo("LEAD_READY");
        assertThat(response.borrowerProfileCaptured()).isTrue();
        verify(pricingServiceClient).calculate(any(QuoteCalculationRequestDto.class));
        verify(quoteMetricsService).recordQuoteRefinementRequested(5L, "session-5");
        verify(quoteNotificationPublisher).publish(any(QuoteNotificationMessage.class));
    }

    @Test
    void createPublicQuoteRejectsInvalidInputs() {
        assertThatThrownBy(() -> loanQuoteService.createPublicQuote(new PublicLoanQuoteRequestDto(
                new BigDecimal("450000.00"),
                new BigDecimal("450000.00"),
                "60614",
                "CONVENTIONAL",
                "PRIMARY_RESIDENCE",
                30
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("downPayment must be less than homePrice");
    }

    private LoanQuote buildQuote() throws Exception {
        LoanQuote quote = new LoanQuote();
        java.lang.reflect.Field idField = LoanQuote.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(quote, 5L);
        quote.setSessionId("session-5");
        quote.setRequestFingerprint("fp-5");
        quote.setProcessingStatus("COMPLETED");
        quote.setHomePrice(new BigDecimal("450000.00"));
        quote.setDownPayment(new BigDecimal("90000.00"));
        quote.setFinancedAmount(new BigDecimal("360000.00"));
        quote.setZipCode("60614");
        quote.setLoanProgram("CONVENTIONAL");
        quote.setPropertyUse("PRIMARY_RESIDENCE");
        quote.setTermYears(30);
        quote.setEstimatedRate(new BigDecimal("6.1000"));
        quote.setEstimatedApr(new BigDecimal("6.2800"));
        quote.setEstimatedMonthlyPayment(new BigDecimal("2200.00"));
        quote.setEstimatedCashToClose(new BigDecimal("99500.00"));
        quote.setQualificationTier("Prime");
        quote.setQuoteStage("PUBLIC");
        quote.setQuoteStatus("ESTIMATED");
        return quote;
    }
}
