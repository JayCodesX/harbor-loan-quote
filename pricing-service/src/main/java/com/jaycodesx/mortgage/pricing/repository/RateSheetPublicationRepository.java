package com.jaycodesx.mortgage.pricing.repository;

import com.jaycodesx.mortgage.pricing.model.RateSheetPublication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for rate sheet publications.
 *
 * Extends {@link JpaRepository} because the {@code status} column requires update access —
 * when a new sheet is published, the previously ACTIVE sheet for the same investor is
 * transitioned to SUPERSEDED. This is the only permitted mutation; entity columns other
 * than {@code status} are protected by {@code @Column(updatable = false)}.
 *
 * Records must never be deleted — they are the permanent audit record of investor
 * publications. Any PR adding delete* methods to this interface must be reviewed by
 * a compliance owner before merging.
 */
public interface RateSheetPublicationRepository extends JpaRepository<RateSheetPublication, Long> {

    /**
     * Returns the most recently effective ACTIVE sheet for the given investor,
     * or empty if no active sheet exists.
     */
    Optional<RateSheetPublication> findTopByInvestorIdAndStatusOrderByEffectiveAtDesc(
            String investorId, String status);

    /**
     * Returns all sheets for the given investor in a given status, newest first.
     * Used during publish to identify sheets that must be superseded.
     */
    List<RateSheetPublication> findByInvestorIdAndStatusOrderByEffectiveAtDesc(
            String investorId, String status);
}
