package com.jaycodesx.mortgage.pricing.service;

import com.jaycodesx.mortgage.pricing.dto.RateSheetEntryRequest;
import com.jaycodesx.mortgage.pricing.model.RateSheetEntry;
import com.jaycodesx.mortgage.pricing.model.RateSheetPublication;
import com.jaycodesx.mortgage.pricing.repository.RateSheetEntryRepository;
import com.jaycodesx.mortgage.pricing.repository.RateSheetPublicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateSheetServiceTest {

    @Mock
    private RateSheetPublicationRepository publicationRepository;

    @Mock
    private RateSheetEntryRepository entryRepository;

    @InjectMocks
    private RateSheetService rateSheetService;

    private static final LocalDateTime EFFECTIVE_AT = LocalDateTime.of(2026, 4, 2, 9, 0);
    private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 4, 2, 23, 59);

    @Test
    void publishCreatesNewPublicationAndEntries() {
        when(publicationRepository.findByInvestorIdAndStatusOrderByEffectiveAtDesc("FANNIE_MAE", "ACTIVE"))
                .thenReturn(List.of());
        RateSheetPublication saved = new RateSheetPublication("FANNIE_MAE", EFFECTIVE_AT, EXPIRES_AT,
                "fannie-mae-morning-2026-04-02");
        when(publicationRepository.save(any(RateSheetPublication.class))).thenReturn(saved);

        List<RateSheetEntryRequest> entries = List.of(
                new RateSheetEntryRequest("CONVENTIONAL_30", new BigDecimal("6.7500"), new BigDecimal("-0.5000")),
                new RateSheetEntryRequest("CONVENTIONAL_30", new BigDecimal("7.0000"), new BigDecimal("0.0000")),
                new RateSheetEntryRequest("FHA_30", new BigDecimal("6.5000"), new BigDecimal("0.2500"))
        );

        RateSheetPublication result = rateSheetService.publish(
                "FANNIE_MAE", EFFECTIVE_AT, EXPIRES_AT, "fannie-mae-morning-2026-04-02", entries);

        assertThat(result).isEqualTo(saved);
        verify(publicationRepository).save(any(RateSheetPublication.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RateSheetEntry>> entryCaptor = ArgumentCaptor.forClass(List.class);
        verify(entryRepository).saveAll(entryCaptor.capture());
        List<RateSheetEntry> savedEntries = entryCaptor.getValue();
        assertThat(savedEntries).hasSize(3);
        assertThat(savedEntries.get(0).getProductTermId()).isEqualTo("CONVENTIONAL_30");
        assertThat(savedEntries.get(0).getRate()).isEqualByComparingTo("6.7500");
        assertThat(savedEntries.get(0).getPrice()).isEqualByComparingTo("-0.5000");
        assertThat(savedEntries.get(2).getProductTermId()).isEqualTo("FHA_30");
    }

    @Test
    void publishSupersedespreviouslyActiveSheet() {
        RateSheetPublication existing = new RateSheetPublication("FANNIE_MAE", EFFECTIVE_AT.minusHours(8),
                EXPIRES_AT.minusHours(8), "fannie-mae-prior-sheet");
        when(publicationRepository.findByInvestorIdAndStatusOrderByEffectiveAtDesc("FANNIE_MAE", "ACTIVE"))
                .thenReturn(List.of(existing));
        when(publicationRepository.save(any(RateSheetPublication.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        rateSheetService.publish("FANNIE_MAE", EFFECTIVE_AT, EXPIRES_AT,
                "fannie-mae-afternoon-2026-04-02", List.of());

        assertThat(existing.getStatus()).isEqualTo("SUPERSEDED");
        verify(publicationRepository).saveAll(anyList());
    }

    @Test
    void publishDoesNotSupersedeSheetsFromOtherInvestors() {
        when(publicationRepository.findByInvestorIdAndStatusOrderByEffectiveAtDesc("FANNIE_MAE", "ACTIVE"))
                .thenReturn(List.of());
        when(publicationRepository.save(any(RateSheetPublication.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        rateSheetService.publish("FANNIE_MAE", EFFECTIVE_AT, EXPIRES_AT, "source", List.of());

        verify(publicationRepository).findByInvestorIdAndStatusOrderByEffectiveAtDesc(eq("FANNIE_MAE"), eq("ACTIVE"));
        verify(publicationRepository, never()).findByInvestorIdAndStatusOrderByEffectiveAtDesc(
                eq("FREDDIE_MAC"), any());
    }

    @Test
    void findActiveReturnsCurrentSheet() {
        RateSheetPublication active = new RateSheetPublication("FANNIE_MAE", EFFECTIVE_AT, EXPIRES_AT, "source");
        when(publicationRepository.findTopByInvestorIdAndStatusOrderByEffectiveAtDesc("FANNIE_MAE", "ACTIVE"))
                .thenReturn(Optional.of(active));

        Optional<RateSheetPublication> result = rateSheetService.findActive("FANNIE_MAE");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(active);
    }

    @Test
    void findActiveReturnsEmptyWhenNoActiveSheetExists() {
        when(publicationRepository.findTopByInvestorIdAndStatusOrderByEffectiveAtDesc("FANNIE_MAE", "ACTIVE"))
                .thenReturn(Optional.empty());

        assertThat(rateSheetService.findActive("FANNIE_MAE")).isEmpty();
    }

    @Test
    void findByIdReturnsSheetForAudit() {
        RateSheetPublication superseded = new RateSheetPublication("FANNIE_MAE",
                EFFECTIVE_AT.minusDays(1), EXPIRES_AT.minusDays(1), "prior-source");
        when(publicationRepository.findById(42L)).thenReturn(Optional.of(superseded));

        Optional<RateSheetPublication> result = rateSheetService.findById(42L);

        assertThat(result).isPresent();
    }

    @Test
    void findEntriesForSheetDelegatesToRepository() {
        RateSheetEntry entry = new RateSheetEntry(10L, "CONVENTIONAL_30",
                new BigDecimal("7.0000"), new BigDecimal("0.0000"));
        when(entryRepository.findByRateSheetIdOrderByProductTermIdAscRateAsc(10L))
                .thenReturn(List.of(entry));

        List<RateSheetEntry> results = rateSheetService.findEntriesForSheet(10L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProductTermId()).isEqualTo("CONVENTIONAL_30");
        assertThat(results.get(0).getRate()).isEqualByComparingTo("7.0000");
    }
}
