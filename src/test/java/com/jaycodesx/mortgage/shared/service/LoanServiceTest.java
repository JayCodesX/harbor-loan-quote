package com.jaycodesx.mortgage.shared.service;

import com.jaycodesx.mortgage.shared.dto.AmortizationResponseDto;
import com.jaycodesx.mortgage.shared.dto.LoanCreateRequestDto;
import com.jaycodesx.mortgage.shared.dto.MortgagePaymentResponseDto;
import com.jaycodesx.mortgage.borrower.service.BorrowerService;
import com.jaycodesx.mortgage.shared.model.Loan;
import com.jaycodesx.mortgage.shared.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BorrowerService borrowerService;

    private final MortgageMathService mortgageMathService = new MortgageMathService();

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(loanRepository, borrowerService, mortgageMathService);
    }

    @Test
    void createLoanSavesWhenBorrowerExists() {
        Loan saved = new Loan(5L, 2L, new BigDecimal("325000.00"), new BigDecimal("6.1000"), 30, "PENDING");
        when(borrowerService.borrowerExists(2L)).thenReturn(true);
        when(loanRepository.save(any(Loan.class))).thenReturn(saved);

        Loan result = loanService.createLoan(new LoanCreateRequestDto(
                2L,
                new BigDecimal("325000.00"),
                new BigDecimal("6.1000"),
                30,
                "PENDING"
        ));

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getBorrowerId()).isEqualTo(2L);
    }

    @Test
    void createLoanRejectsUnknownBorrower() {
        when(borrowerService.borrowerExists(99L)).thenReturn(false);

        assertThatThrownBy(() -> loanService.createLoan(new LoanCreateRequestDto(
                99L,
                new BigDecimal("325000.00"),
                new BigDecimal("6.1000"),
                30,
                "PENDING"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Borrower not found");
    }

    @Test
    void findByIdDelegatesToRepository() {
        Loan loan = new Loan(3L, 2L, new BigDecimal("300000.00"), new BigDecimal("6.2500"), 30, "APPROVED");
        when(loanRepository.findById(3L)).thenReturn(Optional.of(loan));

        assertThat(loanService.findById(3L)).contains(loan);
    }

    @Test
    void buildsAmortizationSchedule() {
        Loan loan = new Loan(3L, 2L, new BigDecimal("300000.00"), new BigDecimal("6.2500"), 30, "APPROVED");
        when(loanRepository.findById(3L)).thenReturn(Optional.of(loan));

        AmortizationResponseDto response = loanService.buildAmortization(3L);

        assertThat(response.loanId()).isEqualTo(3L);
        assertThat(response.schedule()).hasSize(360);
    }

    @Test
    void buildAmortizationRejectsMissingLoan() {
        when(loanRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.buildAmortization(7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Loan not found");
    }

    @Test
    void calculatesMortgagePayment() {
        MortgagePaymentResponseDto dto = loanService.calculateMortgagePayment(
                new BigDecimal("450000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("6.5000"),
                30
        );

        assertThat(dto.financedPrincipal()).isEqualByComparingTo("360000.00");
        assertThat(dto.monthlyPayment()).isEqualByComparingTo("2275.44");
    }

    @Test
    void rejectsInvalidMortgagePaymentInput() {
        assertThatThrownBy(() -> loanService.calculateMortgagePayment(
                new BigDecimal("450000.00"),
                new BigDecimal("450000.00"),
                new BigDecimal("6.5000"),
                30
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("downPayment must be less than loanAmount");
    }
}
