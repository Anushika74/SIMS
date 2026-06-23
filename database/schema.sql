-- =====================================================================
-- Smart Inventory Management System — MySQL Schema (DDL)
-- =====================================================================
-- The Java application (Hibernate, ddl-auto=update) will also create these
-- tables automatically. This script is provided for reference, the ER diagram
-- and manual setup. Run it before sample_data.sql if you import data manually.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS smart_inventory
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_inventory;

-- ---------- Users (authentication & roles) ----------
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,          -- BCrypt hash
    full_name   VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL,          -- ADMIN | STAFF
    enabled     BIT(1)       NOT NULL DEFAULT b'1',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------- Categories ----------
CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(60)  NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- ---------- Products ----------
CREATE TABLE IF NOT EXISTS products (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku           VARCHAR(40)  NOT NULL UNIQUE,
    name          VARCHAR(120) NOT NULL,
    description   VARCHAR(400),
    category_id   BIGINT,
    unit_price    DECIMAL(12,2) NOT NULL DEFAULT 0,
    cost_price    DECIMAL(12,2) NOT NULL DEFAULT 0,
    quantity      INT          NOT NULL DEFAULT 0,
    reorder_level INT          NOT NULL DEFAULT 10,
    supplier      VARCHAR(120),
    expiry_date   DATE,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories (id),
    INDEX idx_product_name (name),
    INDEX idx_product_category (category_id)
);

-- ---------- Sales (bills / invoices) ----------
CREATE TABLE IF NOT EXISTS sales (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_no     VARCHAR(30)  NOT NULL UNIQUE,
    sale_date      DATETIME     NOT NULL,
    cashier        VARCHAR(50),
    payment_method VARCHAR(20)  DEFAULT 'CASH',
    total_amount   DECIMAL(12,2) NOT NULL DEFAULT 0,
    INDEX idx_sale_date (sale_date)
);

-- ---------- Sale line items ----------
CREATE TABLE IF NOT EXISTS sale_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id     BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    quantity    INT    NOT NULL,
    unit_price  DECIMAL(12,2) NOT NULL DEFAULT 0,
    line_total  DECIMAL(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_item_sale    FOREIGN KEY (sale_id)    REFERENCES sales (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES products (id),
    INDEX idx_item_product (product_id),
    INDEX idx_item_sale (sale_id)
);

-- ---------- Stock movement audit trail ----------
CREATE TABLE IF NOT EXISTS stock_movements (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT NOT NULL,
    type         VARCHAR(10) NOT NULL,          -- IN | OUT | ADJUST
    quantity     INT    NOT NULL,
    reason       VARCHAR(200),
    performed_by VARCHAR(50),
    timestamp    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movement_product FOREIGN KEY (product_id) REFERENCES products (id),
    INDEX idx_movement_product (product_id)
);

-- ---------- Wastage (stock lost not sold) ----------
CREATE TABLE IF NOT EXISTS wastage (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT NOT NULL,
    quantity     INT    NOT NULL,
    reason       VARCHAR(20) NOT NULL,          -- EXPIRED | DAMAGED | SPOILED | THEFT | OTHER
    note         VARCHAR(255),
    loss_amount  DECIMAL(12,2) NOT NULL DEFAULT 0,
    performed_by VARCHAR(50),
    timestamp    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wastage_product FOREIGN KEY (product_id) REFERENCES products (id),
    INDEX idx_wastage_product (product_id)
);
