package com.jaycodesx.mortgage.borrower.metrics;

import com.jaycodesx.mortgage.borrower.model.Borrower;
import com.jaycodesx.mortgage.borrower.repository.BorrowerRepository;
import com.jaycodesx.mortgage.infrastructure.admin.BorrowerMetricsResponseDto;
import com.jaycodesx.mortgage.infrastructure.admin.MetricSliceDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BorrowerMetricsService {

    private final BorrowerRepository borrowerRepository;

    public BorrowerMetricsService(BorrowerRepository borrowerRepository) {
        this.borrowerRepository = borrowerRepository;
    }

    public BorrowerMetricsResponseDto getSnapshot() {
        List<Borrower> borrowers = borrowerRepository.findAll();
        long total = borrowers.size();
        double averageCreditScore = total == 0
                ? 0.0
                : BigDecimal.valueOf(
                        borrowers.stream()
                                .map(Borrower::getCreditScore)
                                .filter(score -> score != null)
                                .mapToInt(Integer::intValue)
                                .average()
                                .orElse(0.0)
                ).setScale(1, RoundingMode.HALF_UP).doubleValue();

        long prime = borrowers.stream().filter(b -> b.getCreditScore() != null && b.getCreditScore() >= 740).count();
        long nearPrime = borrowers.stream().filter(b -> b.getCreditScore() != null && b.getCreditScore() >= 680 && b.getCreditScore() < 740).count();
        long emerging = borrowers.stream().filter(b -> b.getCreditScore() != null && b.getCreditScore() < 680).count();

        return new BorrowerMetricsResponseDto(
                total,
                averageCreditScore,
                List.of(
                        new MetricSliceDto("Prime 740+", prime),
                        new MetricSliceDto("Near Prime 680-739", nearPrime),
                        new MetricSliceDto("Emerging <680", emerging)
                )
        );
    }
}
