# EcoNest – Database Design and JDBC Implementation

**Database name:** `econest_db`  **Server:** MySQL-compatible server in XAMPP (XAMPP ships MariaDB 10.4+, which uses the same SQL and the same JDBC driver)  **Driver:** MySQL Connector/J 8.4.0 (`com.mysql:mysql-connector-j`)

Files in the project:

| File | What it is |
|---|---|
| `database/schema.sql` | Creates the database, all 13 tables, keys, constraints, indexes and the first admin user |
| `database/sample-data.sql` | Optional test data (12 products, 6 clients, 4 agents, 11 orders, deliveries, e-mails) |
| `src/main/java/econest/dao/DBConnection.java` | The JDBC connection class |
| `src/main/java/econest/dao/BaseDao.java` | Shared JDBC code (PreparedStatement, ResultSet, error handling) |
| `src/main/java/econest/dao/*Dao.java` | One DAO class per table group – all the SQL lives here |
| `config/db.properties` | URL, user and password (created automatically; not committed to Git) |

## 1. Tables

| # | Table | Purpose | Primary key | Foreign keys |
|---|---|---|---|---|
| 1 | `users` | Login accounts (Admin panel) | `user_id` | – |
| 2 | `products` | Plants, planters, gardening kits (Task 1) | `product_id` | – |
| 3 | `product_price_history` | Every price change (extra – Senuji) | `history_id` | `product_id` → products (CASCADE) |
| 4 | `clients` | Household and office clients (Task 2) | `client_id` | – |
| 5 | `inventory` | Quantity on hand and reorder level (Task 3) | `product_id` | `product_id` → products (CASCADE) |
| 6 | `suppliers` | Who supplies stock (Task 3) | `supplier_id` | – |
| 7 | `stock_in` | Stock received from suppliers (Task 3) | `stock_in_id` | `product_id` → products, `supplier_id` → suppliers (RESTRICT) |
| 8 | `delivery_agents` | Agents and vehicles (Task 4) | `agent_id` | – |
| 9 | `orders` | Order header: client, date, status, total (Task 5) | `order_id` | `client_id` → clients (RESTRICT) |
| 10 | `order_items` | Order lines: product, qty, unit price, line total (Task 5) | `item_id` | `order_id` → orders (CASCADE), `product_id` → products (RESTRICT) |
| 11 | `deliveries` | Agent assigned to an order, schedule, status (Task 6) | `delivery_id` | `order_id` → orders (UNIQUE, CASCADE), `agent_id` → delivery_agents (RESTRICT) |
| 12 | `email_templates` | Editable e-mail wording (Task 8 extra – Rakshana) | `template_key` | – |
| 13 | `email_log` | Every e-mail sent/failed/simulated (Tasks 8–9, extra – Sandeepa) | `log_id` | `order_id` → orders (SET NULL) |

**Other constraints:** unique `users.username`, `suppliers.name`, `delivery_agents.vehicle_number`, `orders.order_ref`, `deliveries.order_id`; CHECK constraints for price > 0, eco-rating 1–5, quantities ≥ 0 / > 0; ENUM columns for categories and statuses; indexes on `orders(order_date)`, `orders(status)`, `email_log(status)`.

## 2. Relationships (ER summary)

- **products 1 — 1 inventory**: every product has exactly one stock record (created in the same transaction as the product).
- **products 1 — N product_price_history**.
- **suppliers 1 — N stock_in N — 1 products**: each stock-in records which supplier delivered which product.
- **clients 1 — N orders**.
- **orders 1 — N order_items N — 1 products**: this resolves the many-to-many relationship between orders and products. `unit_price` is copied at order time so later price changes do not alter old orders.
- **orders 1 — 0..1 deliveries N — 1 delivery_agents**: an order has at most one delivery; an agent has many.
- **orders 1 — N email_log**.
- `users` and `email_templates` are standalone.

```
clients ─1───N─ orders ─1───N─ order_items ─N───1─ products ─1───1─ inventory
                  │  │                                  │  └─1───N─ product_price_history
                  │  └─1───0..1─ deliveries ─N───1─ delivery_agents
                  └─1───N─ email_log            stock_in ─N───1─ products
                                                stock_in ─N───1─ suppliers
```

**Delete rules chosen on purpose:** a client with orders, a product used in orders, an agent with deliveries and a supplier with stock-in records **cannot be deleted** (RESTRICT) because reports and history depend on them. The application explains this and offers the alternative (products become *inactive*, agents become *unavailable*).

## 3. Status values

| Order status | Meaning | Delivery status | Meaning |
|---|---|---|---|
| PENDING | Saved, stock reserved | ASSIGNED | Agent and time chosen → agent e-mailed |
| PROCESSING | Agent assigned | IN_TRANSIT | Left the shop → order DISPATCHED → client e-mailed |
| DISPATCHED | On the way | DELIVERED | Order DELIVERED |
| DELIVERED | Completed | FAILED | Order back to PROCESSING, can be re-assigned |
| CANCELLED | Stock returned, excluded from revenue | | |

