-- V2: Rate sheet data model (ADR-0019)
--
-- rate_sheet: one record per investor publication. Immutable once created except for the
--   status column (ACTIVE → SUPERSEDED when a newer sheet is published, EXPIRED when past expires_at).
--   Records are never deleted — they are the permanent audit record of what rates were available and when.
--
-- rate_sheet_entry: one row per (rate_sheet, product_term, rate) tuple — the rate/price ladder.
--   Fully immutable. The FK constraint to rate_sheet ensures entries can never outlive their parent.
--   The parent rate_sheet record is also never deleted, so referential integrity is permanent.

CREATE TABLE rate_sheet (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    investor_id VARCHAR(50)  NOT NULL,
    effective_at DATETIME(6) NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    imported_at DATETIME(6)  NOT NULL,
    source      VARCHAR(200) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_rate_sheet_investor_status (investor_id, status),
    INDEX idx_rate_sheet_effective_at (effective_at)
) ENGINE = InnoDB;

CREATE TABLE rate_sheet_entry (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    rate_sheet_id   BIGINT        NOT NULL,
    product_term_id VARCHAR(40)   NOT NULL,
    rate            DECIMAL(5, 4) NOT NULL,
    price           DECIMAL(6, 4) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rate_sheet_entry_rate_sheet
        FOREIGN KEY (rate_sheet_id) REFERENCES rate_sheet (id),
    INDEX idx_rate_sheet_entry_sheet_product (rate_sheet_id, product_term_id)
) ENGINE = InnoDB;
