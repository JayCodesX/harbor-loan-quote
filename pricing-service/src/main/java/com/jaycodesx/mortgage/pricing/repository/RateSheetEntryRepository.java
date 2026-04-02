package com.jaycodesx.mortgage.pricing.repository;

import com.jaycodesx.mortgage.pricing.model.RateSheetEntry;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Append-only repository for rate sheet entries.
 *
 * Extends {@link Repository} directly rather than {@link org.springframework.data.jpa.repository.JpaRepository}
 * so that only explicitly declared methods are available. Delete and update operations are
 * intentionally absent — rate sheet entries are financial records and must never be removed
 * or modified after they are written. Referential integrity is also enforced at the schema
 * level via a foreign key constraint on the {@code rate_sheet_id} column.
 */
public interface RateSheetEntryRepository extends Repository<RateSheetEntry, Long> {

    RateSheetEntry save(RateSheetEntry entry);

    List<RateSheetEntry> saveAll(Iterable<RateSheetEntry> entries);

    /**
     * Returns all entries for a given rate sheet, ordered by product/term then rate ascending
     * (lowest rate first within each product/term ladder).
     */
    List<RateSheetEntry> findByRateSheetIdOrderByProductTermIdAscRateAsc(Long rateSheetId);
}