## 4. Complete SQL script – `database/schema.sql`

```sql
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
```

`database/sample-data.sql` contains INSERT statements for every table (see the project folder). After importing both files you can log in as **admin / admin123** (administrator) or **staff / staff123** (staff).

## 5. JDBC connection class – `DBConnection.java`

Location: `src/main/java/econest/dao/DBConnection.java`

```java
package econest.dao;

import econest.util.ConfigFile;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC connection class. Every DAO asks this class for a connection.
 *
 * Settings are read from config/db.properties so the password is not in the
 * source code. XAMPP default: user "root" with an empty password.
 */
public final class DBConnection {

    public static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/econest_db";

    private static final ConfigFile CONFIG = new ConfigFile("db.properties", defaults());
    private static Properties settings;

    private DBConnection() {
    }

    private static Properties defaults() {
        Properties p = new Properties();
        p.setProperty("db.url", DEFAULT_URL);
        p.setProperty("db.user", "root");
        p.setProperty("db.password", "");
        return p;
    }

    private static synchronized Properties settings() {
        if (settings == null) {
            settings = CONFIG.load();
        }
        return settings;
    }

    public static String getUrl() {
        return settings().getProperty("db.url");
    }

    public static String getUser() {
        return settings().getProperty("db.user");
    }

    public static String getPassword() {
        return settings().getProperty("db.password", "");
    }

    public static String getConfigPath() {
        return CONFIG.getPath().toString();
    }

    /** Saves new settings to config/db.properties and uses them immediately. */
    public static synchronized void saveSettings(String url, String user, String password) throws IOException {
        Properties p = new Properties();
        p.setProperty("db.url", url.trim());
        p.setProperty("db.user", user.trim());
        p.setProperty("db.password", password);
        CONFIG.save(p, "EcoNest database settings");
        settings = p;
    }

    /** Opens a new connection. Always close it (use try-with-resources). */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(getUrl(), getUser(), getPassword());
        } catch (SQLException e) {
            throw new DataAccessException(explain(e), e, DataAccessException.Kind.CONNECTION);
        }
    }

    /** Opens and closes a connection; returns a short description of the server. */
    public static String testConnection() {
        try (Connection con = getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            return "Connected to " + meta.getDatabaseProductName() + " "
                    + meta.getDatabaseProductVersion() + " (" + getUrl() + ")";
        } catch (SQLException e) {
            throw new DataAccessException(explain(e), e, DataAccessException.Kind.CONNECTION);
        }
    }

    /** Work that runs inside one database transaction. */
    @FunctionalInterface
    public interface TransactionWork<T> {
        T run(Connection con) throws SQLException;
    }

    /**
     * Runs several SQL statements as one unit: either all succeed (commit)
     * or none are saved (rollback). Used for orders, stock-in, deliveries.
     */
    public static <T> T inTransaction(TransactionWork<T> work) {
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                T result = work.run(con);
                con.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                if (e instanceof SQLException) {
                    throw BaseDao.translate((SQLException) e);
                }
                throw (RuntimeException) e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw BaseDao.translate(e);
        }
    }

    /** Turns common connection problems into instructions a beginner can follow. */
    static String explain(SQLException e) {
        String msg = String.valueOf(e.getMessage());
        String lower = msg.toLowerCase();
        if (lower.contains("no suitable driver")) {
            return "The MySQL JDBC driver was not found.\n"
                    + "Add mysql-connector-j to the project (see the installation guide, step B).";
        }
        if (lower.contains("communications link failure") || lower.contains("connection refused")) {
            return "Cannot reach the MySQL server at " + getUrl() + ".\n"
                    + "Open the XAMPP Control Panel and click Start next to MySQL.";
        }
        if (lower.contains("unknown database")) {
            return "The database econest_db does not exist yet.\n"
                    + "Open phpMyAdmin and import database/schema.sql, then sample-data.sql.";
        }
        if (lower.contains("access denied")) {
            return "MySQL refused the username/password.\n"
                    + "Check db.user and db.password in " + getConfigPath();
        }
        return "Database error: " + msg;
    }
}
```

`config/db.properties` (created automatically the first time the program runs):

```
db.url=jdbc:mysql://localhost:3306/econest_db
db.user=root
db.password=
```

## 6. CRUD operations

All SQL uses `PreparedStatement` with `?` parameters (protects against SQL injection). `BaseDao` contains the repeated JDBC code once:

| Helper in `BaseDao` | Used for | JDBC calls inside |
|---|---|---|
| `insert(sql, params…)` | **C**reate – returns the new AUTO_INCREMENT id | `prepareStatement(sql, RETURN_GENERATED_KEYS)`, `executeUpdate()`, `getGeneratedKeys()` |
| `query(sql, mapper, params…)` / `queryOne(...)` | **R**ead – turns each row into a Java object | `executeQuery()`, `ResultSet.next()`, `getString/getInt/getBigDecimal/getObject` |
| `update(sql, params…)` | **U**pdate and **D**elete | `executeUpdate()` |
| `bind(...)` | Puts Java values into `?` | `setString`, `setInt`, `setBigDecimal`, `setObject(LocalDateTime)`, `setNull` |
| `translate(SQLException)` | Error handling | Converts MySQL error 1062 (duplicate), 1451 (record in use), 1452 (missing parent) and connection errors into clear messages |

