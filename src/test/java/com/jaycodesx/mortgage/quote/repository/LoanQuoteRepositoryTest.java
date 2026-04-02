package com.jaycodesx.mortgage.quote.repository;

import com.jaycodesx.mortgage.quote.model.LoanQuote;
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
class LoanQuoteRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private LoanQuoteRepository loanQuoteRepository;

    @Test
    void savesAndFindsLoanQuote() {
        LoanQuote quote = new LoanQuote();
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

        LoanQuote saved = loanQuoteRepository.save(quote);

        assertThat(saved.getId()).isNotNull();
        assertThat(loanQuoteRepository.findById(saved.getId())).contains(saved);
    }
}
