-- V3: LLPA (Loan Level Price Adjustment) flat condition table (ADR-0020)
--
-- Each row is one LLPA rule for a given investor. Evaluation: load all active adjustments
-- for the investor, evaluate each condition against the loan scenario, sum all matches.
--
-- condition_json stores typed condition expressions:
--   Range:    {"min": 680, "max": 699}   — matches when the target value is within [min, max]
--   Equality: {"equals": "INVESTMENT"}   — matches when the target value equals the string
--   One-sided range is supported: {"min": 96} or {"max": 60}
--
-- product_type is nullable: NULL means the adjustment applies to all loan products.
-- expires_at is nullable: NULL means no expiry — the rule is active until explicitly deactivated.
--
-- LLPA data is managed by lenders through the admin UI. Records are mutable (admins can
-- deactivate or update values when investors publish revised matrices), unlike rate sheet
-- entries which are fully immutable. However, deactivation is preferred over deletion —
-- retaining the history of what LLPAs were applied and when.

CREATE TABLE llpa_adjustment (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    investor_id          VARCHAR(50)   NOT NULL,
    product_type         VARCHAR(40)   NULL,
    adjustment_category  VARCHAR(40)   NOT NULL,
    condition_json       TEXT          NOT NULL,
    price_adjustment     DECIMAL(7, 4) NOT NULL,
    effective_at         DATETIME(6)   NOT NULL,
    expires_at           DATETIME(6)   NULL,
    active               BIT(1)        NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    INDEX idx_llpa_investor_active (investor_id, active),
    INDEX idx_llpa_category (adjustment_category)
) ENGINE = InnoDB;