Example – the four CRUD operations for clients (`ClientDao`, which implements the `CrudDao<Client>` interface):

```java
public int insert(Client c) {                        // CREATE
    return insert("INSERT INTO clients (client_type, name, company_name, phone, email, address, created_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
            c.getClientType(), c.getName(), c.getCompanyName(), c.getPhone(), c.getEmail(), c.getAddress(),
            LocalDateTime.now().withNano(0));
}

public List<Client> findAll() {                     // READ
    return query(SELECT + "ORDER BY name", this::map);   // map() copies each column into a Client object
}

public void update(Client c) {                       // UPDATE
    update("UPDATE clients SET client_type = ?, name = ?, company_name = ?, phone = ?, email = ?, address = ? "
            + "WHERE client_id = ?", c.getClientType(), c.getName(), c.getCompanyName(), c.getPhone(),
            c.getEmail(), c.getAddress(), c.getId());
}

public void delete(int id) {                         // DELETE
    update("DELETE FROM clients WHERE client_id = ?", id);
}
```

**Transactions.** Operations that change several tables use `DBConnection.inTransaction(...)` so they succeed or fail as one unit:

- *Add product* → insert into `products` + `inventory`.
- *Place order* → insert `orders` + `order_items`, reduce `inventory` (`UPDATE … WHERE quantity_on_hand >= ?`, so stock can never go negative even with two users). Not enough stock → everything is rolled back.
- *Cancel order* → return stock, set status CANCELLED, close the open delivery.
- *Record stock-in* → insert `stock_in` + increase `inventory`.
- *Assign agent / change delivery status* → `deliveries` + `orders` status kept in step.
- *Change price* → update `products` + insert `product_price_history`.

## 7. How each Java class talks to the database

The application is layered. **Screens never contain SQL.** Each layer only talks to the one below it:

```
Swing screen (ui)  →  Service (business rules, validation)  →  DAO (SQL)  →  DBConnection (JDBC)  →  MySQL
     ProductPanel  →  ProductService                        →  ProductDao →  DBConnection
```

| Swing screen | Service class | DAO class(es) | Tables |
|---|---|---|---|
| `LoginDialog`, `UserPanel`, `ChangePasswordDialog` | `UserService` | `UserDao` | users |
| `ProductPanel`, `ProductDialog`, `PriceHistoryDialog` | `ProductService` | `ProductDao`, `InventoryDao` | products, product_price_history, inventory |
| `ClientPanel`, `ClientDialog`, `ClientHistoryDialog` | `ClientService` | `ClientDao`, `OrderDao` | clients, orders, order_items |
| `InventoryPanel`, `StockInDialog`, `SupplierPanel` | `InventoryService` | `InventoryDao`, `StockInDao`, `SupplierDao`, `ProductDao` | inventory, stock_in, suppliers, products |
| `AgentPanel`, `AgentDialog` | `AgentService` | `AgentDao` | delivery_agents, deliveries |
| `OrderPanel`, `NewOrderDialog`, `OrderDetailsDialog` | `OrderService` | `OrderDao`, `ProductDao`, `InventoryDao`, `DeliveryDao`, `ClientDao` | orders, order_items, inventory |
| `DeliveryPanel`, `AssignDeliveryDialog` | `DeliveryService` (+ `NotificationService`) | `DeliveryDao`, `OrderDao`, `AgentDao` | deliveries, orders |
| `ReportPanel`, `DashboardPanel` | `ReportService` | `ReportDao`, `InventoryDao`, `StockInDao` | all (read only) |
| `NotificationPanel`, `SettingsPanel` | `NotificationService` | `EmailLogDao`, `EmailTemplateDao` | email_log, email_templates |

Every DAO method opens a connection with `DBConnection.getConnection()` inside *try-with-resources*, so the connection, statement and result set are always closed, even after an error.

## 8. Database error handling

| Situation | What the user sees |
|---|---|
| MySQL not started in XAMPP | "Cannot reach the MySQL server… Open the XAMPP Control Panel and click Start next to MySQL." |
| Database not imported | "The database econest_db does not exist yet. Open phpMyAdmin and import database/schema.sql…" |
| Wrong password in `db.properties` | "MySQL refused the username/password. Check db.user and db.password in …" |
| Driver JAR missing | "The MySQL JDBC driver was not found. Add mysql-connector-j…" |
| Duplicate value (e.g. vehicle number) | Clear validation message before saving; error 1062 translated as a fallback |
| Deleting a record that is in use | Friendly explanation (error 1451) |
| Error in the middle of an order | Transaction rolled back – no half-saved order, stock unchanged |

`DataAccessException` (unchecked) carries the friendly message from the DAO layer to the screen, and `ValidationException` carries business-rule messages. `UiUtil.handle()` shows them in a dialog.
