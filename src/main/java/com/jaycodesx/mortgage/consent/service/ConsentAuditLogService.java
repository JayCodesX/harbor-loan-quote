package com.jaycodesx.mortgage.consent.service;

import com.jaycodesx.mortgage.consent.model.ConsentAuditLog;
import com.jaycodesx.mortgage.consent.repository.ConsentAuditLogRepository;
import com.jaycodesx.mortgage.quote.dto.QuoteRefinementRequestDto;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Records and retrieves consent audit log entries.
 *
 * This service is the single point of entry for writing consent records. It never
 * exposes update or delete operations — all writes produce new immutable log entries.
 * Revocations are recorded as new REVOKED entries; the full consent history is preserved.
 */
@Service
public class ConsentAuditLogService {

    private final ConsentAuditLogRepository repository;

    public ConsentAuditLogService(ConsentAuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Records all consent events captured at the point of lead submission.
     * One entry is written per consent type. The consent language, IP address, and
     * user-agent are captured as presented to the borrower at submission time.
     *
     * @param loanQuoteId            the associated loan quote
     * @param borrowerQuoteProfileId the borrower profile that submitted consent
     * @param request                the refinement request containing consent flags and language
     * @param ipAddress              the borrower's IP address at time of submission
     * @param userAgent              the borrower's client user-agent at time of submission
     */
    public void recordConsentAtLeadSubmission(
            Long loanQuoteId,
            Long borrowerQuoteProfileId,
            QuoteRefinementRequestDto request,
            String ipAddress,
            String userAgent
    ) {
        String ip = ipAddress != null && !ipAddress.isBlank() ? ipAddress : "unknown";
        String lang = request.consentLanguage() != null ? request.consentLanguage() : "";

        record(loanQuoteId, borrowerQuoteProfileId, "TCPA",
                Boolean.TRUE.equals(request.tcpaConsent()) ? "GRANTED" : "REVOKED",
                lang, ip, userAgent);

        if (request.emailOptIn() != null) {
            record(loanQuoteId, borrowerQuoteProfileId, "EMAIL_OPT_IN",
                    Boolean.TRUE.equals(request.emailOptIn()) ? "GRANTED" : "REVOKED",
                    lang, ip, userAgent);
        }

        record(loanQuoteId, borrowerQuoteProfileId, "LEAD_SHARE",
                Boolean.TRUE.equals(request.leadShareConsent()) ? "GRANTED" : "REVOKED",
                lang, ip, userAgent);
    }

    /**
     * Returns the full consent history for a loan quote, ordered by event time ascending.
     * Suitable for compliance review and dispute resolution.
     */
    public List<ConsentAuditLog> getHistory(Long loanQuoteId) {
        return repository.findByLoanQuoteIdOrderByRecordedAtAsc(loanQuoteId);
    }

    private void record(Long loanQuoteId, Long borrowerQuoteProfileId,
                        String consentType, String consentAction,
                        String consentLanguage, String ipAddress, String userAgent) {
        repository.save(new ConsentAuditLog(
                loanQuoteId, borrowerQuoteProfileId,
                consentType, consentAction,
                consentLanguage, ipAddress, userAgent
        ));
    }
}
