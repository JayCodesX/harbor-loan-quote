package com.jaycodesx.mortgage.pricing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * An investor-published rate sheet.
 *
 * Rate sheets are immutable once published — investor ID, effective window, and source
 * are set at construction and never changed. The only mutable field is {@code status},
 * which transitions from ACTIVE to SUPERSEDED when a newer sheet is published for the
 * same investor, or to EXPIRED when the effective window has passed.
 *
 * Records are never deleted. Every published rate sheet is retained permanently as the
 * authoritative record of what rates were available during its effective window.
 * Pricing results store {@code rate_sheet_id} so any quote can be reproduced exactly.
 */
@Entity
@Table(name = "rate_sheet")
public class RateSheetPublication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(nullable = false, updatable = false, length = 50)
    private String investorId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime effectiveAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    /**
     * Lifecycle status. ACTIVE → SUPERSEDED when a newer publication for the same investor
     * is ingested. EXPIRED when the effective window is past. This is the only column
     * updated after initial insert.
     */
    @Column(nullable = false, length = 20)
    private String status;

    /** Set by @PrePersist — the wall-clock time this sheet was ingested by the system. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime importedAt;

    /**
     * Human-readable source identifier for this sheet, e.g.
     * "fannie-mae-morning-sheet-2026-04-02T09:00".
     */
    @Column(nullable = false, updatable = false, length = 200)
    private String source;

    protected RateSheetPublication() {
        // JPA only
    }

    public RateSheetPublication(String investorId, LocalDateTime effectiveAt,
                                 LocalDateTime expiresAt, String source) {
        this.investorId = investorId;
        this.effectiveAt = effectiveAt;
        this.expiresAt = expiresAt;
        this.source = source;
        this.status = "ACTIVE";
    }

    @PrePersist
    void onInsert() {
        this.importedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getInvestorId() { return investorId; }
    public LocalDateTime getEffectiveAt() { return effectiveAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public String getStatus() { return status; }
    public LocalDateTime getImportedAt() { return importedAt; }
    public String getSource() { return source; }

    /** Only status transitions are permitted after initial insert. */
    public void setStatus(String status) { this.status = status; }
}
