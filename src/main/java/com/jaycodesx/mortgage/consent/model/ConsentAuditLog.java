package com.jaycodesx.mortgage.consent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Append-only consent audit log entry. Records a single point-in-time consent event.
 *
 * Every column is marked updatable = false and the entity exposes no setters — Hibernate
 * cannot generate an UPDATE statement for any field after the initial INSERT. Revocations
 * are recorded as new entries; existing records are never modified or deleted.
 *
 * Retention: TCPA statute of limitations is 4 years from the date of consent or last
 * contact, whichever is later. Records must not be deleted during the retention window.
 * Phase 3 migration to S3 Object Lock for compliance archival — see ADR-0034.
 */
@Entity
@Table(name = "consent_audit_log")
public class ConsentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    /** The loan quote associated with this consent event. */
    @Column(nullable = false, updatable = false)
    private Long loanQuoteId;

    /** The borrower profile that gave or revoked consent. */
    @Column(nullable = false, updatable = false)
    private Long borrowerQuoteProfileId;

    /**
     * The type of consent captured.
     * Valid values: TCPA | EMAIL_OPT_IN | LEAD_SHARE
     */
    @Column(nullable = false, updatable = false, length = 20)
    private String consentType;

    /**
     * Whether consent was given or withdrawn.
     * Valid values: GRANTED | REVOKED
     */
    @Column(nullable = false, updatable = false, length = 10)
    private String consentAction;

    /** The exact consent language displayed to the borrower at the time of consent. */
    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String consentLanguage;

    /** IP address of the borrower at the time of consent. Supports IPv4 and IPv6. */
    @Column(nullable = false, updatable = false, length = 45)
    private String ipAddress;

    /** User-agent string of the borrower's browser or client at the time of consent. */
    @Column(updatable = false, length = 512)
    private String userAgent;

    /** Server-side UTC timestamp recorded at the moment of INSERT. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    /** Schema version for future evolution of this record format. */
    @Column(nullable = false, updatable = false)
    private int schemaVersion;

    /** Required by JPA — not for application use. */
    protected ConsentAuditLog() {}

    public ConsentAuditLog(
            Long loanQuoteId,
            Long borrowerQuoteProfileId,
            String consentType,
            String consentAction,
            String consentLanguage,
            String ipAddress,
            String userAgent
    ) {
        this.loanQuoteId = loanQuoteId;
        this.borrowerQuoteProfileId = borrowerQuoteProfileId;
        this.consentType = consentType;
        this.consentAction = consentAction;
        this.consentLanguage = consentLanguage;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.schemaVersion = 1;
    }

    @PrePersist
    void onInsert() {
        recordedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getLoanQuoteId() { return loanQuoteId; }
    public Long getBorrowerQuoteProfileId() { return borrowerQuoteProfileId; }
    public String getConsentType() { return consentType; }
    public String getConsentAction() { return consentAction; }
    public String getConsentLanguage() { return consentLanguage; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public int getSchemaVersion() { return schemaVersion; }
}
