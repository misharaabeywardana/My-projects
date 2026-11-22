<?php
/* =========================================
   YUMMYPREP - ORDERS API
   ========================================= */

require_once __DIR__ . '/helpers.php';

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

/** Load items for a list of order ids, grouped by order id. */
function load_items($orderIds)
{
    if (!$orderIds) {
        return [];
    }

    $marks = implode(',', array_fill(0, count($orderIds), '?'));
    $stmt  = db()->prepare("SELECT * FROM order_items WHERE order_id IN ($marks) ORDER BY id");
    $stmt->execute(array_values($orderIds));

    $grouped = [];
    foreach ($stmt as $it) {
        $grouped[$it['order_id']][] = [
            'meal_id'   => $it['meal_id'] !== null ? (int) $it['meal_id'] : null,
            'item_name' => $it['item_name'],
            'name'      => $it['item_name'],
            'category'  => $it['category'],
            'price'     => (float) $it['price'],
            'quantity'  => (int) $it['quantity'],
            'image'     => $it['image'],
        ];
    }
    return $grouped;
}

/**
 * Shape an order for the front-end. Personal details (name, phone, address, email)
 * are only included when $withContact is true.
 */
function format_order($o, $items, $withContact, $withInternalId = false)
{
    $out = [
        'order_number'   => $o['order_number'],
        'status'         => $o['status'],
        'delivery_day'   => $o['delivery_day'],
        'payment_method' => $o['payment_method'],
        'subtotal'       => (float) $o['subtotal'],
        'delivery_fee'   => (float) $o['delivery_fee'],
        'discount'       => (float) $o['discount'],
        'total'          => (float) $o['total'],
        'created_at'     => iso_date($o['created_at']),
        'items'          => $items,
    ];

    if ($withInternalId) {
        $out['id'] = (int) $o['id'];
    }

    if ($withContact) {
        $out['first_name']    = $o['first_name'];
        $out['last_name']     = $o['last_name'];
        $out['customer_name'] = trim($o['first_name'] . ' ' . $o['last_name']);
        $out['email']         = $o['email'];
        $out['phone']         = $o['phone'];
        $out['address']       = $o['address'];
    }

    return $out;
}

/** Fetch several orders (already queried) and attach their items. */
function build_orders($rows, $withContact, $withInternalId)
{
    $items = load_items(array_column($rows, 'id'));
    $out   = [];
    foreach ($rows as $o) {
        $out[] = format_order($o, $items[$o['id']] ?? [], $withContact, $withInternalId);
    }
    return $out;
}


$action = get_action();

