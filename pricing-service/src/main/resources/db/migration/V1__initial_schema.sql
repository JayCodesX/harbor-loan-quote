-- V1: Initial schema — captures tables previously managed by Hibernate ddl-auto: update.
-- Flyway now owns all DDL from this point forward.

CREATE TABLE IF NOT EXISTS pricing_products (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    program_code VARCHAR(40)   NOT NULL,
    product_name VARCHAR(80)   NOT NULL,
    base_rate    DECIMAL(7, 4) NOT NULL,
    active       BIT(1)        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pricing_products_program_code (program_code)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS rate_sheets (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    program_code   VARCHAR(40)   NOT NULL,
    property_use   VARCHAR(40)   NOT NULL,
    zip_prefix     VARCHAR(5)    NOT NULL,
    zip_adjustment DECIMAL(7, 4) NOT NULL,
    active         BIT(1)        NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS pricing_adjustment_rules (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    rule_type  VARCHAR(40)   NOT NULL,
    rule_key   VARCHAR(40)   NOT NULL,
    adjustment DECIMAL(7, 4) NOT NULL,
    active     BIT(1)        NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;
