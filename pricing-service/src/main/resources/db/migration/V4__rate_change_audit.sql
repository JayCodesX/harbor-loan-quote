-- V4: Rate-change audit log (ADR-0052)
--
-- Durable record of every rate-sheet activation, written by the Kafka audit
-- consumer group (rate-change-audit) as it reads the pricing.rate-sheet.activated
-- topic. This is the concrete justification for putting the rate-change fan-out on
-- Kafka rather than a work queue: retention + replay. The topic can be re-read to
-- rebuild this table, or to onboard a new consumer over full history.
--
-- rate_sheet_id is UNIQUE so the consumer is idempotent: Kafka guarantees
-- at-least-once delivery, so redeliveries and offset replays must not create
-- duplicate audit rows. The consumer treats a unique-key conflict as "already
-- recorded" and moves on.

CREATE TABLE rate_change_audit (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    rate_sheet_id      BIGINT       NOT NULL,
    investor_id        VARCHAR(50)  NOT NULL,
    effective_at       DATETIME(6)  NOT NULL,
    expires_at         DATETIME(6)  NULL,
    event_received_at  DATETIME(6)  NOT NULL,
    kafka_partition    INT          NULL,
    kafka_offset       BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_rate_change_audit_sheet (rate_sheet_id),
    INDEX idx_rate_change_audit_investor (investor_id),
    INDEX idx_rate_change_audit_received (event_received_at)
) ENGINE = InnoDB;