switch ($action) {

    /* ==========================================================
       PLACE ORDER
       ========================================================== */
    case 'place_order':
        require_method('POST');

        $body = json_decode((string) file_get_contents('php://input'), true);
        if (!is_array($body)) {
            fail('Invalid order data.', 400);
        }

        $first   = clean_text($body['firstName'] ?? '', 50);
        $last    = clean_text($body['lastName'] ?? '', 50);
        $email   = strtolower(clean_text($body['email'] ?? '', 150));
        $phone   = clean_text($body['phone'] ?? '', 20);
        $address = clean_text($body['address'] ?? '', 255);
        $payment = clean_text($body['payment'] ?? 'Cash on Delivery', 40) ?: 'Cash on Delivery';
        $day     = (strtolower((string) ($body['deliveryDay'] ?? '')) === 'tomorrow') ? 'tomorrow' : 'today';
        $promoIn = clean_text($body['promoCode'] ?? '', 30);
        $items   = $body['items'] ?? null;

        if ($first === '' || $last === '' || $address === '') fail('Please fill in all required customer details.', 422);
        if (!valid_email($email))                              fail('Please enter a valid email address.', 422);
        if (!valid_phone($phone))                              fail('Please enter a valid phone number.', 422);
        if (!is_array($items) || count($items) === 0)          fail('Your cart is empty.', 422);
        if (count($items) > 50)                                fail('Too many items in one order.', 422);

        $pdo = db();

        $mealStmt = $pdo->prepare(
            'SELECT id, name, category, price, image_url, is_available FROM meals WHERE id = ?'
        );
        $lines    = [];
        $subtotal = 0.0;

        foreach ($items as $it) {
            if (!is_array($it)) {
                fail('Invalid cart item.', 422);
            }

            $qty = filter_var($it['quantity'] ?? 1, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1, 'max_range' => 50]]);
            if ($qty === false) {
                fail('Invalid item quantity.', 422);
            }

            $mealId = $it['mealId'] ?? null;

            if ($mealId !== null && $mealId !== '' && ctype_digit((string) $mealId)) {
                $mealStmt->execute([(int) $mealId]);
                $meal = $mealStmt->fetch();

                if (!$meal || (int) $meal['is_available'] !== 1) {
                    fail('A meal in your cart is no longer available. Please refresh your cart.', 422);
                }

                $line = [
                    'meal_id'  => (int) $meal['id'],
                    'name'     => $meal['name'],
                    'category' => $meal['category'],
                    'price'    => round((float) $meal['price'], 2),
                    'image'    => $meal['image_url'],
                ];
            } else {
                if (($it['category'] ?? '') !== 'Customized') {
                    fail('Invalid cart item.', 422);
                }

                $price = $it['price'] ?? 0;
                if (!is_numeric($price) || $price <= 0 || $price > 50000) {
                    fail('Invalid price for a customised meal.', 422);
                }

                // සියලුම selections, beverages සහ juices සහිත සම්පූර්ණ නම ඩේටාබේස් එකට ලබා දීම
                $detailedItemName = clean_text($it['name'] ?? 'Customized Meal', 500);

                $line = [
                    'meal_id'  => null,
                    'name'     => $detailedItemName,
                    'category' => 'Customized',
                    'price'    => round((float) $price, 2),
                    'image'    => '../images/customizeFood.jpeg',
                ];
            }

            $line['quantity'] = $qty;
            $subtotal        += $line['price'] * $qty;
            $lines[]          = $line;
        }

        $subtotal = round($subtotal, 2);

        $promo    = null;
        $discount = 0.0;
        if ($promoIn !== '') {
            $promo = find_valid_promo($promoIn);
            if (!$promo) {
                fail('The promo code is no longer valid. Please remove it and try again.', 422);
            }
            $discount = calc_discount($promo, $subtotal);
        }

        $delivery = (float) DELIVERY_FEE;
        $total    = round(max(0, $subtotal + $delivery - $discount), 2);
        $userId   = optional_user_id();

        $pdo->beginTransaction();
        try {
            $orderId     = null;
            $orderNumber = null;

            $insert = $pdo->prepare(
                'INSERT INTO orders (order_number, user_id, first_name, last_name, email, phone, address,
                                     delivery_day, payment_method, subtotal, delivery_fee, discount,
                                     promo_code, total, status)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
            );

            for ($attempt = 0; $attempt < 10 && $orderId === null; $attempt++) {
                $candidate = 'YP' . random_int(100000, 999999);
                try {
                    $insert->execute([
                        $candidate, $userId, $first, $last, $email, $phone, $address,
                        $day, $payment, $subtotal, $delivery, $discount,
                        $promo ? $promo['code'] : null, $total, DEFAULT_ORDER_STATUS,
                    ]);
                    $orderId     = (int) $pdo->lastInsertId();
                    $orderNumber = $candidate;
                } catch (PDOException $e) {
                    if ($e->getCode() !== '23000') {
                        throw $e;
                    }
                }
            }
            if ($orderId === null) {
                throw new RuntimeException('Could not generate an order number.');
            }

            $itemInsert = $pdo->prepare(
                'INSERT INTO order_items (order_id, meal_id, item_name, category, price, quantity, image)
                 VALUES (?, ?, ?, ?, ?, ?, ?)'
            );
            foreach ($lines as $l) {
                $itemInsert->execute([$orderId, $l['meal_id'], $l['name'], $l['category'], $l['price'], $l['quantity'], $l['image']]);
            }

            $pdo->commit();
        } catch (Throwable $e) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            throw $e;
        }

        ok([
            'orderNumber' => $orderNumber,
            'total'       => $total,
            'subtotal'    => $subtotal,
            'delivery'    => $delivery,
            'discount'    => $discount,
            'deliveryDay' => $day,
        ], 'Order placed successfully!');
        break;


    /* ==========================================================
       TRACK ORDER
       ========================================================== */
    case 'track_order':
        $number = strtoupper(ltrim(trim((string) ($_GET['orderNumber'] ?? '')), '#'));
        if (!preg_match('/^[A-Z0-9]{4,20}$/', $number)) {
            fail('Order not found.', 404);
        }

        $stmt = db()->prepare('SELECT * FROM orders WHERE order_number = ?');
        $stmt->execute([$number]);
        $order = $stmt->fetch();

        if (!$order) {
            fail('Order not found.', 404);
        }

        $isAdmin = ($_SESSION['user_type'] ?? '') === 'admin';
        $isOwner = $order['user_id'] !== null && (int) $order['user_id'] === optional_user_id();

        $built = build_orders([$order], $isAdmin || $isOwner, $isAdmin);
        ok($built[0]);
        break;


    /* ==========================================================
       CUSTOMER ORDER HISTORY
       ========================================================== */
    case 'get_user_orders':
        $userId = require_user();

        $stmt = db()->prepare('SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC, id DESC');
        $stmt->execute([$userId]);

        ok(build_orders($stmt->fetchAll(), true, false));
        break;


    /* ==========================================================
       ADMIN: ALL ORDERS + STATUS UPDATE
       ========================================================== */
    case 'admin_get_orders':
        if (empty($_SESSION['user_id']) || ($_SESSION['user_type'] ?? '') !== 'admin') {
            fail('Admin authorization required.', 403);
        }

        try {
            $rows = db()->query('SELECT * FROM orders ORDER BY created_at DESC, id DESC')->fetchAll();
            ok(build_orders($rows, true, true));
        } catch (Exception $e) {
            fail('Failed to fetch orders: ' . $e->getMessage(), 500);
        }
        break;


    case 'admin_update_status':
        require_method('POST');
        if (empty($_SESSION['user_id']) || ($_SESSION['user_type'] ?? '') !== 'admin') {
            fail('Admin authorization required.', 403);
        }

        $orderId = filter_var($_POST['orderId'] ?? null, FILTER_VALIDATE_INT);
        $status  = post_text('status', 30);

        if (!$orderId)                                 fail('Invalid order.', 422);
        if (!in_array($status, ORDER_STATUSES, true))  fail('Invalid status.', 422);

        $stmt = db()->prepare('UPDATE orders SET status = ? WHERE id = ?');
        $stmt->execute([$status, $orderId]);

        $exists = db()->prepare('SELECT 1 FROM orders WHERE id = ?');
        $exists->execute([$orderId]);
        if (!$exists->fetch()) fail('Order not found.', 404);

        ok(null, 'Order status updated.');
        break;


    default:
        fail('Unknown action.', 400);
}