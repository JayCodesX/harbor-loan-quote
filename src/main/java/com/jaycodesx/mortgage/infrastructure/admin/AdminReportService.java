package com.jaycodesx.mortgage.infrastructure.admin;

import com.jaycodesx.mortgage.borrower.service.BorrowerService;
import com.jaycodesx.mortgage.quote.model.LoanQuote;
import com.jaycodesx.mortgage.quote.repository.LoanQuoteRepository;
import com.jaycodesx.mortgage.shared.model.Loan;
import com.jaycodesx.mortgage.shared.repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Service
public class AdminReportService {

    private final LoanRepository loanRepository;
    private final LoanQuoteRepository loanQuoteRepository;
    private final AdminMetricsClientService adminMetricsClientService;
    private final BorrowerService borrowerService;

    public AdminReportService(
            LoanRepository loanRepository,
            LoanQuoteRepository loanQuoteRepository,
            AdminMetricsClientService adminMetricsClientService,
            BorrowerService borrowerService
    ) {
        this.loanRepository = loanRepository;
        this.loanQuoteRepository = loanQuoteRepository;
        this.adminMetricsClientService = adminMetricsClientService;
        this.borrowerService = borrowerService;
    }

    public AdminReportResponseDto runReport(AdminReportQueryDto query) {
        String reportType = normalize(query.reportType());
        return switch (reportType) {
            case "PRODUCTS" -> productReport(query);
            case "BORROWERS" -> borrowerReport(query);
            case "LOANS" -> loanReport(query);
            default -> throw new IllegalArgumentException("Unsupported report type");
        };
    }

    private AdminReportResponseDto productReport(AdminReportQueryDto query) {
        Predicate<AdminPricingProductResponseDto> filter = product -> matchesSearch(query.search(), product.programCode(), product.productName())
                && matchesProgram(query.programCode(), product.programCode())
                && matchesActive(query.activeOnly(), product.active());

        List<Map<String, Object>> rows = adminMetricsClientService.fetchProducts().stream()
                .filter(filter)
                .map(product -> Map.<String, Object>of(
                        "id", product.id(),
                        "programCode", product.programCode(),
                        "productName", product.productName(),
                        "baseRate", product.baseRate(),
                        "active", product.active()
                ))
                .toList();

        return new AdminReportResponseDto("Pricing products", List.of("id", "programCode", "productName", "baseRate", "active"), rows, rows.size());
    }

    private AdminReportResponseDto borrowerReport(AdminReportQueryDto query) {
        Predicate<BorrowerAdminResponseDto> filter = borrower -> matchesSearch(query.search(), borrower.firstName(), borrower.lastName(), borrower.email())
                && matchesCreditScore(query.minCreditScore(), query.maxCreditScore(), borrower.creditScore());

        List<Map<String, Object>> rows = borrowerService.findAll().stream()
                .map(b -> new BorrowerAdminResponseDto(b.getId(), b.getFirstName(), b.getLastName(), b.getEmail(), b.getCreditScore()))
                .filter(filter)
                .map(borrower -> Map.<String, Object>of(
                        "id", borrower.id(),
                        "firstName", borrower.firstName(),
                        "lastName", borrower.lastName(),
                        "email", borrower.email(),
                        "creditScore", borrower.creditScore()
                ))
                .toList();

        return new AdminReportResponseDto("Borrowers", List.of("id", "firstName", "lastName", "email", "creditScore"), rows, rows.size());
    }

    private AdminReportResponseDto loanReport(AdminReportQueryDto query) {
        Predicate<Loan> loanFilter = loan -> matchesSearch(query.search(), String.valueOf(loan.getBorrowerId()), loan.getStatus())
                && matchesStatus(query.status(), loan.getStatus());
        Predicate<LoanQuote> quoteFilter = quote -> matchesSearch(query.search(), quote.getLoanProgram(), quote.getPropertyUse(), quote.getZipCode())
                && matchesStatus(query.status(), quote.getQuoteStatus())
                && matchesProgram(query.programCode(), quote.getLoanProgram());

        List<Map<String, Object>> loanRows = loanRepository.findAll().stream()
                .filter(loanFilter)
                .map(loan -> Map.<String, Object>of(
                        "recordType", "LOAN",
                        "id", loan.getId(),
                        "borrowerId", loan.getBorrowerId(),
                        "programOrStatus", loan.getStatus(),
                        "amount", loan.getLoanAmount(),
                        "rate", loan.getInterestRate()
                ))
                .toList();

        List<Map<String, Object>> quoteRows = loanQuoteRepository.findAll().stream()
                .filter(quoteFilter)
                .map(quote -> Map.<String, Object>of(
                        "recordType", "QUOTE",
                        "id", quote.getId(),
                        "borrowerId", quote.getSessionId() == null ? "--" : quote.getSessionId(),
                        "programOrStatus", quote.getLoanProgram() + " / " + quote.getQuoteStatus(),
                        "amount", quote.getFinancedAmount() == null ? quote.getHomePrice() : quote.getFinancedAmount(),
                        "rate", quote.getEstimatedRate() == null ? "--" : quote.getEstimatedRate()
                ))
                .toList();

        List<Map<String, Object>> rows = Stream.concat(loanRows.stream(), quoteRows.stream()).toList();
        return new AdminReportResponseDto("Loans and quotes", List.of("recordType", "id", "borrowerId", "programOrStatus", "amount", "rate"), rows, rows.size());
    }

    private boolean matchesSearch(String search, String... values) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String needle = normalize(search);
        for (String value : values) {
            if (value != null && normalize(value).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesProgram(String requestedProgram, String actualProgram) {
        return requestedProgram == null || requestedProgram.isBlank() || normalize(requestedProgram).equals(normalize(actualProgram));
    }

    private boolean matchesActive(Boolean activeOnly, boolean active) {
        return activeOnly == null || !activeOnly || active;
    }

    private boolean matchesStatus(String requestedStatus, String actualStatus) {
        return requestedStatus == null || requestedStatus.isBlank() || normalize(requestedStatus).equals(normalize(actualStatus));
    }

    private boolean matchesCreditScore(Integer minCreditScore, Integer maxCreditScore, Integer actualScore) {
        if (actualScore == null) {
            return false;
        }
        if (minCreditScore != null && actualScore < minCreditScore) {
            return false;
        }
        if (maxCreditScore != null && actualScore > maxCreditScore) {
            return false;
        }
        return true;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.US);
    }
}
