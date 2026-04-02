package com.jaycodesx.mortgage.pricing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A single rate/price pair within an investor-published rate sheet.
 *
 * Each entry represents one point on the rate/price ladder for a specific product and term:
 * the borrower can choose a lower rate by paying points (positive price) or accept a higher
 * rate in exchange for lender credits (negative price). Multiple entries with the same
 * {@code productTermId} but different {@code rate}/{@code price} values form the ladder.
 *
 * Entries are fully immutable — no column may be updated after initial insert. The
 * repository intentionally exposes no delete or update methods. Referential integrity is
 * enforced at the schema level via a foreign key to {@code rate_sheet}.
 */
@Entity
@Table(name = "rate_sheet_entry")
public class RateSheetEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    /**
     * FK to {@code rate_sheet.id}. Stored as a plain Long (not a @ManyToOne) to
     * keep the entity decoupled and reinforce the append-only discipline — entries
     * are written once and referenced by ID, never navigated back to their parent.
     */
    @Column(nullable = false, updatable = false)
    private Long rateSheetId;

    /**
     * Product and term identifier, e.g. "CONVENTIONAL_30", "FHA_30", "VA_15".
     * Matches the {@code programCode + "_" + termYears} convention used elsewhere in the
     * pricing service.
     */
    @Column(nullable = false, updatable = false, length = 40)
    private String productTermId;

    /**
     * The note rate offered at this point on the ladder, e.g. 6.7500.
     * Stored to 4 decimal places (basis points / 100).
     */
    @Column(nullable = false, updatable = false, precision = 5, scale = 4)
    private BigDecimal rate;

    /**
     * The price in points at this rate. Negative values are lender credits (borrower receives
     * cash at closing); positive values are discount points (borrower pays to buy down the rate).
     * Stored to 4 decimal places.
     */
    @Column(nullable = false, updatable = false, precision = 6, scale = 4)
    private BigDecimal price;

    protected RateSheetEntry() {
        // JPA only
    }

    public RateSheetEntry(Long rateSheetId, String productTermId, BigDecimal rate, BigDecimal price) {
        this.rateSheetId = rateSheetId;
        this.productTermId = productTermId;
        this.rate = rate;
        this.price = price;
    }

    public Long getId() { return id; }
    public Long getRateSheetId() { return rateSheetId; }
    public String getProductTermId() { return productTermId; }
    public BigDecimal getRate() { return rate; }
    public BigDecimal getPrice() { return price; }
}
