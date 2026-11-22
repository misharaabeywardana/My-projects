-- =====================================================================
--  EcoNest - Sample data for testing and demonstrations
--  Import AFTER schema.sql. All e-mail addresses use example.com
--  (a reserved test domain) so no real person is ever e-mailed.
--  Sample orders are dated October and November 2026: choose those
--  months on the Reports screen to see them.
-- =====================================================================

USE econest_db;

-- Staff user:  username staff / password staff123
INSERT INTO users (username, password_hash, salt, full_name, role, active) VALUES
('staff', 'uFU15TpJIViGxJdG+JIDELBvY2laX1GPdTEw1ZSRMW0=', 'ZTAxeUPM2qrtAbLnc3jokw==', 'Shop Staff Member', 'STAFF', 1);

-- Products (Task 1)
INSERT INTO products (product_id, name, category, description, price, eco_rating, active, created_at) VALUES
(1,  'Snake Plant',                   'PLANT',      'Hardy air-purifying plant, low light tolerant',   1500.00, 5, 1, '2026-09-01 09:00:00'),
(2,  'Peace Lily',                    'PLANT',      'Flowering indoor plant, likes shade',             1850.00, 4, 1, '2026-09-01 09:00:00'),
(3,  'Money Plant (Pothos)',          'PLANT',      'Fast-growing trailing plant',                      950.00, 5, 1, '2026-09-01 09:00:00'),
(4,  'Areca Palm',                    'PLANT',      'Large palm for offices and living rooms',         3200.00, 4, 1, '2026-09-01 09:00:00'),
(5,  'ZZ Plant',                      'PLANT',      'Glossy leaves, survives irregular watering',      2400.00, 5, 1, '2026-09-01 09:00:00'),
(6,  'Terracotta Pot 8in',            'PLANTER',    'Hand-made local clay pot',                         950.00, 5, 1, '2026-09-01 09:00:00'),
(7,  'Recycled Plastic Planter 10in', 'PLANTER',    'Made from 100% recycled plastic',                 1200.00, 4, 1, '2026-09-01 09:00:00'),
(8,  'Coconut Coir Hanging Basket',   'PLANTER',    'Biodegradable coir liner with jute rope',         1450.00, 5, 1, '2026-09-01 09:00:00'),
(9,  'Self-Watering Ceramic Pot',     'PLANTER',    'Water reservoir lasts up to 2 weeks',             2750.00, 3, 1, '2026-09-01 09:00:00'),
(10, 'Herb Garden Starter Kit',       'GARDEN_KIT', 'Basil, mint and gotukola seeds with coir pots',   4500.00, 5, 1, '2026-09-01 09:00:00'),
(11, 'Balcony Vegetable Kit',         'GARDEN_KIT', 'Chilli, tomato and leafy greens for small spaces',6200.00, 4, 1, '2026-09-01 09:00:00'),
(12, 'Desk Succulent Kit',            'GARDEN_KIT', 'Three succulents, gravel and a bamboo tray',      3800.00, 4, 1, '2026-09-01 09:00:00');

-- Price history (additional feature - Senuji)
INSERT INTO product_price_history (product_id, old_price, new_price, changed_by, changed_at) VALUES
(1, 1350.00, 1500.00, 'admin', '2026-09-30 10:00:00'),
(4, 2950.00, 3200.00, 'admin', '2026-10-15 11:20:00');

-- Stock levels (Task 3). Peace Lily, Areca Palm and Balcony Kit are low; Coir Basket is out of stock.
INSERT INTO inventory (product_id, quantity_on_hand, reorder_level, updated_at) VALUES
(1, 42, 10, '2026-11-13 08:45:00'),
(2,  8, 10, '2026-11-12 15:30:00'),
(3, 60, 15, '2026-11-06 10:00:00'),
(4,  4,  5, '2026-10-28 11:30:00'),
(5, 25,  8, '2026-11-10 13:45:00'),
(6, 70, 20, '2026-11-12 15:30:00'),
(7, 35, 10, '2026-10-28 11:30:00'),
(8,  0,  6, '2026-11-13 08:45:00'),
(9, 12,  5, '2026-10-12 09:40:00'),
(10, 18, 6, '2026-11-03 14:20:00'),
(11,  5, 5, '2026-11-03 14:20:00'),
(12, 22, 6, '2026-11-11 09:10:00');

INSERT INTO suppliers (supplier_id, name, phone, email) VALUES
(1, 'GreenLeaf Nurseries (Pvt) Ltd', '0112345678', 'sales@greenleaf.example.com'),
(2, 'Kelani Clay Works',             '0332223344', 'orders@kelaniclay.example.com'),
(3, 'Island Eco Kits',               '0812233445', 'hello@islandecokits.example.com');

