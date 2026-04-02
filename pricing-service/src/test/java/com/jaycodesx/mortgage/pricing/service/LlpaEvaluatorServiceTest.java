package com.jaycodesx.mortgage.pricing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaycodesx.mortgage.pricing.model.LlpaAdjustment;
import com.jaycodesx.mortgage.pricing.repository.LlpaAdjustmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlpaEvaluatorServiceTest {

    @Mock
    private LlpaAdjustmentRepository repository;

    private LlpaEvaluatorService evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new LlpaEvaluatorService(repository, new ObjectMapper());
    }

    @Test
    void sumsAllMatchingAdjustments() {
        LlpaScenario scenario = scenario(720, new BigDecimal("78.00"), "INVESTMENT", "PURCHASE", "SINGLE_FAMILY");
        when(repository.findByInvestorIdAndActiveTrue("FANNIE_MAE")).thenReturn(List.of(
                adj("CREDIT_SCORE", "{\"min\": 720, \"max\": 739}", "0.5000"),  // matches 720
                adj("LTV",          "{\"min\": 75.01, \"max\": 80}", "0.5000"), // matches 78.00
                adj("OCCUPANCY",    "{\"equals\": \"INVESTMENT\"}", "2.0000"),  // matches
                adj("LOAN_PURPOSE", "{\"equals\": \"CASH_OUT_REFI\"}", "0.3750") // no match
        ));

        BigDecimal result = evaluator.evaluate(scenario);

        assertThat(result).isEqualByComparingTo("3.0000"); // 0.50 + 0.50 + 2.00
    }

    @Test
    void returnsZeroWhenNoAdjustmentsMatch() {
        LlpaScenario scenario = scenario(780, new BigDecimal("55.00"), "PRIMARY_RESIDENCE", "PURCHASE", "SINGLE_FAMILY");
        when(repository.findByInvestorIdAndActiveTrue("FANNIE_MAE")).thenReturn(List.of(
                adj("CREDIT_SCORE", "{\"min\": 640, \"max\": 679}", "1.2500"), // no match (780 >= 680)
                adj("LTV",          "{\"min\": 80.01, \"max\": 85}", "0.7500") // no match (55 < 80.01)
        ));

        BigDecimal result = evaluator.evaluate(scenario);

        assertThat(result).isEqualByComparingTo("0.0000");
    }

    @Test
    void returnsZeroWhenNoAdjustmentsExist() {
        when(repository.findByInvestorIdAndActiveTrue("FANNIE_MAE")).thenReturn(List.of());

        BigDecimal result = evaluator.evaluate(
                scenario(760, new BigDecimal("80.00"), "PRIMARY_RESIDENCE", "PURCHASE", "SINGLE_FAMILY"));

        assertThat(result).isEqualByComparingTo("0.0000");
    }

    @Test
    void oneSidedRangeMinOnlyMatches() {
        LlpaScenario scenario = scenario(null, new BigDecimal("96.00"), "PRIMARY_RESIDENCE", "PURCHASE", "SINGLE_FAMILY");
        when(repository.findByInvestorIdAndActiveTrue("FANNIE_MAE")).thenReturn(List.of(
                adj("LTV", "{\"min\": 95.01}", "1.5000") // matches 96.00 (>= 95.01)
        ));

        assertThat(evaluator.evaluate(scenario)).isEqualByComparingTo("1.5000");
    }

    @Test
    void oneSidedRangeMaxOnlyMatches() {
        LlpaScenario scenario = scenario(null, new BigDecimal("58.00"), "PRIMARY_RESIDENCE", "PURCHASE", "SINGLE_FAMILY");
        when(repository.findByInvestorIdAndActiveTrue("FANNIE_MAE")).thenReturn(List.of(
                adj("LTV", "{\"max\": 60}", "-0.2500") // matches 58.00 (<= 60)
        ));

        assertThat(evaluator.evaluate(scenario)).isEqualByComparingTo("-0.2500");
    }

    @Test
    void nullScenarioValueSkipsAdjustment() {
        LlpaScenario scenario = new LlpaScenario("FANNIE_MAE", "CONVENTIONAL",
                null, null, null, null, null); // all null
        when(repository.findByInvestorIdAndActiveTrue("FANNIE_MAE")).thenReturn(List.of(
                adj("CREDIT_SCORE", "{\"min\": 680, \"max\": 699}", "1.0000"),
                adj("LTV",          "{\"min\": 75.01, \"max\": 80}", "0.5000"),
                adj("OCCUPANCY",    "{\"equals\": \"INVESTMENT\"}", "2.0000")
        ));

        assertThat(evaluator.evaluate(scenario)).isEqualByComparingTo("0.0000");
    }

    @Test
    void productTypedAdjustmentOnlyAppliesWhenProductMatches() {
        LlpaScenario conventional = scenario(740, new BigDecimal("75.00"), "PRIMARY_RESIDENCE", "PURCHASE", "SINGLE_FAMILY");
        LlpaScenario fha = new LlpaScenario("FANNIE_MAE", "FHA",
                740, new BigDecimal("75.00"), "PRIMARY_RESIDENCE", "PURCHASE", "SINGLE_FAMILY");

        LlpaAdjustment conventionalOnly = adj("CREDIT_SCORE", "{\"min\": 740, \"max\": 759}", "0.2500");
        conventionalOnly.setProductType("CONVENTIONAL");

        when(repository.findByInvestorIdAndActiveTrue("FANNIE_MAE")).thenReturn(List.of(conventionalOnly));

        assertThat(evaluator.evaluate(conventional)).isEqualByComparingTo("0.2500");
        assertThat(evaluator.evaluate(fha)).isEqualByComparingTo("0.0000");
    }

    @Test
    void nullProductTypeOnAdjustmentAppliesToAllProducts() {
        LlpaScenario conventional = scenario(700, new BigDecimal("80.00"), "PRIMARY_RESIDENCE", "PURCHASE", "SINGLE_FAMILY");
        LlpaScenario fha = new LlpaScenario("FANNIE_MAE", "FHA",
                700, new BigDecimal("80.00"), "PRIMARY_RESIDENCE", "PURCHASE", "SINGLE_FAMILY");

        LlpaAdjustment universalAdj = adj("CREDIT_SCORE", "{\"min\": 700, \"max\": 719}", "0.7500");
        // productType is null by default

        when(repository.findByInvestorIdAndActiveTrue("FANNIE_MAE")).thenReturn(List.of(universalAdj));

        assertThat(evaluator.evaluate(conventional)).isEqualByComparingTo("0.7500");
        assertThat(evaluator.evaluate(fha)).isEqualByComparingTo("0.7500");
    }

    @Test
    void malformedConditionJsonIsSkippedGracefully() {
        LlpaScenario scenario = scenario(720, new BigDecimal("78.00"), "PRIMARY_RESIDENCE", "PURCHASE", "SINGLE_FAMILY");
        when(repository.findByInvestorIdAndActiveTrue("FANNIE_MAE")).thenReturn(List.of(
                adj("CREDIT_SCORE", "not-valid-json", "0.5000"),   // skipped — bad JSON
                adj("LTV", "{\"min\": 75.01, \"max\": 80}", "0.5000") // matches
        ));

        assertThat(evaluator.evaluate(scenario)).isEqualByComparingTo("0.5000");
    }

    @Test
    void unknownCategoryIsSkippedGracefully() {
        LlpaScenario scenario = scenario(720, new BigDecimal("78.00"), "PRIMARY_RESIDENCE", "PURCHASE", "SINGLE_FAMILY");
        when(repository.findByInvestorIdAndActiveTrue("FANNIE_MAE")).thenReturn(List.of(
                adj("UNKNOWN_CATEGORY", "{\"equals\": \"something\"}", "1.0000"), // skipped
                adj("OCCUPANCY", "{\"equals\": \"PRIMARY_RESIDENCE\"}", "0.0000") // matches, zero contribution
        ));

        assertThat(evaluator.evaluate(scenario)).isEqualByComparingTo("0.0000");
    }

    @Test
    void equalityMatchIsCaseInsensitive() {
        LlpaScenario scenario = new LlpaScenario("FANNIE_MAE", "CONVENTIONAL",
                null, null, "investment", null, null); // lowercase
        when(repository.findByInvestorIdAndActiveTrue("FANNIE_MAE")).thenReturn(List.of(
                adj("OCCUPANCY", "{\"equals\": \"INVESTMENT\"}", "2.0000") // uppercase in JSON
        ));

        assertThat(evaluator.evaluate(scenario)).isEqualByComparingTo("2.0000");
    }

    // --- helpers ---

    private LlpaScenario scenario(Integer creditScore, BigDecimal ltv,
                                   String occupancy, String loanPurpose, String propertyType) {
        return new LlpaScenario("FANNIE_MAE", "CONVENTIONAL",
                creditScore, ltv, occupancy, loanPurpose, propertyType);
    }

    private LlpaAdjustment adj(String category, String conditionJson, String priceAdj) {
        LlpaAdjustment a = new LlpaAdjustment();
        a.setInvestorId("FANNIE_MAE");
        a.setAdjustmentCategory(category);
        a.setConditionJson(conditionJson);
        a.setPriceAdjustment(new BigDecimal(priceAdj));
        a.setEffectiveAt(LocalDateTime.of(2024, 1, 1, 0, 0));
        a.setActive(true);
        return a;
    }
}
