package com.jaycodesx.mortgage.quote.dto;

import com.jaycodesx.mortgage.lead.dto.MortgageLeadResponseDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LoanQuoteResponseDtoTest {

    @Test
    void exposesRecordValues() {
        MortgageLeadResponseDto lead = new MortgageLeadResponseDto(9L, 4L, "NEW", "PUBLIC_QUOTE_FUNNEL");
        LoanQuoteResponseDto dto = new LoanQuoteResponseDto(
                4L,
                "user-abc",
                "session-123",
                "COMPLETED",
                false,
                "REFINED",
                "LEAD_READY",
                true,
                true,
                new BigDecimal("450000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("360000.00"),
                "60614",
                "CONVENTIONAL",
                "PRIMARY_RESIDENCE",
                30,
                new BigDecimal("6.0500"),
                new BigDecimal("6.2300"),
                new BigDecimal("2178.44"),
                new BigDecimal("99500.00"),
                "Prime",
                "Route to a loan officer",
                lead,
                null,
                null
        );

        assertThat(dto.id()).isEqualTo(4L);
        assertThat(dto.userId()).isEqualTo("user-abc");
        assertThat(dto.sessionId()).isEqualTo("session-123");
        assertThat(dto.processingStatus()).isEqualTo("COMPLETED");
        assertThat(dto.quoteStage()).isEqualTo("REFINED");
        assertThat(dto.quoteStatus()).isEqualTo("LEAD_READY");
        assertThat(dto.leadCaptured()).isTrue();
        assertThat(dto.borrowerProfileCaptured()).isTrue();
        assertThat(dto.financedAmount()).isEqualByComparingTo("360000.00");
        assertThat(dto.qualificationTier()).isEqualTo("Prime");
        assertThat(dto.lead()).isEqualTo(lead);
    }
}