INSERT INTO stock_in (product_id, supplier_id, quantity, unit_cost, received_date, note, recorded_by) VALUES
(1,  1, 30,  800.00, '2026-10-02', 'Monthly nursery delivery', 'admin'),
(3,  1, 40,  450.00, '2026-10-02', 'Monthly nursery delivery', 'admin'),
(6,  2, 50,  500.00, '2026-10-09', NULL,                       'staff'),
(10, 3, 12, 2600.00, '2026-10-21', NULL,                       'staff'),
(5,  1, 20, 1300.00, '2026-11-02', 'Monthly nursery delivery', 'admin'),
(6,  2, 30,  500.00, '2026-11-05', 'Second batch of pots',     'staff'),
(12, 3, 15, 2100.00, '2026-11-08', NULL,                       'admin');

-- Clients (Task 2)
INSERT INTO clients (client_id, client_type, name, company_name, phone, email, address, created_at) VALUES
(1, 'HOUSEHOLD', 'Nimali Perera',          NULL,                    '0771234567', 'nimali.perera@example.com',        '45 Flower Road, Colombo 07',            '2026-10-01 09:30:00'),
(2, 'OFFICE',    'Ruwan Jayasinghe',       'Bluewave Technologies', '0112567890', 'facilities@bluewave.example.com',  'Level 5, 120 Union Place, Colombo 02',  '2026-10-10 14:00:00'),
(3, 'HOUSEHOLD', 'Fathima Rizwan',         NULL,                    '0759876543', 'fathima.rizwan@example.com',       '12/3 Temple Lane, Kandy',               '2026-10-18 11:15:00'),
(4, 'OFFICE',    'Dilani Fernando',        'Serendib Insurance',    '0114455667', 'admin@serendib-ins.example.com',   '88 Galle Road, Colombo 03',             '2026-10-25 10:00:00'),
(5, 'HOUSEHOLD', 'Arjun Krishnan',         NULL,                    '0763456789', 'arjun.krishnan@example.com',       '7 Lake Drive, Kurunegala',              '2026-11-02 16:40:00'),
(6, 'HOUSEHOLD', 'Sachini Wickramasinghe', NULL,                    '0702345678', 'sachini.w@example.com',            '21 Hill Street, Galle',                 '2026-11-09 12:10:00');

-- Delivery agents (Task 4). Tharindu is marked unavailable.
INSERT INTO delivery_agents (agent_id, name, phone, email, vehicle_type, vehicle_number, active) VALUES
(1, 'Kasun Silva',      '0771112233', 'kasun.silva@example.com',      'MOTORBIKE',     'WP BCD-4521', 1),
(2, 'Mohamed Imran',    '0712223344', 'imran.mohamed@example.com',    'THREE_WHEELER', 'WP QA-7789',  1),
(3, 'Pradeep Kumara',   '0723334455', 'pradeep.kumara@example.com',   'VAN',           'WP LH-3301',  1),
(4, 'Tharindu Bandara', '0784445566', 'tharindu.bandara@example.com', 'BICYCLE',       'CP 1122-B',   0);

-- Orders (Task 5). total_amount = sum of the line totals below.
INSERT INTO orders (order_id, order_ref, client_id, order_date, status, total_amount, notes, created_by) VALUES
(1,  'ECN-20261005-00001', 1, '2026-10-05 10:15:00', 'DELIVERED',  4900.00, NULL,                         'staff'),
(2,  'ECN-20261012-00002', 2, '2026-10-12 09:40:00', 'DELIVERED', 27600.00, 'Deliver to reception',       'admin'),
(3,  'ECN-20261020-00003', 3, '2026-10-20 16:05:00', 'CANCELLED',  4500.00, 'Client changed her mind',    'staff'),
(4,  'ECN-20261028-00004', 4, '2026-10-28 11:30:00', 'DELIVERED', 17600.00, NULL,                         'admin'),
(5,  'ECN-20261103-00005', 5, '2026-11-03 14:20:00', 'DELIVERED', 10700.00, NULL,                         'staff'),
(6,  'ECN-20261106-00006', 1, '2026-11-06 10:00:00', 'DELIVERED',  4300.00, NULL,                         'staff'),
(7,  'ECN-20261110-00007', 2, '2026-11-10 13:45:00', 'DISPATCHED',33500.00, 'New floor - 10 desks',       'admin'),
(8,  'ECN-20261111-00008', 6, '2026-11-11 09:10:00', 'PROCESSING', 7600.00, NULL,                         'staff'),
(9,  'ECN-20261112-00009', 3, '2026-11-12 15:30:00', 'PENDING',    2800.00, NULL,                         'staff'),
(10, 'ECN-20261112-00010', 4, '2026-11-12 17:00:00', 'CANCELLED', 12400.00, 'Duplicate order',            'admin'),
(11, 'ECN-20261113-00011', 5, '2026-11-13 08:45:00', 'PENDING',    5900.00, 'Gift - please wrap',         'staff');

INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total) VALUES
(1,  1,  2, 1500.00,  3000.00),
(1,  6,  2,  950.00,  1900.00),
(2,  2,  6, 1850.00, 11100.00),
(2,  9,  6, 2750.00, 16500.00),
(3, 10,  1, 4500.00,  4500.00),
(4,  4,  4, 3200.00, 12800.00),
(4,  7,  4, 1200.00,  4800.00),
(5, 11,  1, 6200.00,  6200.00),
(5, 10,  1, 4500.00,  4500.00),
(6,  3,  3,  950.00,  2850.00),
(6,  8,  1, 1450.00,  1450.00),
(7,  5, 10, 2400.00, 24000.00),
(7,  6, 10,  950.00,  9500.00),
(8, 12,  2, 3800.00,  7600.00),
(9,  2,  1, 1850.00,  1850.00),
(9,  6,  1,  950.00,   950.00),
(10, 11, 2, 6200.00, 12400.00),
(11, 1,  2, 1500.00,  3000.00),
(11, 8,  2, 1450.00,  2900.00);

-- Deliveries (Task 6)
INSERT INTO deliveries (order_id, agent_id, scheduled_at, status, assigned_at, delivered_at, notes) VALUES
(1, 1, '2026-10-06 10:00:00', 'DELIVERED',  '2026-10-05 11:00:00', '2026-10-06 10:40:00', NULL),
(2, 3, '2026-10-13 09:00:00', 'DELIVERED',  '2026-10-12 10:00:00', '2026-10-13 10:15:00', 'Use service lift'),
(4, 3, '2026-10-29 14:00:00', 'DELIVERED',  '2026-10-28 12:00:00', '2026-10-29 15:05:00', NULL),
(5, 1, '2026-11-04 11:00:00', 'DELIVERED',  '2026-11-03 15:00:00', '2026-11-04 11:30:00', NULL),
(6, 2, '2026-11-07 09:30:00', 'DELIVERED',  '2026-11-06 10:30:00', '2026-11-07 10:10:00', NULL),
(7, 3, '2026-11-11 10:00:00', 'IN_TRANSIT', '2026-11-10 14:00:00', NULL,                  'Two trips may be needed'),
(8, 1, '2026-11-14 10:00:00', 'ASSIGNED',   '2026-11-11 09:30:00', NULL,                  'Call before arriving');

-- E-mail log (Tasks 8 and 9). One FAILED row shows the Resend feature.
INSERT INTO email_log (recipient_email, recipient_type, subject, body, order_id, status, error_message, created_at) VALUES
('nimali.perera@example.com', 'CLIENT', 'Your EcoNest order ECN-20261106-00006 is on its way!',
'Dear Nimali Perera,

Good news - your order ECN-20261106-00006 has been dispatched.

Thank you for growing greener with EcoNest!', 6, 'FAILED', 'Could not send e-mail: Connection refused', '2026-11-07 09:00:00'),
('pradeep.kumara@example.com', 'AGENT', 'New delivery assigned: ECN-20261110-00007',
'Hello Pradeep Kumara,

A new EcoNest delivery has been assigned to you.

Order reference : ECN-20261110-00007
Deliver to      : Ruwan Jayasinghe (Bluewave Technologies)

EcoNest Operations', 7, 'SIMULATED', NULL, '2026-11-10 14:00:00'),
('facilities@bluewave.example.com', 'CLIENT', 'Your EcoNest order ECN-20261110-00007 is on its way!',
'Dear Ruwan Jayasinghe,

Good news - your order ECN-20261110-00007 has been dispatched.

Thank you for growing greener with EcoNest!', 7, 'SIMULATED', NULL, '2026-11-11 09:45:00'),
('kasun.silva@example.com', 'AGENT', 'New delivery assigned: ECN-20261111-00008',
'Hello Kasun Silva,

A new EcoNest delivery has been assigned to you.

Order reference : ECN-20261111-00008
Deliver to      : Sachini Wickramasinghe

EcoNest Operations', 8, 'SIMULATED', NULL, '2026-11-11 09:30:00');
