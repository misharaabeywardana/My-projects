<?php
/* =========================================
   YUMMYPREP - ADMIN DASHBOARD STATS API
   ========================================= */

require_once __DIR__ . '/helpers.php';

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

// Ensure Admin Access
if (empty($_SESSION['user_id']) || ($_SESSION['user_type'] ?? '') !== 'admin') {
    fail('Admin access required.', 403);
}

$pdo = db();

try {
    // 1. Total Users Count
    $userStmt = $pdo->query('SELECT COUNT(*) AS total_users FROM users');
    $totalUsers = (int) ($userStmt->fetch()['total_users'] ?? 0);

    // 2. Total Meals Count
    $mealStmt = $pdo->query('SELECT COUNT(*) AS total_meals FROM meals');
    $totalMeals = (int) ($mealStmt->fetch()['total_meals'] ?? 0);

    // 3. Total Orders & Total Revenue
    $orderStmt = $pdo->query('SELECT COUNT(*) AS total_orders, COALESCE(SUM(total), 0) AS revenue FROM orders');
    $orderTotals = $orderStmt->fetch();

    // 4. Status Breakdown
    $statusSummary = [
        'Preparing'        => 0,
        'Packed'           => 0,
        'Out for Delivery' => 0,
        'Delivered'        => 0
    ];

    $statusStmt = $pdo->query('SELECT status, COUNT(*) AS count FROM orders GROUP BY status');
    while ($row = $statusStmt->fetch()) {
        $st = $row['status'];
        if (array_key_exists($st, $statusSummary)) {
            $statusSummary[$st] = (int) $row['count'];
        }
    }

    ok([
        'totalUsers'     => $totalUsers,
        'totalMeals'     => $totalMeals,
        'totalOrders'    => (int) ($orderTotals['total_orders'] ?? 0),
        'totalRevenue'   => 'Rs. ' . number_format((float) ($orderTotals['revenue'] ?? 0), 2),
        'revenueValue'   => (float) ($orderTotals['revenue'] ?? 0),
        'statusSummary'  => $statusSummary
    ]);

} catch (Exception $e) {
    fail('Failed to fetch dashboard stats: ' . $e->getMessage(), 500);
}