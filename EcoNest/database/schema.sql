-- =====================================================================
--  EcoNest Desktop Application - Database Schema
--  Database : econest_db
--  Engine   : MySQL 8 / MariaDB 10.4+ (the "MySQL" that ships with XAMPP)
--  How to use: phpMyAdmin > Import > choose this file > Go
--  Then import sample-data.sql the same way.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS econest_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE econest_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS email_log;
DROP TABLE IF EXISTS email_templates;
DROP TABLE IF EXISTS deliveries;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS stock_in;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS product_price_history;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS clients;
DROP TABLE IF EXISTS delivery_agents;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- users : people who can log in to the application (Admin panel)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    salt          VARCHAR(64)  NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          ENUM('ADMIN','STAFF') NOT NULL DEFAULT 'STAFF',
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Task 1 (Senuji) : product catalogue
-- ---------------------------------------------------------------------
CREATE TABLE products (
    product_id  INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    category    ENUM('PLANT','PLANTER','GARDEN_KIT') NOT NULL,
    description VARCHAR(255)  NULL,
    price       DECIMAL(10,2) NOT NULL,
    eco_rating  TINYINT       NOT NULL,
    active      TINYINT(1)    NOT NULL DEFAULT 1,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_products_price  CHECK (price > 0),
    CONSTRAINT chk_products_rating CHECK (eco_rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

-- Additional feature (Senuji) : every price change is recorded
CREATE TABLE product_price_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT           NOT NULL,
    old_price  DECIMAL(10,2) NOT NULL,
    new_price  DECIMAL(10,2) NOT NULL,
    changed_by VARCHAR(50)   NOT NULL,
    changed_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_price_history_product FOREIGN KEY (product_id)
        REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Task 2 (Mishara) : household and office clients
-- ---------------------------------------------------------------------
CREATE TABLE clients (
    client_id    INT AUTO_INCREMENT PRIMARY KEY,
    client_type  ENUM('HOUSEHOLD','OFFICE') NOT NULL,
    name         VARCHAR(100) NOT NULL,
    company_name VARCHAR(100) NULL,
    phone        VARCHAR(20)  NOT NULL,
    email        VARCHAR(100) NOT NULL,
    address      VARCHAR(255) NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Task 3 (Zija) : stock / inventory, suppliers and stock-in records
-- ---------------------------------------------------------------------
CREATE TABLE inventory (
    product_id       INT      NOT NULL PRIMARY KEY,
    quantity_on_hand INT      NOT NULL DEFAULT 0,
    reorder_level    INT      NOT NULL DEFAULT 10,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_inventory_qty     CHECK (quantity_on_hand >= 0),
    CONSTRAINT chk_inventory_reorder CHECK (reorder_level >= 0),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id)
        REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE suppliers (
    supplier_id INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    phone       VARCHAR(20)  NOT NULL,
    email       VARCHAR(100) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE stock_in (
    stock_in_id   INT AUTO_INCREMENT PRIMARY KEY,
    product_id    INT           NOT NULL,
    supplier_id   INT           NOT NULL,
    quantity      INT           NOT NULL,
    unit_cost     DECIMAL(10,2) NOT NULL,
    received_date DATE          NOT NULL,
    note          VARCHAR(255)  NULL,
    recorded_by   VARCHAR(50)   NOT NULL,
    CONSTRAINT chk_stock_in_qty CHECK (quantity > 0),
    CONSTRAINT fk_stock_in_product FOREIGN KEY (product_id)
        REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_in_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers(supplier_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Task 4 (Zija) : delivery agents and their vehicles
-- ---------------------------------------------------------------------
CREATE TABLE delivery_agents (
    agent_id       INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    email          VARCHAR(100) NOT NULL,
    vehicle_type   ENUM('BICYCLE','MOTORBIKE','THREE_WHEELER','VAN','LORRY') NOT NULL,
    vehicle_number VARCHAR(20)  NOT NULL UNIQUE,
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Task 5 (Fahath) : client orders and order lines
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    order_id     INT AUTO_INCREMENT PRIMARY KEY,
    order_ref    VARCHAR(30)   NOT NULL UNIQUE,
    client_id    INT           NOT NULL,
    order_date   DATETIME      NOT NULL,
    status       ENUM('PENDING','PROCESSING','DISPATCHED','DELIVERED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    notes        VARCHAR(255)  NULL,
    created_by   VARCHAR(50)   NOT NULL,
    CONSTRAINT fk_orders_client FOREIGN KEY (client_id)
        REFERENCES clients(client_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_orders_date   ON orders(order_date);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TABLE order_items (
    item_id    INT AUTO_INCREMENT PRIMARY KEY,
    order_id   INT           NOT NULL,
    product_id INT           NOT NULL,
    quantity   INT           NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    line_total DECIMAL(12,2) NOT NULL,
    CONSTRAINT chk_order_items_qty CHECK (quantity > 0),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products(product_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Task 6 (Fahath) : one delivery per order, assigned to one agent
-- ---------------------------------------------------------------------
CREATE TABLE deliveries (
    delivery_id  INT AUTO_INCREMENT PRIMARY KEY,
    order_id     INT          NOT NULL UNIQUE,
    agent_id     INT          NOT NULL,
    scheduled_at DATETIME     NOT NULL,
    status       ENUM('ASSIGNED','IN_TRANSIT','DELIVERED','FAILED') NOT NULL DEFAULT 'ASSIGNED',
    assigned_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at DATETIME     NULL,
    notes        VARCHAR(255) NULL,
    CONSTRAINT fk_deliveries_order FOREIGN KEY (order_id)
        REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_deliveries_agent FOREIGN KEY (agent_id)
        REFERENCES delivery_agents(agent_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tasks 8 & 9 (Rakshana, Sandeepa) : e-mail templates and e-mail log
-- ---------------------------------------------------------------------
CREATE TABLE email_templates (
    template_key VARCHAR(40)  NOT NULL PRIMARY KEY,
    subject      VARCHAR(200) NOT NULL,
    body         TEXT         NOT NULL,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE email_log (
    log_id          INT AUTO_INCREMENT PRIMARY KEY,
    recipient_email VARCHAR(100) NOT NULL,
    recipient_type  ENUM('CLIENT','AGENT','TEST') NOT NULL,
    subject         VARCHAR(200) NOT NULL,
    body            TEXT         NOT NULL,
    order_id        INT          NULL,
    status          ENUM('SENT','FAILED','SIMULATED') NOT NULL,
    error_message   VARCHAR(255) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_log_order FOREIGN KEY (order_id)
        REFERENCES orders(order_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_email_log_status ON email_log(status);

-- ---------------------------------------------------------------------
-- Default administrator so you can log in the first time.
--   username: admin     password: admin123   (change it after first login!)
-- Passwords are stored as salted PBKDF2 hashes, never as plain text.
-- ---------------------------------------------------------------------
INSERT INTO users (username, password_hash, salt, full_name, role, active) VALUES
('admin', '+LiUkl2MjovYJH/QGLiVjVA3F3LwpoyYx0Tes+zoKT0=', 'FyLPylflvP8iflgRYKmxkg==', 'System Administrator', 'ADMIN', 1);
