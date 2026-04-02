package com.jaycodesx.mortgage.quote.repository;

import com.jaycodesx.mortgage.quote.model.BorrowerQuoteProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class BorrowerQuoteProfileRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private BorrowerQuoteProfileRepository borrowerQuoteProfileRepository;

    @Test
    void findsByLoanQuoteId() {
        BorrowerQuoteProfile profile = new BorrowerQuoteProfile();
        profile.setLoanQuoteId(15L);
        profile.setFirstName("Jay");
        profile.setLastName("Lane");
        profile.setEmail("jay.profile@jaycodesx.dev");
        profile.setPhone("555-111-0101");
        profile.setAnnualIncome(new BigDecimal("120000.00"));
        profile.setMonthlyDebts(new BigDecimal("800.00"));
        profile.setCreditScore(740);
        profile.setCashReserves(new BigDecimal("25000.00"));
        profile.setFirstTimeBuyer(true);
        profile.setVaEligible(false);
        borrowerQuoteProfileRepository.save(profile);

        assertThat(borrowerQuoteProfileRepository.findByLoanQuoteId(15L)).isPresent();
    }
}
