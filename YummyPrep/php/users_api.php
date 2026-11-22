<?php
/* =========================================
   YUMMYPREP - USERS API
   Customer:
     GET  action=get_profile
     POST action=update_profile            name, phone, address
   Admin (web master):
     GET  action=admin_get_users
     POST action=admin_update_user_status  userId, isActive (1/0)
     POST action=admin_delete_user         userId
   ========================================= */

require_once __DIR__ . '/helpers.php';

$action = get_action();

switch ($action) {

    case 'get_profile':
        $id   = require_user();
        $stmt = db()->prepare(
            'SELECT id, full_name, email, phone, delivery_address FROM users WHERE id = ?'
        );
        $stmt->execute([$id]);
        $user = $stmt->fetch();

        if (!$user) {
            fail('User not found.', 404);
        }

        // Both naming styles are returned because profile.js / checkout.js read either
        $user['id']      = (int) $user['id'];
        $user['name']    = $user['full_name'];
        $user['address'] = $user['delivery_address'];
        ok($user);
        break;


    case 'update_profile':
        require_method('POST');
        $id = require_user();

        $name    = post_text('name', 100);
        $phone   = post_text('phone', 20);
        $address = post_text('address', 255);

        if (mb_strlen($name) < 2)  fail('Please enter your name.', 422);
        if (!valid_phone($phone))  fail('Please enter a valid phone number.', 422);

        $stmt = db()->prepare(
            'UPDATE users SET full_name = ?, phone = ?, delivery_address = ? WHERE id = ?'
        );
        $stmt->execute([$name, $phone, $address === '' ? null : $address, $id]);

        ok(null, 'Profile updated successfully!');
        break;


    case 'admin_get_users':
        require_admin();

        $rows = db()->query(
            "SELECT u.id, u.full_name, u.email, u.phone, u.is_active, u.created_at,
                    (SELECT COUNT(*) FROM orders o WHERE o.user_id = u.id) AS orders
               FROM users u
              ORDER BY u.created_at DESC, u.id DESC"
        )->fetchAll();

        foreach ($rows as &$r) {
            $r['id']         = (int) $r['id'];
            $r['name']       = $r['full_name'];
            $r['is_active']  = (int) $r['is_active'];
            $r['status']     = $r['is_active'] === 1 ? 'Active' : 'Disabled';
            $r['orders']     = (int) $r['orders'];
            $r['created_at'] = iso_date($r['created_at']);
            $r['joined']     = $r['created_at'];
        }
        unset($r);

        ok($rows);
        break;


    case 'admin_update_user_status':
        require_method('POST');
        require_admin();

        $userId   = filter_var($_POST['userId'] ?? null, FILTER_VALIDATE_INT);
        $isActive = (($_POST['isActive'] ?? '0') === '1') ? 1 : 0;

        if (!$userId) fail('Invalid user.', 422);

        $stmt = db()->prepare('UPDATE users SET is_active = ? WHERE id = ?');
        $stmt->execute([$isActive, $userId]);

        ok(null, $isActive ? 'User enabled.' : 'User disabled.');
        break;


    case 'admin_delete_user':
        require_method('POST');
        require_admin();

        $userId = filter_var($_POST['userId'] ?? null, FILTER_VALIDATE_INT);
        if (!$userId) fail('Invalid user.', 422);

        // Orders are kept for the shop's records (orders.user_id becomes NULL)
        $stmt = db()->prepare('DELETE FROM users WHERE id = ?');
        $stmt->execute([$userId]);

        if ($stmt->rowCount() === 0) fail('User not found.', 404);

        ok(null, 'User deleted successfully.');
        break;


    default:
        fail('Unknown action.', 400);
}
