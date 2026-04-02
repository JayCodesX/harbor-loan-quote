package com.jaycodesx.mortgage.pricing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaycodesx.mortgage.pricing.model.LlpaAdjustment;
import com.jaycodesx.mortgage.pricing.repository.LlpaAdjustmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Evaluates LLPA (Loan Level Price Adjustment) rules against a loan scenario per ADR-0020.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Load all active LLPA adjustments for the investor from the database.</li>
 *   <li>Filter to those that apply to the loan's product type (or have no product filter).</li>
 *   <li>Evaluate each remaining adjustment's condition JSON against the corresponding
 *       loan scenario parameter.</li>
 *   <li>Sum the {@code priceAdjustment} values of all matching rules.</li>
 * </ol>
 *
 * <p>Supported condition formats:
 * <ul>
 *   <li>Range: {@code {"min": 680, "max": 699}} — matches when the numeric scenario value is
 *       within the inclusive bounds. Either bound may be omitted (one-sided range).</li>
 *   <li>Equality: {@code {"equals": "INVESTMENT"}} — matches when the string scenario value
 *       equals the condition value, case-insensitive.</li>
 * </ul>
 *
 * <p>If a condition JSON is malformed or the scenario is missing a required field, the
 * adjustment is skipped (treated as non-matching) and a warning is logged. Evaluation never
 * throws — a pricing result is always returned.
 */
@Service
public class LlpaEvaluatorService {

    private static final Logger log = LoggerFactory.getLogger(LlpaEvaluatorService.class);

    private final LlpaAdjustmentRepository repository;
    private final ObjectMapper objectMapper;

    public LlpaEvaluatorService(LlpaAdjustmentRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluates all active LLPA adjustments for the investor against the loan scenario
     * and returns the total price adjustment in points.
     *
     * @param scenario the loan scenario to evaluate against
     * @return the sum of all matching LLPA adjustments in points; never null
     */
    @Transactional(readOnly = true)
    public BigDecimal evaluate(LlpaScenario scenario) {
        List<LlpaAdjustment> adjustments = repository.findByInvestorIdAndActiveTrue(scenario.investorId());
        return adjustments.stream()
                .filter(adj -> appliesToProduct(adj, scenario.productType()))
                .filter(adj -> conditionMatches(adj, scenario))
                .map(LlpaAdjustment::getPriceAdjustment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean appliesToProduct(LlpaAdjustment adj, String productType) {
        return adj.getProductType() == null
                || adj.getProductType().equalsIgnoreCase(productType);
    }

    private boolean conditionMatches(LlpaAdjustment adj, LlpaScenario scenario) {
        try {
            JsonNode condition = objectMapper.readTree(adj.getConditionJson());
            Object value = resolveScenarioValue(adj.getAdjustmentCategory(), scenario);
            if (value == null) {
                return false;
            }
            if (condition.has("equals")) {
                return condition.get("equals").asText().equalsIgnoreCase(value.toString());
            }
            if (condition.has("min") || condition.has("max")) {
                BigDecimal numValue = toBigDecimal(value);
                boolean minOk = !condition.has("min")
                        || numValue.compareTo(new BigDecimal(condition.get("min").asText())) >= 0;
                boolean maxOk = !condition.has("max")
                        || numValue.compareTo(new BigDecimal(condition.get("max").asText())) <= 0;
                return minOk && maxOk;
            }
            log.warn("Unrecognised condition format for LLPA adjustment id={}: {}", adj.getId(), adj.getConditionJson());
            return false;
        } catch (Exception e) {
            log.warn("Failed to evaluate LLPA condition for adjustment id={}: {}", adj.getId(), e.getMessage());
            return false;
        }
    }

    private Object resolveScenarioValue(String category, LlpaScenario scenario) {
        return switch (category.toUpperCase()) {
            case "CREDIT_SCORE" -> scenario.creditScore();
            case "LTV"          -> scenario.ltv();
            case "OCCUPANCY"    -> scenario.occupancy();
            case "LOAN_PURPOSE" -> scenario.loanPurpose();
            case "PROPERTY_TYPE" -> scenario.propertyType();
            default -> {
                log.warn("Unknown LLPA adjustment category: {}", category);
                yield null;
            }
        };
    }

    private BigDecimal toBigDecimal(Object value) {
        return switch (value) {
            case BigDecimal bd -> bd;
            case Integer i     -> new BigDecimal(i);
            case Long l        -> new BigDecimal(l);
            case Number n      -> new BigDecimal(n.toString());
            default            -> new BigDecimal(value.toString());
        };
    }
}
