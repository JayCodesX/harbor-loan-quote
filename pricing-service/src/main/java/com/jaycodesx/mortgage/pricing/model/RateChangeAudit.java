package com.jaycodesx.mortgage.pricing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * An audit record of a single rate-sheet activation, written by the Kafka audit
 * consumer group as it reads the rate-change stream (ADR-0052).
 *
 * <p>{@code rateSheetId} is unique — the consumer is idempotent, so an
 * at-least-once redelivery or an offset replay updates nothing and inserts no
 * duplicate. The Kafka coordinates ({@code kafkaPartition}, {@code kafkaOffset})
 * are retained so an operator can correlate a row back to its exact position in
 * the log.
 */
@Entity
@Table(name = "rate_change_audit")
public class RateChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_sheet_id", nullable = false, unique = true)
    private Long rateSheetId;

    @Column(name = "investor_id", nullable = false, length = 50)
    private String investorId;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "event_received_at", nullable = false)
    private LocalDateTime eventReceivedAt;

    @Column(name = "kafka_partition")
    private Integer kafkaPartition;

    @Column(name = "kafka_offset")
    private Long kafkaOffset;

    protected RateChangeAudit() {
        // JPA
    }

    public RateChangeAudit(Long rateSheetId, String investorId, LocalDateTime effectiveAt,
                           LocalDateTime expiresAt, LocalDateTime eventReceivedAt,
                           Integer kafkaPartition, Long kafkaOffset) {
        this.rateSheetId = rateSheetId;
        this.investorId = investorId;
        this.effectiveAt = effectiveAt;
        this.expiresAt = expiresAt;
        this.eventReceivedAt = eventReceivedAt;
        this.kafkaPartition = kafkaPartition;
        this.kafkaOffset = kafkaOffset;
    }

    public Long getId() { return id; }
    public Long getRateSheetId() { return rateSheetId; }
    public String getInvestorId() { return investorId; }
    public LocalDateTime getEffectiveAt() { return effectiveAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getEventReceivedAt() { return eventReceivedAt; }
    public Integer getKafkaPartition() { return kafkaPartition; }
    public Long getKafkaOffset() { return kafkaOffset; }
}
