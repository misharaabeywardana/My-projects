<?php
/* =========================================
   YUMMYPREP - SHARED HELPERS
   ========================================= */

require_once __DIR__ . '/config.php';
require_once __DIR__ . '/db.php';

ini_set('display_errors', '0');
ini_set('log_errors', '1');
error_reporting(E_ALL);

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('Cache-Control: no-store');


/* ---------- Sessions ---------- */

if (session_status() === PHP_SESSION_NONE) {
    ini_set('session.gc_maxlifetime', (string) (REMEMBER_ME_DAYS * 86400));
    session_name('YUMMYPREPSESS');
    session_set_cookie_params([
        'lifetime' => 0,
        'path'     => '/',
        'secure'   => !empty($_SERVER['HTTPS']),
        'httponly' => true,
        'samesite' => 'Lax',
    ]);
    session_start();
}


/* ---------- Response helpers ---------- */

function respond($status, $message = '', $data = null, $code = 200)
{
    http_response_code($code);
    echo json_encode(
        ['status' => $status, 'message' => $message, 'data' => $data],
        JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PARTIAL_OUTPUT_ON_ERROR
    );
    exit;
}

function ok($data = null, $message = '')
{
    respond('success', $message, $data, 200);
}

function fail($message, $code = 400)
{
    respond('error', $message, null, $code);
}

set_exception_handler(function ($e) {
    error_log('[YummyPrep] ' . $e->getMessage() . ' @ ' . $e->getFile() . ':' . $e->getLine());
    fail('Server error. Please try again later.', 500);
});


/* ---------- Request helpers ---------- */

function get_action()
{
    return (string) ($_POST['action'] ?? $_GET['action'] ?? '');
}

function require_method($method)
{
    if ($_SERVER['REQUEST_METHOD'] !== $method) {
        fail('Method not allowed.', 405);
    }

    if ($method === 'POST' && !empty($_SERVER['HTTP_ORIGIN'])) {
        $originHost = parse_url($_SERVER['HTTP_ORIGIN'], PHP_URL_HOST);
        $originPort = parse_url($_SERVER['HTTP_ORIGIN'], PHP_URL_PORT);
        $origin     = $originHost . ($originPort ? ':' . $originPort : '');

        if (strcasecmp($origin, (string) ($_SERVER['HTTP_HOST'] ?? '')) !== 0) {
            fail('Cross-site request blocked.', 403);
        }
    }
}

function clean_text($value, $max = 255, $multiline = false)
{
    if (!is_string($value) && !is_numeric($value)) {
        return '';
    }

    $value = strip_tags((string) $value);
    $value = str_replace(['<', '>', '"'], ['', '', "'"], $value);
    $value = preg_replace('/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/u', '', $value) ?? '';

    if (!$multiline) {
        $value = preg_replace('/\s+/u', ' ', $value) ?? '';
    }

    return mb_substr(trim($value), 0, $max);
}

function post_text($key, $max = 255, $multiline = false)
{
    return clean_text($_POST[$key] ?? '', $max, $multiline);
}

function valid_email($email)
{
    return strlen($email) <= 150 && filter_var($email, FILTER_VALIDATE_EMAIL) !== false;
}

function valid_phone($phone)
{
    return (bool) preg_match('/^[0-9+\-\s()]{7,20}$/', $phone);
}

function valid_date($date)
{
    $d = DateTime::createFromFormat('Y-m-d', $date);
    return $d && $d->format('Y-m-d') === $date;
}

function iso_date($mysqlDate)
{
    if (!$mysqlDate) {
        return null;
    }
    $ts = strtotime($mysqlDate);
    return $ts ? date('c', $ts) : null;
}


/* ---------- Authentication guards ---------- */

function require_user()
{
    if (($_SESSION['user_type'] ?? '') !== 'customer' || empty($_SESSION['user_id'])) {
        fail('Please log in to continue.', 401);
    }

    $stmt = db()->prepare('SELECT is_active FROM users WHERE id = ?');
    $stmt->execute([$_SESSION['user_id']]);
    $row = $stmt->fetch();

    if (!$row || (int) $row['is_active'] !== 1) {
        $_SESSION = [];
        session_destroy();
        fail('Your account is not available. Please contact support.', 401);
    }

    return (int) $_SESSION['user_id'];
}

function require_admin()
{
    if (($_SESSION['user_type'] ?? '') !== 'admin' || (empty($_SESSION['admin_id']) && empty($_SESSION['user_id']))) {
        fail('Administrator access required.', 403);
    }
    return (int) ($_SESSION['admin_id'] ?? $_SESSION['user_id']);
}

function optional_user_id()
{
    if (($_SESSION['user_type'] ?? '') === 'customer' && !empty($_SESSION['user_id'])) {
        return (int) $_SESSION['user_id'];
    }
    return null;
}


/* ---------- Promotions ---------- */

function find_valid_promo($code)
{
    $code = strtoupper(trim($code));
    if ($code === '') {
        return null;
    }

    $stmt = db()->prepare(
        "SELECT * FROM promotions
          WHERE code = ? AND status = 'Active' AND expiry_date >= CURDATE()"
    );
    $stmt->execute([$code]);
    $promo = $stmt->fetch();

    return $promo ?: null;
}

function calc_discount($promo, $subtotal)
{
    if (!$promo || $subtotal <= 0) {
        return 0.0;
    }

    $value    = (float) $promo['discount_value'];
    $discount = $promo['discount_type'] === 'Percentage' ? $subtotal * ($value / 100) : $value;

    return round(min($discount, $subtotal), 2);
}