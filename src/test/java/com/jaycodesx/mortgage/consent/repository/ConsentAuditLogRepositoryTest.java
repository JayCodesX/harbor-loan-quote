package com.jaycodesx.mortgage.consent.repository;

import com.jaycodesx.mortgage.consent.model.ConsentAuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ConsentAuditLogRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private ConsentAuditLogRepository repository;

    @Test
    void savePersistsEntry() {
        ConsentAuditLog entry = new ConsentAuditLog(
                1L, 2L, "TCPA", "GRANTED",
                "I agree to be contacted.", "192.168.1.1", "Mozilla/5.0"
        );

        ConsentAuditLog saved = repository.save(entry);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLoanQuoteId()).isEqualTo(1L);
        assertThat(saved.getBorrowerQuoteProfileId()).isEqualTo(2L);
        assertThat(saved.getConsentType()).isEqualTo("TCPA");
        assertThat(saved.getConsentAction()).isEqualTo("GRANTED");
        assertThat(saved.getConsentLanguage()).isEqualTo("I agree to be contacted.");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(saved.getRecordedAt()).isNotNull();
        assertThat(saved.getSchemaVersion()).isEqualTo(1);
    }

    @Test
    void findByLoanQuoteIdReturnsOrderedResults() {
        repository.save(new ConsentAuditLog(10L, 5L, "TCPA", "GRANTED", "consent text", "1.2.3.4", "agent"));
        repository.save(new ConsentAuditLog(10L, 5L, "EMAIL_OPT_IN", "GRANTED", "consent text", "1.2.3.4", "agent"));
        repository.save(new ConsentAuditLog(10L, 5L, "LEAD_SHARE", "REVOKED", "consent text", "1.2.3.4", "agent"));

        List<ConsentAuditLog> results = repository.findByLoanQuoteIdOrderByRecordedAtAsc(10L);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getConsentType()).isEqualTo("TCPA");
        assertThat(results.get(1).getConsentType()).isEqualTo("EMAIL_OPT_IN");
        assertThat(results.get(2).getConsentType()).isEqualTo("LEAD_SHARE");
        assertThat(results.get(2).getConsentAction()).isEqualTo("REVOKED");
    }

    @Test
    void findByLoanQuoteIdReturnsEmptyForUnknownId() {
        List<ConsentAuditLog> results = repository.findByLoanQuoteIdOrderByRecordedAtAsc(999L);

        assertThat(results).isEmpty();
    }

    @Test
    void findByBorrowerQuoteProfileIdReturnsMatchingEntries() {
        repository.save(new ConsentAuditLog(20L, 7L, "TCPA", "GRANTED", "consent", "10.0.0.1", null));
        repository.save(new ConsentAuditLog(21L, 7L, "LEAD_SHARE", "GRANTED", "consent", "10.0.0.1", null));
        repository.save(new ConsentAuditLog(22L, 8L, "TCPA", "GRANTED", "consent", "10.0.0.2", null));

        List<ConsentAuditLog> results = repository.findByBorrowerQuoteProfileIdOrderByRecordedAtAsc(7L);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(e -> e.getBorrowerQuoteProfileId().equals(7L));
    }
}
