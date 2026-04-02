package com.jaycodesx.mortgage.pricing.service;

import java.math.BigDecimal;

/**
 * The loan scenario parameters used for LLPA evaluation.
 *
 * <p>All fields that correspond to LLPA categories are nullable — when a field is null the
 * evaluator skips any adjustments targeting that category, treating them as non-matching.
 * This allows partial evaluation (e.g. public quotes that don't yet have a credit score).
 *
 * @param investorId   the investor whose active LLPA rules are loaded (e.g. "FANNIE_MAE")
 * @param productType  the loan product (e.g. "CONVENTIONAL", "FHA"); used to filter
 *                     product-scoped adjustments. Null-targeting adjustments always apply.
 * @param creditScore  the borrower's FICO credit score
 * @param ltv          the loan-to-value ratio as a percentage (e.g. 80.00 for 80%)
 * @param occupancy    the property occupancy type: PRIMARY_RESIDENCE, SECOND_HOME, or INVESTMENT
 * @param loanPurpose  the loan purpose: PURCHASE, RATE_TERM_REFI, or CASH_OUT_REFI
 * @param propertyType the property type: SINGLE_FAMILY, MULTI_UNIT, CONDO, or MANUFACTURED
 */
public record LlpaScenario(
        String investorId,
        String productType,
        Integer creditScore,
        BigDecimal ltv,
        String occupancy,
        String loanPurpose,
        String propertyType
) {
}
