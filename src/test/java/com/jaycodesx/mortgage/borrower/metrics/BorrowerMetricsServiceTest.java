package com.jaycodesx.mortgage.borrower.metrics;

import com.jaycodesx.mortgage.borrower.model.Borrower;
import com.jaycodesx.mortgage.borrower.repository.BorrowerRepository;
import com.jaycodesx.mortgage.infrastructure.admin.BorrowerMetricsResponseDto;
import com.jaycodesx.mortgage.infrastructure.admin.MetricSliceDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BorrowerMetricsServiceTest {

    @Test
    void buildsBorrowerMetricsSnapshot() {
        BorrowerRepository repository = mock(BorrowerRepository.class);
        when(repository.findAll()).thenReturn(List.of(
                new Borrower(1L, "Jay", "One", "jay.one@jaycodesx.dev", 760),
                new Borrower(2L, "Jay", "Two", "jay.two@jaycodesx.dev", 715),
                new Borrower(3L, "Jay", "Three", "jay.three@jaycodesx.dev", 640)
        ));

        BorrowerMetricsResponseDto snapshot = new BorrowerMetricsService(repository).getSnapshot();

        assertThat(snapshot.totalBorrowers()).isEqualTo(3);
        assertThat(snapshot.averageCreditScore()).isEqualTo(705.0);
        assertThat(snapshot.creditScoreBands()).extracting(MetricSliceDto::value).containsExactly(1L, 1L, 1L);
    }
}
