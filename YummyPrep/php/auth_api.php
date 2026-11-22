<?php
/* =========================================
   YUMMYPREP - AUTHENTICATION API
   File: php/auth_api.php
   ========================================= */

require_once __DIR__ . '/helpers.php';

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

$action = get_action();

switch ($action) {

    /* ---------- Unified Login (Checks both `admins` and `users` tables) ---------- */
    case 'login':
        require_method('POST');

        $email    = strtolower(post_text('email', 150));
        $password = (string) ($_POST['password'] ?? '');

        if ($email === '' || $password === '') {
            fail('Please enter both email and password.', 422);
        }

        $pdo = db();

        // 1. Check in `admins` table first (e.g. admin@yummyprep.lk)
        $adminStmt = $pdo->prepare('SELECT * FROM admins WHERE email = ?');
        $adminStmt->execute([$email]);
        $admin = $adminStmt->fetch();

        if ($admin && password_verify($password, $admin['password_hash'])) {
            session_regenerate_id(true);
            $_SESSION['user_type'] = 'admin';
            $_SESSION['user_id']   = (int) $admin['id'];

            ok([
                'id'    => (int) $admin['id'],
                'name'  => $admin['name'],
                'email' => $admin['email'],
                'type'  => 'admin',
            ], 'Admin login successful.');
        }

        // 2. Check in `users` table if not found in `admins`
        $userStmt = $pdo->prepare('SELECT * FROM users WHERE email = ?');
        $userStmt->execute([$email]);
        $user = $userStmt->fetch();

        $hash = $user ? $user['password_hash'] : DUMMY_HASH;
        if ($user && password_verify($password, $hash)) {
            $userType = !empty($user['user_type']) ? strtolower($user['user_type']) : 'customer';

            session_regenerate_id(true);
            $_SESSION['user_type'] = $userType;
            $_SESSION['user_id']   = (int) $user['id'];

            ok([
                'id'            => (int) $user['id'],
                'name'          => $user['full_name'],
                'email'         => $user['email'],
                'phone'         => $user['phone'],
                'address'       => $user['delivery_address'] ?? '',
                'type'          => $userType,
                'profile_image' => $user['profile_image'] ?? '',
            ], 'Login successful.');
        }

        fail('Invalid email or password.', 401);
        break;


    /* ---------- Dedicated Admin Login (from adminLogin.html) ---------- */
    case 'admin_login':
        require_method('POST');

        $email    = strtolower(post_text('adminEmail', 150));
        $password = (string) ($_POST['adminPassword'] ?? '');

        if ($email === '' || $password === '') {
            fail('Please enter both admin email and password.', 422);
        }

        $stmt = db()->prepare('SELECT * FROM admins WHERE email = ?');
        $stmt->execute([$email]);
        $admin = $stmt->fetch();

        if (!$admin || !password_verify($password, $admin['password_hash'])) {
            fail('Invalid administrator credentials.', 401);
        }

        session_regenerate_id(true);
        $_SESSION['user_type'] = 'admin';
        $_SESSION['user_id']   = (int) $admin['id'];

        ok([
            'id'    => (int) $admin['id'],
            'name'  => $admin['name'],
            'email' => $admin['email'],
            'type'  => 'admin',
        ], 'Admin login successful.');
        break;


    /* ---------- Customer Registration ---------- */
    case 'register':
        require_method('POST');

        $name     = post_text('fullName', 100);
        $email    = strtolower(post_text('email', 150));
        $phone    = post_text('phone', 20);
        $password = (string) ($_POST['password'] ?? '');
        $confirm  = (string) ($_POST['confirmPassword'] ?? '');

        if (mb_strlen($name) < 2)          fail('Please enter your full name.', 422);
        if (!valid_email($email))          fail('Please enter a valid email address.', 422);
        if (!valid_phone($phone))          fail('Please enter a valid phone number.', 422);
        if (strlen($password) < MIN_PASSWORD_LENGTH) {
            fail('Password must be at least ' . MIN_PASSWORD_LENGTH . ' characters.', 422);
        }
        if ($password !== $confirm)        fail('Passwords do not match.', 422);

        $pdo  = db();
        $stmt = $pdo->prepare('SELECT id FROM users WHERE email = ?');
        $stmt->execute([$email]);
        if ($stmt->fetch()) {
            fail('An account with this email already exists.', 409);
        }

        $stmt = $pdo->prepare(
            'INSERT INTO users (full_name, email, phone, password_hash) VALUES (?, ?, ?, ?)'
        );
        $stmt->execute([$name, $email, $phone, password_hash($password, PASSWORD_DEFAULT)]);

        $newUserId = (int) $pdo->lastInsertId();

        session_regenerate_id(true);
        $_SESSION['user_type'] = 'customer';
        $_SESSION['user_id']   = $newUserId;

        ok([
            'id'            => $newUserId,
            'name'          => $name,
            'email'         => $email,
            'phone'         => $phone,
            'address'       => '',
            'type'          => 'customer',
            'profile_image' => '',
        ], 'Account created successfully!');
        break;


    /* ---------- Profile Image Upload ---------- */
    case 'upload_avatar':
        require_method('POST');

        if (empty($_SESSION['user_id'])) {
            fail('Not logged in.', 401);
        }

        $imageData = $_POST['image'] ?? '';
        if (!$imageData) {
            fail('No image data provided.', 422);
        }

        $stmt = db()->prepare('UPDATE users SET profile_image = ? WHERE id = ?');
        $stmt->execute([$imageData, $_SESSION['user_id']]);

        ok(['profile_image' => $imageData], 'Profile picture updated successfully!');
        break;


    /* ---------- Session Check (Admin & Customer) ---------- */
    case 'check_session':
        $userId   = $_SESSION['user_id'] ?? null;
        $userType = $_SESSION['user_type'] ?? '';

        if (!empty($userId)) {
            $pdo = db();

            if ($userType === 'admin') {
                $stmt = $pdo->prepare('SELECT id, name, email FROM admins WHERE id = ?');
                $stmt->execute([$userId]);
                $admin = $stmt->fetch();

                if ($admin) {
                    $admin['type'] = 'admin';
                    ok($admin);
                }
            } else {
                $stmt = $pdo->prepare(
                    'SELECT id, full_name AS name, email, phone, delivery_address AS address, profile_image
                       FROM users WHERE id = ? AND is_active = 1'
                );
                $stmt->execute([$userId]);
                $user = $stmt->fetch();

                if ($user) {
                    try {
                        $orderStmt = $pdo->prepare('
                            SELECT
                                COUNT(*) AS total_orders,
                                SUM(CASE WHEN LOWER(status) IN ("completed", "delivered") THEN 1 ELSE 0 END) AS completed_orders,
                                SUM(CASE WHEN LOWER(status) NOT IN ("completed", "delivered", "cancelled") THEN 1 ELSE 0 END) AS pending_orders
                            FROM orders WHERE user_id = ?
                        ');
                        $orderStmt->execute([$userId]);
                        $counts = $orderStmt->fetch();

                        $user['total_orders']     = (int) ($counts['total_orders'] ?? 0);
                        $user['completed_orders'] = (int) ($counts['completed_orders'] ?? 0);
                        $user['pending_orders']   = (int) ($counts['pending_orders'] ?? 0);
                    } catch (Exception $e) {
                        $user['total_orders']     = 0;
                        $user['completed_orders'] = 0;
                        $user['pending_orders']   = 0;
                    }

                    $user['type'] = 'customer';
                    ok($user);
                }
            }
        }

        fail('Not logged in.', 401);
        break;


    /* ---------- Logout ---------- */
    case 'logout':
        $_SESSION = [];
        if (ini_get('session.use_cookies')) {
            $p = session_get_cookie_params();
            setcookie(session_name(), '', [
                'expires'  => time() - 42000,
                'path'     => $p['path'],
                'secure'   => $p['secure'],
                'httponly' => $p['httponly'],
                'samesite' => 'Lax',
            ]);
        }
        session_destroy();
        ok(null, 'Logged out.');
        break;

    default:
        fail('Unknown action.', 400);
}