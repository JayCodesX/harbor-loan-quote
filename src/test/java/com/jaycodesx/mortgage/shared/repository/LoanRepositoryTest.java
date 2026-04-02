package com.jaycodesx.mortgage.shared.repository;

import com.jaycodesx.mortgage.shared.model.Loan;
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
class LoanRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private LoanRepository loanRepository;

    @Test
    void savesAndFindsLoan() {
        Loan loan = new Loan(null, 101L, new BigDecimal("325000.00"), new BigDecimal("6.1250"), 30, "PENDING");

        Loan saved = loanRepository.save(loan);

        assertThat(saved.getId()).isNotNull();
        assertThat(loanRepository.findById(saved.getId())).contains(saved);
    }
}
