-- ─────────────────────────────────────────────────────────────────────────────
-- WealthWise — MySQL Schema
-- Run this once to create the database and all tables.
-- Then start the Spring Boot app — JPA will manage columns from here.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE DATABASE IF NOT EXISTS wealthwise
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE wealthwise;

-- ── M02: Scheme Master ────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS scheme_master (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    amfi_code       VARCHAR(20)  UNIQUE NOT NULL,
    isin_growth     VARCHAR(20),
    isin_idcw       VARCHAR(20),
    scheme_name     VARCHAR(500) NOT NULL,
    amc_name        VARCHAR(200),
    fund_family_name VARCHAR(200),
    plan_type       ENUM('DIRECT','REGULAR'),
    option_type     ENUM('GROWTH','IDCW_PAYOUT','IDCW_REINVESTMENT'),
    fund_type       ENUM('OPEN_ENDED','CLOSE_ENDED','INTERVAL'),
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_amc_name (amc_name),
    INDEX idx_isin_growth (isin_growth),
    INDEX idx_isin_idcw (isin_idcw),
    INDEX idx_is_active (is_active)
);

CREATE TABLE IF NOT EXISTS scheme_category (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    amfi_code          VARCHAR(20),
    broad_category     ENUM('EQUITY','DEBT','HYBRID','SOLUTION','OTHER'),
    sebi_category      VARCHAR(100),
    sub_category       VARCHAR(100),
    equity_percentage  DECIMAL(5,2),
    taxation_type      ENUM('EQUITY_TAX','DEBT_TAX'),
    risk_level         TINYINT CHECK (risk_level BETWEEN 1 AND 6),
    benchmark_index    VARCHAR(200),
    FOREIGN KEY (amfi_code) REFERENCES scheme_master(amfi_code),
    INDEX idx_broad_cat (broad_category)
);

CREATE TABLE IF NOT EXISTS nav_data (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    amfi_code   VARCHAR(20)    NOT NULL,
    nav_date    DATE           NOT NULL,
    nav_value   DECIMAL(15,4)  NOT NULL,
    FOREIGN KEY (amfi_code) REFERENCES scheme_master(amfi_code),
    UNIQUE KEY uq_nav_date (amfi_code, nav_date),
    INDEX idx_nav_amfi (amfi_code)
);

CREATE TABLE IF NOT EXISTS pdf_upload_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name       VARCHAR(255),
    uploaded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status          ENUM('PROCESSING','SUCCESS','FAILED'),
    records_parsed  INT,
    error_message   TEXT
);

-- ── M04: Portfolio / CAS ──────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_portfolio (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT        NOT NULL,
    folio_number     VARCHAR(50),
    amfi_code        VARCHAR(20),
    scheme_name      VARCHAR(500),
    amc_name         VARCHAR(200),
    total_units      DECIMAL(15,4),
    avg_nav          DECIMAL(15,4),
    invested_amount  DECIMAL(15,2),
    current_value    DECIMAL(15,2),
    unrealised_gain  DECIMAL(15,2),
    xirr             DECIMAL(8,4),
    last_updated     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (amfi_code) REFERENCES scheme_master(amfi_code),
    UNIQUE KEY uq_user_folio (user_id, folio_number),
    INDEX idx_up_user (user_id)
);

CREATE TABLE IF NOT EXISTS user_transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    folio_number        VARCHAR(50),
    amfi_code           VARCHAR(20),
    transaction_date    DATE,
    transaction_type    ENUM('PURCHASE','REDEMPTION','SIP',
                             'SWITCH_IN','SWITCH_OUT','DIVIDEND','BONUS'),
    amount              DECIMAL(15,2),
    units               DECIMAL(15,4),
    nav                 DECIMAL(15,4),
    balance_units       DECIMAL(15,4),
    description         VARCHAR(500),
    FOREIGN KEY (amfi_code) REFERENCES scheme_master(amfi_code),
    INDEX idx_ut_user_folio (user_id, folio_number),
    INDEX idx_ut_date (transaction_date)
);

CREATE TABLE IF NOT EXISTS cas_upload_log (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    file_name           VARCHAR(255),
    uploaded_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status              ENUM('PROCESSING','SUCCESS','FAILED'),
    total_folios        INT DEFAULT 0,
    total_transactions  INT DEFAULT 0,
    error_message       TEXT,
    INDEX idx_cul_user (user_id)
);

CREATE TABLE IF NOT EXISTS tax_lots (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    amfi_code        VARCHAR(20),
    folio_number     VARCHAR(50),
    purchase_date    DATE,
    units            DECIMAL(15,4),
    purchase_nav     DECIMAL(15,4),
    cost_basis       DECIMAL(15,2),
    remaining_units  DECIMAL(15,4),
    is_exhausted     BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (amfi_code) REFERENCES scheme_master(amfi_code),
    INDEX idx_tl_user (user_id),
    INDEX idx_tl_exhausted (is_exhausted)
);
