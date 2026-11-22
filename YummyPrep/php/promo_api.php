<?php
/* =========================================
   YUMMYPREP - PROMOTIONS API
   Public:
     GET  action=validate_promo&code=&subtotal=
     GET  action=get_active_promos
   Admin (web master):
     GET  action=admin_get_promos
     POST action=admin_save_promo    [id], title, code, discountType, discountValue,
                                     expiryDate, status, description
     POST action=admin_delete_promo  id
   ========================================= */

require_once __DIR__ . '/helpers.php';

function format_promo($p)
{
    return [
        'id'             => (int) $p['id'],
        'title'          => $p['title'],
        'code'           => $p['code'],
        'description'    => $p['description'],
        'discount_type'  => $p['discount_type'],
        'discount_value' => (float) $p['discount_value'],
        'expiry_date'    => $p['expiry_date'],
        'status'         => $p['status'],
    ];
}

$action = get_action();

switch ($action) {

    /* =========================================
       PUBLIC: GET ACTIVE PROMOTIONS FOR CUSTOMERS
       ========================================= */
    case 'get_active_promos':
        try {
            $rows = db()->query("SELECT title, code, description, discount_type, discount_value FROM promotions WHERE status = 'Active' AND expiry_date >= CURDATE() ORDER BY id DESC")->fetchAll();
            ok($rows);
        } catch (Exception $e) {
            fail('Failed to fetch active promotions.', 500);
        }
        break;


    /* cart.js stores this object and reads discountType / discountValue */
    case 'validate_promo':
        $code  = clean_text($_GET['code'] ?? '', 30);
        $promo = find_valid_promo($code);

        if (!$promo) {
            fail('Invalid or expired promo code.', 404);
        }

        ok([
            'id'            => (int) $promo['id'],
            'code'          => $promo['code'],
            'title'         => $promo['title'],
            'discountType'  => $promo['discount_type'],
            'discountValue' => (float) $promo['discount_value'],
            'type'          => $promo['discount_type'],
            'value'         => (float) $promo['discount_value'],
        ], "Promo code '" . $promo['code'] . "' applied!");
        break;


    case 'admin_get_promos':
        require_admin();
        $rows = db()->query('SELECT * FROM promotions ORDER BY id DESC')->fetchAll();
        ok(array_map('format_promo', $rows));
        break;


    case 'admin_save_promo':
        require_method('POST');
        require_admin();

        $pdo         = db();
        $id          = filter_var($_POST['id'] ?? null, FILTER_VALIDATE_INT);
        $title       = post_text('title', 100);
        $code        = strtoupper(post_text('code', 30));
        $type        = post_text('discountType', 20);
        $value       = $_POST['discountValue'] ?? '';
        $expiry      = post_text('expiryDate', 10);
        $status      = post_text('status', 10);
        $description = post_text('description', 500, true);

        if ($title === '')                                   fail('Title is required.', 422);
        if (!preg_match('/^[A-Z0-9_\-]{3,30}$/', $code))     fail('Promo code must be 3-30 letters/numbers.', 422);
        if (!in_array($type, ['Percentage', 'Fixed'], true)) fail('Invalid discount type.', 422);
        if (!is_numeric($value) || $value <= 0)              fail('Please enter a valid discount value.', 422);
        if ($type === 'Percentage' && $value > 100)          fail('Percentage discount cannot exceed 100.', 422);
        if (!valid_date($expiry))                            fail('Please enter a valid expiry date.', 422);
        if (!in_array($status, ['Active', 'Inactive'], true)) fail('Invalid status.', 422);
        $value = round((float) $value, 2);

        // Code must be unique
        $stmt = $pdo->prepare('SELECT id FROM promotions WHERE code = ? AND id <> ?');
        $stmt->execute([$code, $id ?: 0]);
        if ($stmt->fetch()) fail('This promo code already exists.', 409);

        if ($id) {
            $stmt = $pdo->prepare(
                'UPDATE promotions SET title=?, code=?, description=?, discount_type=?,
                        discount_value=?, expiry_date=?, status=? WHERE id=?'
            );
            $stmt->execute([$title, $code, $description, $type, $value, $expiry, $status, $id]);
            ok(['id' => $id], 'Promotion updated successfully.');
        }

        $stmt = $pdo->prepare(
            'INSERT INTO promotions (title, code, description, discount_type, discount_value, expiry_date, status)
             VALUES (?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([$title, $code, $description, $type, $value, $expiry, $status]);
        ok(['id' => (int) $pdo->lastInsertId()], 'Promotion added successfully.');
        break;


    case 'admin_delete_promo':
        require_method('POST');
        require_admin();

        $id = filter_var($_POST['id'] ?? null, FILTER_VALIDATE_INT);
        if (!$id) fail('Invalid promotion.', 422);

        $stmt = db()->prepare('DELETE FROM promotions WHERE id = ?');
        $stmt->execute([$id]);
        if ($stmt->rowCount() === 0) fail('Promotion not found.', 404);

        ok(null, 'Promotion deleted successfully.');
        break;


    default:
        fail('Unknown action.', 400);
}