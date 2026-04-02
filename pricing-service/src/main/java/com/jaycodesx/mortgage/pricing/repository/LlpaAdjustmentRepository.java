package com.jaycodesx.mortgage.pricing.repository;

import com.jaycodesx.mortgage.pricing.model.LlpaAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for LLPA adjustment rules.
 *
 * <p>Unlike rate sheet entries, LLPA records are mutable — lenders update price adjustment
 * values and deactivate rules as investors publish revised matrices. Full CRUD access is
 * available through {@link JpaRepository} to support the admin UI.
 *
 * <p>Deleting LLPA records is strongly discouraged — deactivate via {@code active=false}
 * instead so the history of what adjustments were applied and when is retained.
 */
public interface LlpaAdjustmentRepository extends JpaRepository<LlpaAdjustment, Long> {

    /**
     * Returns all active adjustments for the given investor.
     * This is the primary query used by the evaluator at pricing time.
     */
    List<LlpaAdjustment> findByInvestorIdAndActiveTrue(String investorId);

    /**
     * Returns all active adjustments for the given investor and adjustment category.
     * Useful for targeted admin review of a specific category (e.g. all active CREDIT_SCORE rules).
     */
    List<LlpaAdjustment> findByInvestorIdAndAdjustmentCategoryAndActiveTrue(
            String investorId, String adjustmentCategory);
}
