package com.jaycodesx.mortgage.pricing.repository;

import com.jaycodesx.mortgage.pricing.model.LlpaAdjustment;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class LlpaAdjustmentRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private LlpaAdjustmentRepository repository;

    private static final LocalDateTime EPOCH = LocalDateTime.of(2024, 1, 1, 0, 0);

    @Test
    void savePersistsAllFields() {
        LlpaAdjustment adj = adj("FANNIE_MAE", null, "CREDIT_SCORE",
                "{\"min\": 680, \"max\": 699}", "1.0000");

        LlpaAdjustment saved = repository.save(adj);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getInvestorId()).isEqualTo("FANNIE_MAE");
        assertThat(saved.getProductType()).isNull();
        assertThat(saved.getAdjustmentCategory()).isEqualTo("CREDIT_SCORE");
        assertThat(saved.getConditionJson()).isEqualTo("{\"min\": 680, \"max\": 699}");
        assertThat(saved.getPriceAdjustment()).isEqualByComparingTo("1.0000");
        assertThat(saved.getEffectiveAt()).isEqualTo(EPOCH);
        assertThat(saved.getExpiresAt()).isNull();
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void findByInvestorIdAndActiveTrueReturnsOnlyActiveRecords() {
        repository.save(adj("FANNIE_MAE", null, "CREDIT_SCORE", "{\"min\": 680, \"max\": 699}", "1.0000"));
        LlpaAdjustment inactive = adj("FANNIE_MAE", null, "LTV", "{\"min\": 80, \"max\": 85}", "0.7500");
        inactive.setActive(false);
        repository.save(inactive);

        List<LlpaAdjustment> results = repository.findByInvestorIdAndActiveTrue("FANNIE_MAE");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAdjustmentCategory()).isEqualTo("CREDIT_SCORE");
    }

    @Test
    void findByInvestorIdAndActiveTrueIsolatesByInvestor() {
        repository.save(adj("FANNIE_MAE", null, "OCCUPANCY", "{\"equals\": \"INVESTMENT\"}", "2.0000"));
        repository.save(adj("FREDDIE_MAC", null, "OCCUPANCY", "{\"equals\": \"INVESTMENT\"}", "1.7500"));

        List<LlpaAdjustment> fannie = repository.findByInvestorIdAndActiveTrue("FANNIE_MAE");
        List<LlpaAdjustment> freddie = repository.findByInvestorIdAndActiveTrue("FREDDIE_MAC");

        assertThat(fannie).hasSize(1);
        assertThat(fannie.get(0).getInvestorId()).isEqualTo("FANNIE_MAE");
        assertThat(freddie).hasSize(1);
        assertThat(freddie.get(0).getInvestorId()).isEqualTo("FREDDIE_MAC");
    }

    @Test
    void findByInvestorIdAndActiveTrueReturnsEmptyForUnknownInvestor() {
        assertThat(repository.findByInvestorIdAndActiveTrue("UNKNOWN")).isEmpty();
    }

    @Test
    void findByCategoryFiltersCorrectly() {
        repository.save(adj("FANNIE_MAE", null, "CREDIT_SCORE", "{\"min\": 680, \"max\": 699}", "1.0000"));
        repository.save(adj("FANNIE_MAE", null, "CREDIT_SCORE", "{\"min\": 700, \"max\": 719}", "0.7500"));
        repository.save(adj("FANNIE_MAE", null, "LTV", "{\"min\": 75, \"max\": 80}", "0.5000"));

        List<LlpaAdjustment> creditScoreRules = repository
                .findByInvestorIdAndAdjustmentCategoryAndActiveTrue("FANNIE_MAE", "CREDIT_SCORE");

        assertThat(creditScoreRules).hasSize(2);
        assertThat(creditScoreRules).allMatch(a -> a.getAdjustmentCategory().equals("CREDIT_SCORE"));
    }

    @Test
    void deactivatingAdjustmentExcludesItFromActiveQuery() {
        LlpaAdjustment adj = repository.save(adj("FANNIE_MAE", null, "LOAN_PURPOSE",
                "{\"equals\": \"CASH_OUT_REFI\"}", "0.3750"));

        assertThat(repository.findByInvestorIdAndActiveTrue("FANNIE_MAE")).hasSize(1);

        adj.setActive(false);
        repository.save(adj);

        assertThat(repository.findByInvestorIdAndActiveTrue("FANNIE_MAE")).isEmpty();
    }

    @Test
    void productTypedAdjustmentIsPersisted() {
        LlpaAdjustment adj = adj("FANNIE_MAE", "CONVENTIONAL", "PROPERTY_TYPE",
                "{\"equals\": \"CONDO\"}", "0.3750");

        LlpaAdjustment saved = repository.save(adj);

        assertThat(saved.getProductType()).isEqualTo("CONVENTIONAL");
    }

    private LlpaAdjustment adj(String investorId, String productType, String category,
                                String conditionJson, String priceAdj) {
        LlpaAdjustment a = new LlpaAdjustment();
        a.setInvestorId(investorId);
        a.setProductType(productType);
        a.setAdjustmentCategory(category);
        a.setConditionJson(conditionJson);
        a.setPriceAdjustment(new BigDecimal(priceAdj));
        a.setEffectiveAt(EPOCH);
        a.setActive(true);
        return a;
    }
}
