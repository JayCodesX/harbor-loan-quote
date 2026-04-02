package com.jaycodesx.mortgage.pricing.service;

import com.jaycodesx.mortgage.pricing.dto.RateSheetEntryRequest;
import com.jaycodesx.mortgage.pricing.model.RateSheetEntry;
import com.jaycodesx.mortgage.pricing.model.RateSheetPublication;
import com.jaycodesx.mortgage.pricing.repository.RateSheetEntryRepository;
import com.jaycodesx.mortgage.pricing.repository.RateSheetPublicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Manages investor-published rate sheets per ADR-0019.
 *
 * <p>Rate sheets are immutable once published. A new investor publication supersedes
 * all currently ACTIVE sheets for the same investor — their status transitions to
 * SUPERSEDED. The previous records are retained permanently; they are never modified
 * beyond this status transition, and never deleted.
 *
 * <p>Every pricing result stores a {@code rate_sheet_id} so any quote can be reproduced
 * exactly, even months after the sheet was superseded. Use {@link #findById(Long)} to
 * retrieve a historical sheet for audit or dispute resolution.
 */
@Service
public class RateSheetService {

    private final RateSheetPublicationRepository publicationRepository;
    private final RateSheetEntryRepository entryRepository;

    public RateSheetService(RateSheetPublicationRepository publicationRepository,
                             RateSheetEntryRepository entryRepository) {
        this.publicationRepository = publicationRepository;
        this.entryRepository = entryRepository;
    }

    /**
     * Publishes a new rate sheet for the given investor.
     *
     * <ol>
     *   <li>All currently ACTIVE sheets for {@code investorId} are transitioned to SUPERSEDED.</li>
     *   <li>A new {@link RateSheetPublication} is created with status ACTIVE.</li>
     *   <li>The provided entries are written as immutable {@link RateSheetEntry} records
     *       referencing the new publication.</li>
     * </ol>
     *
     * @param investorId  the investor publishing the sheet (e.g., "FANNIE_MAE")
     * @param effectiveAt the moment from which this sheet's rates are valid
     * @param expiresAt   the moment after which this sheet's rates are no longer valid
     * @param source      a human-readable identifier for this publication
     * @param entries     the rate/price ladder entries for this sheet
     * @return the newly created and persisted {@link RateSheetPublication}
     */
    @Transactional
    public RateSheetPublication publish(String investorId,
                                         LocalDateTime effectiveAt,
                                         LocalDateTime expiresAt,
                                         String source,
                                         List<RateSheetEntryRequest> entries) {
        List<RateSheetPublication> active = publicationRepository
                .findByInvestorIdAndStatusOrderByEffectiveAtDesc(investorId, "ACTIVE");
        active.forEach(s -> s.setStatus("SUPERSEDED"));
        publicationRepository.saveAll(active);

        RateSheetPublication publication = new RateSheetPublication(investorId, effectiveAt, expiresAt, source);
        RateSheetPublication saved = publicationRepository.save(publication);

        List<RateSheetEntry> entryEntities = entries.stream()
                .map(e -> new RateSheetEntry(saved.getId(), e.productTermId(), e.rate(), e.price()))
                .toList();
        entryRepository.saveAll(entryEntities);

        return saved;
    }

    /**
     * Returns the currently ACTIVE rate sheet for the given investor, or empty if none exists.
     * This is the sheet used by the pricing engine for live quote calculations.
     */
    @Transactional(readOnly = true)
    public Optional<RateSheetPublication> findActive(String investorId) {
        return publicationRepository
                .findTopByInvestorIdAndStatusOrderByEffectiveAtDesc(investorId, "ACTIVE");
    }

    /**
     * Returns the rate sheet with the given ID, regardless of status.
     * Used for audit and quote reproducibility — a quote can be re-priced against the
     * exact rate sheet that was active at the time of calculation.
     */
    @Transactional(readOnly = true)
    public Optional<RateSheetPublication> findById(Long id) {
        return publicationRepository.findById(id);
    }

    /**
     * Returns all entries for the given rate sheet, ordered by product/term then rate ascending.
     */
    @Transactional(readOnly = true)
    public List<RateSheetEntry> findEntriesForSheet(Long rateSheetId) {
        return entryRepository.findByRateSheetIdOrderByProductTermIdAscRateAsc(rateSheetId);
    }
}
