package com.jaycodesx.mortgage.pricing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single LLPA (Loan Level Price Adjustment) rule for a given investor.
 *
 * <p>LLPAs are additive price adjustments in points applied on top of the base rate sheet
 * price. All active adjustments for an investor are evaluated against the loan scenario;
 * those whose condition matches contribute their {@code priceAdjustment} to the total.
 *
 * <p>The {@code conditionJson} field stores a typed condition expression:
 * <ul>
 *   <li>Range: {@code {"min": 680, "max": 699}} — matches when the target value is
 *       within the inclusive range [min, max]. Either bound may be omitted for a
 *       one-sided range (e.g. {@code {"min": 96}} means "96 or above").</li>
 *   <li>Equality: {@code {"equals": "INVESTMENT"}} — matches when the target value
 *       equals the string, case-insensitive.</li>
 * </ul>
 *
 * <p>Unlike rate sheet entries, LLPA records are mutable — lenders must be able to update
 * price adjustment values and deactivate rules through the admin UI when investors publish
 * revised matrices. Deactivation (setting {@code active=false}) is strongly preferred over
 * deletion so the history of applied adjustments is retained.
 */
@Entity
@Table(name = "llpa_adjustment")
public class LlpaAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The investor this adjustment applies to, e.g. "FANNIE_MAE", "FREDDIE_MAC". */
    @Column(nullable = false, length = 50)
    private String investorId;

    /**
     * The loan product this adjustment is scoped to, e.g. "CONVENTIONAL", "FHA".
     * {@code null} means the adjustment applies to all products for this investor.
     */
    @Column(length = 40)
    private String productType;

    /**
     * The loan characteristic this adjustment targets.
     * Standard values: CREDIT_SCORE, LTV, OCCUPANCY, LOAN_PURPOSE, PROPERTY_TYPE.
     */
    @Column(nullable = false, length = 40)
    private String adjustmentCategory;

    /**
     * JSON condition expression evaluated against the loan scenario parameter identified
     * by {@code adjustmentCategory}. See class-level Javadoc for format details.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String conditionJson;

    /**
     * The price adjustment in points added to the running total when the condition matches.
     * Positive values increase the price (worse for the borrower); negative values decrease it.
     */
    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal priceAdjustment;

    /** The date from which this adjustment is effective. */
    @Column(nullable = false)
    private LocalDateTime effectiveAt;

    /** The date after which this adjustment is no longer effective. {@code null} = no expiry. */
    @Column
    private LocalDateTime expiresAt;

    /** Whether this adjustment is currently active. Set to false to deactivate without deleting. */
    @Column(nullable = false)
    private boolean active;

    public Long getId() { return id; }

    public String getInvestorId() { return investorId; }
    public void setInvestorId(String investorId) { this.investorId = investorId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getAdjustmentCategory() { return adjustmentCategory; }
    public void setAdjustmentCategory(String adjustmentCategory) { this.adjustmentCategory = adjustmentCategory; }

    public String getConditionJson() { return conditionJson; }
    public void setConditionJson(String conditionJson) { this.conditionJson = conditionJson; }

    public BigDecimal getPriceAdjustment() { return priceAdjustment; }
    public void setPriceAdjustment(BigDecimal priceAdjustment) { this.priceAdjustment = priceAdjustment; }

    public LocalDateTime getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(LocalDateTime effectiveAt) { this.effectiveAt = effectiveAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
