package com.jaycodesx.mortgage.lead.repository;

import com.jaycodesx.mortgage.lead.model.MortgageLead;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class MortgageLeadRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private MortgageLeadRepository mortgageLeadRepository;

    @Test
    void findsByLoanQuoteId() {
        MortgageLead lead = new MortgageLead();
        lead.setLoanQuoteId(33L);
        lead.setBorrowerQuoteProfileId(6L);
        lead.setLeadStatus("NEW");
        lead.setLeadSource("PUBLIC_QUOTE_FUNNEL");
        mortgageLeadRepository.save(lead);

        assertThat(mortgageLeadRepository.findByLoanQuoteId(33L)).isPresent();
    }
}
