-- =========================================
-- YUMMYPREP DATABASE (MySQL / MariaDB)
-- Import with phpMyAdmin (WAMP) -> Import, or:  mysql -u root < yummyprep.sql
-- =========================================

CREATE DATABASE IF NOT EXISTS yummyprep CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE yummyprep;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS order_items, orders, contact_messages, faqs, promotions, meals, admins, users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------- Customers ----------
CREATE TABLE users (
    id               INT UNSIGNED NOT NULL AUTO_INCREMENT,
    full_name        VARCHAR(100) NOT NULL,
    email            VARCHAR(150) NOT NULL,
    phone            VARCHAR(20)  NOT NULL,
    delivery_address VARCHAR(255) NULL,
    password_hash    VARCHAR(255) NOT NULL,
    is_active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

-- ---------- Admin ----------
CREATE TABLE admins (
    id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_admins_email (email)
) ENGINE=InnoDB;

-- ---------- Meals ----------
CREATE TABLE meals (
    id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
    meal_code    VARCHAR(20)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    category     ENUM('Rice Bowl','Salad','Grain Bowl','Bread-Based','Beverage') NOT NULL,
    description  TEXT         NOT NULL,
    price        DECIMAL(10,2) NOT NULL,
    image_url    VARCHAR(255) NOT NULL DEFAULT '../images/chickenBowl1.jpeg',
    calories     INT UNSIGNED NOT NULL DEFAULT 0,
    protein_g    INT UNSIGNED NOT NULL DEFAULT 0,
    carbs_g      INT UNSIGNED NOT NULL DEFAULT 0,
    fat_g        INT UNSIGNED NOT NULL DEFAULT 0,
    ingredients  TEXT         NULL,              -- comma separated
    is_available TINYINT(1)   NOT NULL DEFAULT 1,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_meals_code (meal_code),
    KEY idx_meals_category (category)
) ENGINE=InnoDB;

-- ---------- Promotions ----------
CREATE TABLE promotions (
    id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
    title          VARCHAR(100) NOT NULL,
    code           VARCHAR(30)  NOT NULL,
    description    VARCHAR(500) NULL,
    discount_type  ENUM('Percentage','Fixed') NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    expiry_date    DATE         NOT NULL,
    status         ENUM('Active','Inactive') NOT NULL DEFAULT 'Active',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_promo_code (code)
) ENGINE=InnoDB;

-- ---------- Orders ----------
CREATE TABLE orders (
    id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_number   VARCHAR(20)  NOT NULL,
    user_id        INT UNSIGNED NULL,               -- NULL = guest checkout / deleted user
    first_name     VARCHAR(50)  NOT NULL,
    last_name      VARCHAR(50)  NOT NULL,
    email          VARCHAR(150) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    address        VARCHAR(255) NOT NULL,
    delivery_day   ENUM('today','tomorrow') NOT NULL DEFAULT 'today',
    payment_method VARCHAR(40)  NOT NULL DEFAULT 'Cash on Delivery',
    subtotal       DECIMAL(10,2) NOT NULL,
    delivery_fee   DECIMAL(10,2) NOT NULL DEFAULT 0,
    discount       DECIMAL(10,2) NOT NULL DEFAULT 0,
    promo_code     VARCHAR(30)  NULL,
    total          DECIMAL(10,2) NOT NULL,
    status         ENUM('Confirmed','Preparing','Packed','Out for Delivery','Delivered') NOT NULL DEFAULT 'Preparing',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_order_number (order_number),
    KEY idx_orders_user (user_id),
    KEY idx_orders_status (status),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE order_items (
    id        INT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id  INT UNSIGNED NOT NULL,
    meal_id   INT UNSIGNED NULL,                    -- NULL = customised meal / meal later deleted
    item_name VARCHAR(100) NOT NULL,
    category  VARCHAR(30)  NOT NULL,
    price     DECIMAL(10,2) NOT NULL,
    quantity  INT UNSIGNED NOT NULL,
    image     VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_items_order (order_id),
    CONSTRAINT fk_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_items_meal  FOREIGN KEY (meal_id)  REFERENCES meals(id)  ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------- FAQs ----------
CREATE TABLE faqs (
    id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    question   VARCHAR(255) NOT NULL,
    answer     TEXT         NOT NULL,
    category   ENUM('Orders','Delivery','Customization','Account','General') NOT NULL DEFAULT 'General',
    status     ENUM('Active','Inactive') NOT NULL DEFAULT 'Active',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ---------- Contact Us messages ----------
CREATE TABLE contact_messages (
    id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL,
    phone      VARCHAR(20)  NULL,
    subject    VARCHAR(100) NOT NULL,
    message    TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB;


-- =========================================
-- SEED DATA
-- =========================================

-- Default web master login:  admin@yummyprep.lk  /  Admin@123   (CHANGE THIS after first login)
INSERT INTO admins (name, email, password_hash) VALUES
('Web Master', 'admin@yummyprep.lk', '$2y$10$hyNHnXplMJ6H5bpmqx/GyuzmoWJOoq8o3dPJ/SS70LpPWw08B5viW');

INSERT INTO meals (meal_code, name, category, description, price, image_url, calories, protein_g, carbs_g, fat_g, ingredients) VALUES
('MEAL101', 'Grilled Chicken Rice Bowl', 'Rice Bowl',   'Grilled chicken over steamed rice with fresh vegetables and a light house sauce.', 1450.00, '../images/riceBowl1.jpeg', 520, 38, 55, 15, 'Grilled chicken, Steamed rice, Broccoli, Carrot, House sauce'),
('MEAL102', 'Teriyaki Veggie Rice Bowl',  'Rice Bowl',   'Teriyaki glazed vegetables and tofu on fluffy rice.',                              1250.00, '../images/riceBowl2.jpeg', 480, 22, 70, 12, 'Tofu, Steamed rice, Bell pepper, Mushroom, Teriyaki glaze'),
('MEAL201', 'Garden Fresh Salad',         'Salad',       'Crisp greens, cherry tomatoes, cucumber and a citrus vinaigrette.',                 980.00, '../images/salad1.jpeg',    240, 8, 25, 12, 'Lettuce, Cherry tomato, Cucumber, Olives, Citrus vinaigrette'),
('MEAL202', 'Protein Power Salad',        'Salad',       'Grilled chicken, boiled egg, avocado and mixed leaves.',                           1350.00, '../images/salad2.jpeg',    410, 34, 18, 22, 'Grilled chicken, Boiled egg, Avocado, Mixed leaves, Olive oil'),
('MEAL301', 'Quinoa Harvest Bowl',        'Grain Bowl',  'Quinoa with roasted vegetables, chickpeas and tahini dressing.',                   1400.00, '../images/grainBowl2.jpeg',  460, 18, 60, 14, 'Quinoa, Chickpeas, Roasted pumpkin, Spinach, Tahini'),
('MEAL401', 'Chicken Club Sandwich',      'Bread-Based', 'Whole-grain bread, grilled chicken, lettuce, tomato and light mayo.',               1100.00, '../images/sandwich2.jpeg',  430, 30, 42, 14, 'Whole-grain bread, Grilled chicken, Lettuce, Tomato, Light mayo'),
('MEAL402', 'Veggie Delight Sandwich',    'Bread-Based', 'Grilled vegetables, hummus and rocket on toasted multigrain bread.',                950.00, '../images/sandwich1.jpeg',  360, 12, 48, 12, 'Multigrain bread, Hummus, Zucchini, Bell pepper, Rocket'),
('MEAL501', 'Fresh Orange Juice',         'Beverage',    'Cold-pressed orange juice with no added sugar.',                                    450.00, '../images/juice2.jpeg',    120, 2, 28, 0,  'Fresh oranges'),
('MEAL502', 'Green Detox Juice',          'Beverage',    'Cucumber, green apple, celery and lime.',                                           520.00, '../images/juice5.jpeg',     90, 1, 21, 0,  'Cucumber, Green apple, Celery, Lime');

INSERT INTO promotions (title, code, description, discount_type, discount_value, expiry_date, status) VALUES
('Welcome Offer',   'WELCOME10', '10% off your order.',          'Percentage', 10.00, '2026-12-31', 'Active'),
('Rs. 200 Off',     'SAVE200',   'Rs. 200 off orders.',          'Fixed',     200.00, '2026-12-31', 'Active');

INSERT INTO faqs (question, answer, category, status) VALUES
('How do I place an order?',           'Browse our meals, add your favourites to the cart, then proceed to checkout and confirm your details.', 'Orders',        'Active'),
('Can I track my order?',              'Yes. Log in and open Order Tracking, or use your order number on the tracking page.',                   'Orders',        'Active'),
('What time do you deliver?',          'Orders placed for today are delivered the same day; you can also schedule delivery for tomorrow.',     'Delivery',      'Active'),
('How much is the delivery fee?',      'A flat delivery fee of Rs. 250 applies to every order.',                                               'Delivery',      'Active'),
('Can I customise my meal?',           'Yes! Use the Customize page to choose your base, protein and toppings.',                               'Customization', 'Active'),
('How do I update my profile?',        'Log in, open your Profile page and click Edit Profile.',                                                'Account',       'Active');
