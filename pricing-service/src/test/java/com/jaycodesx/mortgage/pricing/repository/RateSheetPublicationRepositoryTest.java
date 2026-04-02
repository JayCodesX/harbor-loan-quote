package com.jaycodesx.mortgage.pricing.repository;

import com.jaycodesx.mortgage.pricing.model.RateSheetEntry;
import com.jaycodesx.mortgage.pricing.model.RateSheetPublication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class RateSheetPublicationRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private RateSheetPublicationRepository publicationRepository;

    @Autowired
    private RateSheetEntryRepository entryRepository;

    private static final LocalDateTime EFFECTIVE = LocalDateTime.of(2026, 4, 2, 9, 0);
    private static final LocalDateTime EXPIRES = LocalDateTime.of(2026, 4, 2, 23, 59);

    @Test
    void saveAndFindActiveSheet() {
        RateSheetPublication pub = new RateSheetPublication("FANNIE_MAE", EFFECTIVE, EXPIRES,
                "fannie-mae-morning-2026-04-02");

        RateSheetPublication saved = publicationRepository.save(pub);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getInvestorId()).isEqualTo("FANNIE_MAE");
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getImportedAt()).isNotNull();
        assertThat(saved.getEffectiveAt()).isEqualTo(EFFECTIVE);
        assertThat(saved.getExpiresAt()).isEqualTo(EXPIRES);
        assertThat(saved.getSource()).isEqualTo("fannie-mae-morning-2026-04-02");
    }

    @Test
    void findTopByInvestorIdAndStatusReturnsActiveSheet() {
        publicationRepository.save(new RateSheetPublication("FANNIE_MAE", EFFECTIVE, EXPIRES, "source-a"));

        Optional<RateSheetPublication> result = publicationRepository
                .findTopByInvestorIdAndStatusOrderByEffectiveAtDesc("FANNIE_MAE", "ACTIVE");

        assertThat(result).isPresent();
        assertThat(result.get().getInvestorId()).isEqualTo("FANNIE_MAE");
        assertThat(result.get().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void findTopByInvestorIdAndStatusReturnsEmptyForUnknownInvestor() {
        Optional<RateSheetPublication> result = publicationRepository
                .findTopByInvestorIdAndStatusOrderByEffectiveAtDesc("UNKNOWN_INVESTOR", "ACTIVE");

        assertThat(result).isEmpty();
    }

    @Test
    void findTopByInvestorIdAndStatusReturnsNewestActiveSheet() {
        LocalDateTime earlier = EFFECTIVE.minusHours(8);
        publicationRepository.save(new RateSheetPublication("FANNIE_MAE", earlier, EXPIRES, "morning-sheet"));
        publicationRepository.save(new RateSheetPublication("FANNIE_MAE", EFFECTIVE, EXPIRES, "afternoon-sheet"));

        Optional<RateSheetPublication> result = publicationRepository
                .findTopByInvestorIdAndStatusOrderByEffectiveAtDesc("FANNIE_MAE", "ACTIVE");

        assertThat(result).isPresent();
        assertThat(result.get().getSource()).isEqualTo("afternoon-sheet");
        assertThat(result.get().getEffectiveAt()).isEqualTo(EFFECTIVE);
    }

    @Test
    void statusTransitionToSupersededIsPersisted() {
        RateSheetPublication pub = publicationRepository.save(
                new RateSheetPublication("FANNIE_MAE", EFFECTIVE, EXPIRES, "source"));

        pub.setStatus("SUPERSEDED");
        publicationRepository.save(pub);

        RateSheetPublication reloaded = publicationRepository.findById(pub.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("SUPERSEDED");
    }

    @Test
    void findByInvestorIdAndStatusReturnsAllMatchingSheets() {
        RateSheetPublication first = publicationRepository.save(
                new RateSheetPublication("FANNIE_MAE", EFFECTIVE.minusHours(8), EXPIRES, "source-1"));
        first.setStatus("SUPERSEDED");
        publicationRepository.save(first);

        publicationRepository.save(
                new RateSheetPublication("FANNIE_MAE", EFFECTIVE, EXPIRES, "source-2"));

        List<RateSheetPublication> superseded = publicationRepository
                .findByInvestorIdAndStatusOrderByEffectiveAtDesc("FANNIE_MAE", "SUPERSEDED");
        List<RateSheetPublication> active = publicationRepository
                .findByInvestorIdAndStatusOrderByEffectiveAtDesc("FANNIE_MAE", "ACTIVE");

        assertThat(superseded).hasSize(1);
        assertThat(superseded.get(0).getSource()).isEqualTo("source-1");
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getSource()).isEqualTo("source-2");
    }

    @Test
    void sheetsFromDifferentInvestorsAreIsolated() {
        publicationRepository.save(new RateSheetPublication("FANNIE_MAE", EFFECTIVE, EXPIRES, "fannie-source"));
        publicationRepository.save(new RateSheetPublication("FREDDIE_MAC", EFFECTIVE, EXPIRES, "freddie-source"));

        Optional<RateSheetPublication> fannie = publicationRepository
                .findTopByInvestorIdAndStatusOrderByEffectiveAtDesc("FANNIE_MAE", "ACTIVE");
        Optional<RateSheetPublication> freddie = publicationRepository
                .findTopByInvestorIdAndStatusOrderByEffectiveAtDesc("FREDDIE_MAC", "ACTIVE");

        assertThat(fannie).isPresent();
        assertThat(fannie.get().getSource()).isEqualTo("fannie-source");
        assertThat(freddie).isPresent();
        assertThat(freddie.get().getSource()).isEqualTo("freddie-source");
    }

    @Test
    void saveAndQueryEntriesForSheet() {
        RateSheetPublication pub = publicationRepository.save(
                new RateSheetPublication("FANNIE_MAE", EFFECTIVE, EXPIRES, "source"));

        entryRepository.saveAll(List.of(
                new RateSheetEntry(pub.getId(), "CONVENTIONAL_30", new BigDecimal("7.0000"), new BigDecimal("0.0000")),
                new RateSheetEntry(pub.getId(), "CONVENTIONAL_30", new BigDecimal("6.7500"), new BigDecimal("-0.5000")),
                new RateSheetEntry(pub.getId(), "FHA_30", new BigDecimal("6.5000"), new BigDecimal("0.2500"))
        ));

        List<RateSheetEntry> entries = entryRepository
                .findByRateSheetIdOrderByProductTermIdAscRateAsc(pub.getId());

        assertThat(entries).hasSize(3);
        // Ordered by product_term_id ASC then rate ASC
        assertThat(entries.get(0).getProductTermId()).isEqualTo("CONVENTIONAL_30");
        assertThat(entries.get(0).getRate()).isEqualByComparingTo("6.7500");
        assertThat(entries.get(1).getProductTermId()).isEqualTo("CONVENTIONAL_30");
        assertThat(entries.get(1).getRate()).isEqualByComparingTo("7.0000");
        assertThat(entries.get(2).getProductTermId()).isEqualTo("FHA_30");
        assertThat(entries.get(2).getRate()).isEqualByComparingTo("6.5000");
    }

    @Test
    void entryFieldsArePersistedCorrectly() {
        RateSheetPublication pub = publicationRepository.save(
                new RateSheetPublication("FANNIE_MAE", EFFECTIVE, EXPIRES, "source"));
        RateSheetEntry entry = new RateSheetEntry(pub.getId(), "VA_15",
                new BigDecimal("6.2500"), new BigDecimal("-1.0000"));

        entryRepository.save(entry);

        List<RateSheetEntry> entries = entryRepository
                .findByRateSheetIdOrderByProductTermIdAscRateAsc(pub.getId());
        assertThat(entries).hasSize(1);
        RateSheetEntry loaded = entries.get(0);
        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded.getRateSheetId()).isEqualTo(pub.getId());
        assertThat(loaded.getProductTermId()).isEqualTo("VA_15");
        assertThat(loaded.getRate()).isEqualByComparingTo("6.2500");
        assertThat(loaded.getPrice()).isEqualByComparingTo("-1.0000");
    }
}
