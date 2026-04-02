package com.jaycodesx.mortgage.consent.service;

import com.jaycodesx.mortgage.consent.model.ConsentAuditLog;
import com.jaycodesx.mortgage.consent.repository.ConsentAuditLogRepository;
import com.jaycodesx.mortgage.quote.dto.QuoteRefinementRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentAuditLogServiceTest {

    @Mock
    private ConsentAuditLogRepository repository;

    @InjectMocks
    private ConsentAuditLogService consentAuditLogService;

    private static final String CONSENT_LANGUAGE = "I agree to be contacted by Harbor Mortgage and its partners.";

    @Test
    void recordsThreeEntriesWhenEmailOptInIsPresent() {
        QuoteRefinementRequestDto request = buildRequest(true, true, true, true);
        when(repository.save(any(ConsentAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        consentAuditLogService.recordConsentAtLeadSubmission(1L, 2L, request, "10.0.0.1", "TestAgent/1.0");

        ArgumentCaptor<ConsentAuditLog> captor = ArgumentCaptor.forClass(ConsentAuditLog.class);
        verify(repository, times(3)).save(captor.capture());

        List<ConsentAuditLog> saved = captor.getAllValues();
        assertThat(saved.get(0).getConsentType()).isEqualTo("TCPA");
        assertThat(saved.get(0).getConsentAction()).isEqualTo("GRANTED");
        assertThat(saved.get(0).getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(saved.get(0).getUserAgent()).isEqualTo("TestAgent/1.0");
        assertThat(saved.get(0).getConsentLanguage()).isEqualTo(CONSENT_LANGUAGE);

        assertThat(saved.get(1).getConsentType()).isEqualTo("EMAIL_OPT_IN");
        assertThat(saved.get(1).getConsentAction()).isEqualTo("GRANTED");

        assertThat(saved.get(2).getConsentType()).isEqualTo("LEAD_SHARE");
        assertThat(saved.get(2).getConsentAction()).isEqualTo("GRANTED");
    }

    @Test
    void recordsTwoEntriesWhenEmailOptInIsNull() {
        QuoteRefinementRequestDto request = buildRequest(true, null, true, true);
        when(repository.save(any(ConsentAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        consentAuditLogService.recordConsentAtLeadSubmission(1L, 2L, request, "10.0.0.1", null);

        ArgumentCaptor<ConsentAuditLog> captor = ArgumentCaptor.forClass(ConsentAuditLog.class);
        verify(repository, times(2)).save(captor.capture());

        List<ConsentAuditLog> saved = captor.getAllValues();
        assertThat(saved.get(0).getConsentType()).isEqualTo("TCPA");
        assertThat(saved.get(1).getConsentType()).isEqualTo("LEAD_SHARE");
    }

    @Test
    void recordsRevokedActionsWhenConsentIsFalse() {
        QuoteRefinementRequestDto request = buildRequest(false, false, false, true);
        when(repository.save(any(ConsentAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        consentAuditLogService.recordConsentAtLeadSubmission(1L, 2L, request, null, null);

        ArgumentCaptor<ConsentAuditLog> captor = ArgumentCaptor.forClass(ConsentAuditLog.class);
        verify(repository, times(3)).save(captor.capture());

        List<ConsentAuditLog> saved = captor.getAllValues();
        assertThat(saved.get(0).getConsentType()).isEqualTo("TCPA");
        assertThat(saved.get(0).getConsentAction()).isEqualTo("REVOKED");
        assertThat(saved.get(0).getIpAddress()).isEqualTo("unknown");

        assertThat(saved.get(1).getConsentType()).isEqualTo("EMAIL_OPT_IN");
        assertThat(saved.get(1).getConsentAction()).isEqualTo("REVOKED");

        assertThat(saved.get(2).getConsentType()).isEqualTo("LEAD_SHARE");
        assertThat(saved.get(2).getConsentAction()).isEqualTo("REVOKED");
    }

    @Test
    void getHistoryDelegatesToRepository() {
        ConsentAuditLog entry = new ConsentAuditLog(5L, 3L, "TCPA", "GRANTED", "consent", "1.2.3.4", null);
        when(repository.findByLoanQuoteIdOrderByRecordedAtAsc(5L)).thenReturn(List.of(entry));

        List<ConsentAuditLog> history = consentAuditLogService.getHistory(5L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getConsentType()).isEqualTo("TCPA");
    }

    private QuoteRefinementRequestDto buildRequest(boolean tcpa, Boolean emailOptIn, boolean leadShare, boolean vaEligible) {
        return new QuoteRefinementRequestDto(
                "Jay", "Lane", "jay@jaycodesx.dev", "555-111-0101",
                new BigDecimal("125000.00"), new BigDecimal("900.00"), 735,
                new BigDecimal("24000.00"), true, vaEligible,
                tcpa, emailOptIn, leadShare, CONSENT_LANGUAGE
        );
    }
}
