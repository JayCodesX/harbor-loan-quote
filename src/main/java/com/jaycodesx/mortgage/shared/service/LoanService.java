package com.jaycodesx.mortgage.shared.service;

import com.jaycodesx.mortgage.shared.dto.AmortizationCalculationResponseDto;
import com.jaycodesx.mortgage.shared.dto.AmortizationEntryDto;
import com.jaycodesx.mortgage.shared.dto.AmortizationResponseDto;
import com.jaycodesx.mortgage.shared.dto.LoanCreateRequestDto;
import com.jaycodesx.mortgage.shared.dto.MortgagePaymentResponseDto;
import com.jaycodesx.mortgage.borrower.service.BorrowerService;
import com.jaycodesx.mortgage.shared.model.Loan;
import com.jaycodesx.mortgage.shared.repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BorrowerService borrowerService;
    private final MortgageMathService mortgageMathService;

    public LoanService(LoanRepository loanRepository, BorrowerService borrowerService, MortgageMathService mortgageMathService) {
        this.loanRepository = loanRepository;
        this.borrowerService = borrowerService;
        this.mortgageMathService = mortgageMathService;
    }

    public Loan createLoan(LoanCreateRequestDto request) {
        if (!borrowerService.borrowerExists(request.borrowerId())) {
            throw new IllegalArgumentException("Borrower not found for id: " + request.borrowerId());
        }

        Loan loan = new Loan();
        loan.setBorrowerId(request.borrowerId());
        loan.setLoanAmount(request.loanAmount());
        loan.setInterestRate(request.interestRate());
        loan.setTermYears(request.termYears());
        loan.setStatus(request.status());

        return loanRepository.save(loan);
    }

    public Optional<Loan> findById(Long id) {
        return loanRepository.findById(id);
    }

    public AmortizationResponseDto buildAmortization(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found for id: " + loanId));

        int totalPayments = loan.getTermYears() * 12;
        BigDecimal monthlyRate = mortgageMathService.toMonthlyRate(loan.getInterestRate());

        BigDecimal monthlyPayment = mortgageMathService.calculateMonthlyPaymentForMonthlyRate(
                loan.getLoanAmount(),
                monthlyRate,
                totalPayments
        );
        BigDecimal remainingBalance = loan.getLoanAmount();

        List<AmortizationEntryDto> schedule = new ArrayList<>();

        for (int paymentNumber = 1; paymentNumber <= totalPayments; paymentNumber++) {
            BigDecimal interestPaid = remainingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPaid = monthlyPayment.subtract(interestPaid).setScale(2, RoundingMode.HALF_UP);
            remainingBalance = remainingBalance.subtract(principalPaid).setScale(2, RoundingMode.HALF_UP);

            if (paymentNumber == totalPayments && remainingBalance.compareTo(BigDecimal.ZERO) != 0) {
                principalPaid = principalPaid.add(remainingBalance);
                remainingBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            schedule.add(new AmortizationEntryDto(
                    paymentNumber,
                    monthlyPayment,
                    principalPaid,
                    interestPaid,
                    remainingBalance.max(BigDecimal.ZERO)
            ));
        }

        return new AmortizationResponseDto(
                loan.getId(),
                loan.getBorrowerId(),
                loan.getLoanAmount(),
                loan.getInterestRate(),
                loan.getTermYears(),
                monthlyPayment,
                schedule
        );
    }

    public AmortizationCalculationResponseDto calculateAmortizationPayment(
            BigDecimal principal,
            BigDecimal annualInterestRate,
            Integer termYears
    ) {
        validateCalculatorInputs(principal, annualInterestRate, termYears);

        int numberOfPayments = termYears * 12;
        BigDecimal monthlyInterestRate = mortgageMathService.toMonthlyRate(annualInterestRate);
        BigDecimal monthlyPayment = mortgageMathService.calculateMonthlyPaymentForMonthlyRate(
                principal,
                monthlyInterestRate,
                numberOfPayments
        );

        return new AmortizationCalculationResponseDto(
                principal.setScale(2, RoundingMode.HALF_UP),
                monthlyInterestRate.setScale(8, RoundingMode.HALF_UP),
                numberOfPayments,
                monthlyPayment
        );
    }

    public MortgagePaymentResponseDto calculateMortgagePayment(
            BigDecimal loanAmount,
            BigDecimal downPayment,
            BigDecimal annualInterestRate,
            Integer termYears
    ) {
        validateMortgageInputs(loanAmount, downPayment, annualInterestRate, termYears);

        BigDecimal financedPrincipal = loanAmount.subtract(downPayment);
        int numberOfPayments = termYears * 12;
        BigDecimal monthlyInterestRate = mortgageMathService.toMonthlyRate(annualInterestRate);
        BigDecimal monthlyPayment = mortgageMathService.calculateMonthlyPaymentForMonthlyRate(
                financedPrincipal,
                monthlyInterestRate,
                numberOfPayments
        );

        return new MortgagePaymentResponseDto(
                loanAmount.setScale(2, RoundingMode.HALF_UP),
                downPayment.setScale(2, RoundingMode.HALF_UP),
                financedPrincipal.setScale(2, RoundingMode.HALF_UP),
                annualInterestRate.setScale(4, RoundingMode.HALF_UP),
                termYears,
                numberOfPayments,
                monthlyInterestRate.setScale(8, RoundingMode.HALF_UP),
                monthlyPayment
        );
    }

    private void validateCalculatorInputs(BigDecimal principal, BigDecimal annualInterestRate, Integer termYears) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("principal must be greater than 0");
        }
        if (annualInterestRate == null || annualInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("annualInterestRate must be 0 or greater");
        }
        if (termYears == null || termYears <= 0) {
            throw new IllegalArgumentException("termYears must be greater than 0");
        }
    }

    private void validateMortgageInputs(
            BigDecimal loanAmount,
            BigDecimal downPayment,
            BigDecimal annualInterestRate,
            Integer termYears
    ) {
        if (loanAmount == null || loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("loanAmount must be greater than 0");
        }
        if (downPayment == null || downPayment.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("downPayment must be 0 or greater");
        }
        if (downPayment.compareTo(loanAmount) >= 0) {
            throw new IllegalArgumentException("downPayment must be less than loanAmount");
        }
        if (annualInterestRate == null || annualInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("annualInterestRate must be 0 or greater");
        }
        if (termYears == null || termYears <= 0) {
            throw new IllegalArgumentException("termYears must be greater than 0");
        }
    }

}
