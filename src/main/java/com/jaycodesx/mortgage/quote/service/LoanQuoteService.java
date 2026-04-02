package com.jaycodesx.mortgage.quote.service;

import com.jaycodesx.mortgage.lead.dto.MortgageLeadResponseDto;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class LoanQuoteService {

    private final LoanQuoteRepository loanQuoteRepository;
    private final BorrowerQuoteProfileRepository borrowerQuoteProfileRepository;
    private final MortgageLeadRepository mortgageLeadRepository;
    private final QuoteSessionService quoteSessionService;
    private final PricingServiceClient pricingServiceClient;
    private final QuoteMetricsService quoteMetricsService;
    private final QuoteNotificationPublisher quoteNotificationPublisher;

    public LoanQuoteService(
            LoanQuoteRepository loanQuoteRepository,
            BorrowerQuoteProfileRepository borrowerQuoteProfileRepository,
            MortgageLeadRepository mortgageLeadRepository,
            QuoteSessionService quoteSessionService,
            PricingServiceClient pricingServiceClient,
            QuoteMetricsService quoteMetricsService,
            QuoteNotificationPublisher quoteNotificationPublisher
    ) {
        this.loanQuoteRepository = loanQuoteRepository;
        this.borrowerQuoteProfileRepository = borrowerQuoteProfileRepository;
        this.mortgageLeadRepository = mortgageLeadRepository;
        this.quoteSessionService = quoteSessionService;
        this.pricingServiceClient = pricingServiceClient;
        this.quoteMetricsService = quoteMetricsService;
        this.quoteNotificationPublisher = quoteNotificationPublisher;
    }

    public LoanQuoteResponseDto createPublicQuote(PublicLoanQuoteRequestDto request) {
        return createPublicQuote(null, request);
    }

    public LoanQuoteResponseDto createPublicQuote(String sessionId, PublicLoanQuoteRequestDto request) {
        validateQuoteInputs(request.homePrice(), request.downPayment(), request.termYears());

        String resolvedSessionId = quoteSessionService.resolveSessionId(sessionId);
        String fingerprint = quoteSessionService.fingerprintPublicQuote(resolvedSessionId, request);
        Optional<Long> existingQuoteId = quoteSessionService.findQuoteId(fingerprint);
        if (existingQuoteId.isPresent()) {
            Optional<LoanQuoteResponseDto> existing = getQuote(existingQuoteId.get());
            if (existing.isPresent()) {
                quoteMetricsService.recordQuoteDeduped();
                return markDuplicate(existing.get(), resolvedSessionId);
            }
        }

        LoanQuote quote = new LoanQuote();
        quote.setSessionId(resolvedSessionId);
        quote.setRequestFingerprint(fingerprint);
        quote.setProcessingStatus("PROCESSING");
        quote.setHomePrice(request.homePrice().setScale(2, RoundingMode.HALF_UP));
        quote.setDownPayment(request.downPayment().setScale(2, RoundingMode.HALF_UP));
        quote.setFinancedAmount(request.homePrice().subtract(request.downPayment()).setScale(2, RoundingMode.HALF_UP));
        quote.setZipCode(request.zipCode());
        quote.setLoanProgram(request.loanProgram().trim().toUpperCase());
        quote.setPropertyUse(request.propertyUse().trim().toUpperCase());
        quote.setTermYears(request.termYears());
        quote.setQuoteStage("PUBLIC");
        quote.setQuoteStatus("REQUESTED");
        quote.setLeadCaptured(false);
        LoanQuote savedQuote = loanQuoteRepository.save(quote);
        quoteMetricsService.recordQuoteStarted(savedQuote.getId(), resolvedSessionId);

        QuoteCalculationResponseDto result = pricingServiceClient.calculate(new QuoteCalculationRequestDto(
                "PUBLIC_QUOTE",
                savedQuote.getId(),
                savedQuote.getHomePrice(),
                savedQuote.getDownPayment(),
                savedQuote.getZipCode(),
                savedQuote.getLoanProgram(),
                savedQuote.getPropertyUse(),
                savedQuote.getTermYears(),
                null, null, null, null, null, null
        ));

        applyPricingResult(savedQuote, result);
        quoteSessionService.rememberQuote(fingerprint, savedQuote.getId(), savedQuote.getProcessingStatus());

        LoanQuoteResponseDto response = toResponse(savedQuote, Optional.empty(), Optional.empty(), false, resolvedSessionId);
        quoteNotificationPublisher.publish(QuoteNotificationMessage.fromResponse(response));
        return response;
    }

    public Optional<LoanQuoteResponseDto> getQuote(Long id) {
        return loanQuoteRepository.findById(id)
                .map(quote -> toResponse(
                        quote,
                        borrowerQuoteProfileRepository.findByLoanQuoteId(id),
                        mortgageLeadRepository.findByLoanQuoteId(id),
                        false,
                        quote.getSessionId()
                ));
    }

    public LoanQuoteResponseDto refineQuote(Long id, QuoteRefinementRequestDto request) {
        return refineQuote(id, null, request);
    }

    public LoanQuoteResponseDto refineLatestQuote(String sessionId, String userId, QuoteRefinementRequestDto request) {
        LoanQuote quote = (userId != null
                ? loanQuoteRepository.findTopByUserIdOrderByUpdatedAtDesc(userId)
                : loanQuoteRepository.findTopBySessionIdOrderByIdDesc(sessionId))
                .orElseThrow(() -> new IllegalArgumentException("No quote found for session"));
        return refineQuote(quote.getId(), sessionId, userId, request);
    }

    public List<LoanQuoteResponseDto> getQuotesByUserId(String userId) {
        return loanQuoteRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(q -> toResponse(
                        q,
                        borrowerQuoteProfileRepository.findByLoanQuoteId(q.getId()),
                        mortgageLeadRepository.findByLoanQuoteId(q.getId()),
                        false,
                        q.getSessionId()
                ))
                .toList();
    }

    public Optional<LoanQuoteResponseDto> getCurrentQuoteByUserId(String userId) {
        return loanQuoteRepository.findTopByUserIdOrderByUpdatedAtDesc(userId)
                .map(q -> toResponse(
                        q,
                        borrowerQuoteProfileRepository.findByLoanQuoteId(q.getId()),
                        mortgageLeadRepository.findByLoanQuoteId(q.getId()),
                        false,
                        q.getSessionId()
                ));
    }

    public List<LoanQuoteResponseDto> attachSessionToUser(String sessionId, String userId) {
        List<LoanQuote> unlinked = loanQuoteRepository.findBySessionIdAndUserIdIsNull(sessionId);
        unlinked.forEach(q -> q.setUserId(userId));
        loanQuoteRepository.saveAll(unlinked);
        return getQuotesByUserId(userId);
    }

    public LoanQuoteResponseDto refineQuote(Long id, String sessionId, QuoteRefinementRequestDto request) {
        return refineQuote(id, sessionId, null, request);
    }

    public LoanQuoteResponseDto refineQuote(Long id, String sessionId, String userId, QuoteRefinementRequestDto request) {
        LoanQuote quote = loanQuoteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found for id: " + id));

        String resolvedSessionId = quoteSessionService.resolveSessionId(sessionId != null ? sessionId : quote.getSessionId());
        String fingerprint = quoteSessionService.fingerprintRefinedQuote(resolvedSessionId, id, request);
        Optional<Long> existingQuoteId = quoteSessionService.findQuoteId(fingerprint);
        if (existingQuoteId.isPresent() && existingQuoteId.get().equals(id)) {
            Optional<LoanQuoteResponseDto> existing = getQuote(id);
            if (existing.isPresent()) {
                quoteMetricsService.recordQuoteDeduped();
                return markDuplicate(existing.get(), resolvedSessionId);
            }
        }

        quote.setSessionId(resolvedSessionId);
        if (userId != null) {
            quote.setUserId(userId);
        }
        quote.setRequestFingerprint(fingerprint);
        quote.setProcessingStatus("PROCESSING");
        quote.setQuoteStatus("REFINEMENT_REQUESTED");
        LoanQuote savedQuote = loanQuoteRepository.save(quote);
        BorrowerQuoteProfile savedProfile = upsertBorrowerProfile(savedQuote.getId(), request);
        quoteMetricsService.recordQuoteRefinementRequested(savedQuote.getId(), resolvedSessionId);

        QuoteCalculationResponseDto result = pricingServiceClient.calculate(new QuoteCalculationRequestDto(
                "REFINED_QUOTE",
                savedQuote.getId(),
                savedQuote.getHomePrice(),
                savedQuote.getDownPayment(),
                savedQuote.getZipCode(),
                savedQuote.getLoanProgram(),
                savedQuote.getPropertyUse(),
                savedQuote.getTermYears(),
                request.creditScore(),
                request.annualIncome(),
                request.monthlyDebts(),
                request.cashReserves(),
                request.firstTimeBuyer(),
                request.vaEligible()
        ));

        applyPricingResult(savedQuote, result);

        MortgageLead lead = upsertLead(savedQuote.getId(), savedProfile.getId());
        quoteSessionService.rememberQuote(fingerprint, savedQuote.getId(), savedQuote.getProcessingStatus());
        quoteMetricsService.recordLeadCaptured(resolvedSessionId);

        LoanQuoteResponseDto response = toResponse(savedQuote, Optional.of(savedProfile), Optional.of(lead), false, resolvedSessionId);
        quoteNotificationPublisher.publish(QuoteNotificationMessage.fromResponse(response));
        return response;
    }

    public void applyLeadResult(LeadResultMessage message) {
        LoanQuote quote = loanQuoteRepository.findById(message.loanQuoteId())
                .orElseThrow(() -> new IllegalArgumentException("Quote not found for id: " + message.loanQuoteId()));
        quote.setLeadCaptured(true);
        quote.setQuoteStatus("LEAD_CAPTURED");
        loanQuoteRepository.save(quote);
        quoteMetricsService.recordLeadCaptured(quote.getSessionId());
        quoteSessionService.cacheQuoteStatus(quote.getId(), quote.getProcessingStatus());
        publishNotificationSnapshot(quote);
    }

    private void applyPricingResult(LoanQuote quote, QuoteCalculationResponseDto result) {
        quote.setProcessingStatus("COMPLETED");
        if (result.quoteStage() != null && !result.quoteStage().isBlank()) {
            quote.setQuoteStage(result.quoteStage());
        }
        if (result.quoteStatus() != null && !result.quoteStatus().isBlank()) {
            quote.setQuoteStatus(result.quoteStatus());
        }
        quote.setEstimatedRate(result.estimatedRate());
        quote.setEstimatedApr(result.estimatedApr());
        quote.setFinancedAmount(result.financedAmount());
        quote.setEstimatedMonthlyPayment(result.estimatedMonthlyPayment());
        quote.setEstimatedCashToClose(result.estimatedCashToClose());
        quote.setQualificationTier(result.qualificationTier());
        loanQuoteRepository.save(quote);
        quoteSessionService.cacheQuoteStatus(quote.getId(), quote.getProcessingStatus());
    }

    private void publishNotificationSnapshot(LoanQuote quote) {
        LoanQuoteResponseDto response = toResponse(
                quote,
                borrowerQuoteProfileRepository.findByLoanQuoteId(quote.getId()),
                mortgageLeadRepository.findByLoanQuoteId(quote.getId()),
                false,
                quote.getSessionId()
        );
        quoteNotificationPublisher.publish(QuoteNotificationMessage.fromResponse(response));
    }

    private LoanQuoteResponseDto markDuplicate(LoanQuoteResponseDto response, String sessionId) {
        return new LoanQuoteResponseDto(
                response.id(),
                response.userId(),
                sessionId,
                response.processingStatus(),
                true,
                response.quoteStage(),
                response.quoteStatus(),
                response.leadCaptured(),
                response.borrowerProfileCaptured(),
                response.homePrice(),
                response.downPayment(),
                response.financedAmount(),
                response.zipCode(),
                response.loanProgram(),
                response.propertyUse(),
                response.termYears(),
                response.estimatedRate(),
                response.estimatedApr(),
                response.estimatedMonthlyPayment(),
                response.estimatedCashToClose(),
                response.qualificationTier(),
                response.nextStep(),
                response.lead(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    private LoanQuoteResponseDto toResponse(
            LoanQuote quote,
            Optional<BorrowerQuoteProfile> profile,
            Optional<MortgageLead> lead,
            boolean duplicate,
            String sessionId
    ) {
        String nextStep = quote.isLeadCaptured()
                ? "Lead captured. Route this borrower to a loan officer or pricing workflow."
                : profile.isPresent()
                ? "Borrower profile saved. Refined quote and lead creation complete."
                : "Create an account or continue to provide borrower details for a refined quote.";

        return new LoanQuoteResponseDto(
                quote.getId(),
                quote.getUserId(),
                sessionId,
                defaultProcessingStatus(quote.getProcessingStatus()),
                duplicate,
                quote.getQuoteStage(),
                quote.getQuoteStatus(),
                quote.isLeadCaptured(),
                profile.isPresent(),
                quote.getHomePrice(),
                quote.getDownPayment(),
                quote.getFinancedAmount(),
                quote.getZipCode(),
                quote.getLoanProgram(),
                quote.getPropertyUse(),
                quote.getTermYears(),
                quote.getEstimatedRate(),
                quote.getEstimatedApr(),
                quote.getEstimatedMonthlyPayment(),
                quote.getEstimatedCashToClose(),
                quote.getQualificationTier(),
                nextStep,
                lead.map(item -> new MortgageLeadResponseDto(
                        item.getId(),
                        item.getLoanQuoteId(),
                        item.getLeadStatus(),
                        item.getLeadSource()
                )).orElse(null),
                quote.getCreatedAt(),
                quote.getUpdatedAt()
        );
    }

    private String defaultProcessingStatus(String processingStatus) {
        return processingStatus == null || processingStatus.isBlank() ? "PROCESSING" : processingStatus;
    }

    private BorrowerQuoteProfile upsertBorrowerProfile(Long quoteId, QuoteRefinementRequestDto request) {
        BorrowerQuoteProfile profile = borrowerQuoteProfileRepository.findByLoanQuoteId(quoteId)
                .orElseGet(BorrowerQuoteProfile::new);
        profile.setLoanQuoteId(quoteId);
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setEmail(request.email());
        profile.setPhone(request.phone());
        profile.setAnnualIncome(request.annualIncome().setScale(2, RoundingMode.HALF_UP));
        profile.setMonthlyDebts(request.monthlyDebts().setScale(2, RoundingMode.HALF_UP));
        profile.setCreditScore(request.creditScore());
        profile.setCashReserves(request.cashReserves().setScale(2, RoundingMode.HALF_UP));
        profile.setFirstTimeBuyer(request.firstTimeBuyer());
        profile.setVaEligible(request.vaEligible());
        return borrowerQuoteProfileRepository.save(profile);
    }

    private MortgageLead upsertLead(Long quoteId, Long borrowerQuoteProfileId) {
        MortgageLead lead = mortgageLeadRepository.findByLoanQuoteId(quoteId).orElseGet(MortgageLead::new);
        lead.setLoanQuoteId(quoteId);
        lead.setBorrowerQuoteProfileId(borrowerQuoteProfileId);
        lead.setLeadStatus("NEW");
        lead.setLeadSource("PUBLIC_QUOTE_FUNNEL");
        return mortgageLeadRepository.save(lead);
    }

    private void validateQuoteInputs(BigDecimal homePrice, BigDecimal downPayment, Integer termYears) {
        if (homePrice == null || homePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("homePrice must be greater than 0");
        }
        if (downPayment == null || downPayment.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("downPayment must be 0 or greater");
        }
        if (downPayment.compareTo(homePrice) >= 0) {
            throw new IllegalArgumentException("downPayment must be less than homePrice");
        }
        if (termYears == null || termYears <= 0) {
            throw new IllegalArgumentException("termYears must be greater than 0");
        }
    }
}
