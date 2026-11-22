<?php
/* =========================================
   YUMMYPREP - CONFIGURATION
   Edit the DB values to match your WAMP setup.
   ========================================= */

// ---- Database (WAMP defaults: user "root", empty password) ----
define('DB_HOST', 'localhost');
define('DB_NAME', 'yummyprep');
define('DB_USER', 'root');
define('DB_PASS', '');

// ---- Application settings ----
define('APP_TIMEZONE', 'Asia/Colombo');
define('DELIVERY_FEE', 250.00);            // must match the Rs. 250 used by cart.js / checkout.js
define('REMEMBER_ME_DAYS', 30);            // session cookie lifetime when "Remember me" is ticked
define('MIN_PASSWORD_LENGTH', 8);

// Used to keep login timing the same whether or not the email exists
define('DUMMY_HASH', '$2y$10$ZECJ.h0IYRlGs/YeNS55F.Y9AlAVafJbdBGc1jiWdWh967EDeAKPi');

// Order statuses, in tracking order (orderTracking.js uses the same list)
define('ORDER_STATUSES', ['Confirmed', 'Preparing', 'Packed', 'Out for Delivery', 'Delivered']);
define('DEFAULT_ORDER_STATUS', 'Preparing');

// Meal categories (must match meals.js)
define('MEAL_CATEGORIES', ['Rice Bowl', 'Salad', 'Grain Bowl', 'Bread-Based', 'Beverage']);

// FAQ categories (must match faqManagement.html)
define('FAQ_CATEGORIES', ['Orders', 'Delivery', 'Customization', 'Account', 'General']);

date_default_timezone_set(APP_TIMEZONE);
