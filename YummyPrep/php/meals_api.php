<?php
/* =========================================
   YUMMYPREP - MEALS API
   Public:
     GET  action=get_all
     GET  action=get_details&id=
   Admin (web master):
     POST action=save_meal    [id], mealCode, name, price, category, description,
                              imageUrl, isAvailable, [calories, protein, carbs, fat, ingredients]
     POST action=delete_meal  id
   ========================================= */

require_once __DIR__ . '/helpers.php';

/** Shape a DB row the way meals.js / mealDetails.js / mealManagement.js read it. */
function format_meal($m)
{
    $ingredients = array_values(array_filter(array_map('trim', explode(',', (string) $m['ingredients']))));

    return [
        'id'           => (int) $m['id'],
        'meal_code'    => $m['meal_code'],
        'name'         => $m['name'],
        'category'     => $m['category'],
        'description'  => $m['description'],
        'price'        => (float) $m['price'],
        'image_url'    => $m['image_url'],
        'is_available' => (int) $m['is_available'],
        'status'       => ((int) $m['is_available'] === 1) ? 'Available' : 'Unavailable',
        'calories'     => (int) $m['calories'],
        'protein'      => (int) $m['protein_g'] . 'g',
        'carbs'        => (int) $m['carbs_g'] . 'g',
        'fat'          => (int) $m['fat_g'] . 'g',
        'ingredients'  => $ingredients,
    ];
}

/** Accept ../images/x.jpeg, a bare file name, or an http(s) URL. No quotes/spaces/brackets. */
function clean_image($value)
{
    $value = trim((string) $value);
    $ok = preg_match('~^(\.\./images/[A-Za-z0-9_.\-]+|[A-Za-z0-9_.\-]+|https?://[A-Za-z0-9_.\-/%?=&:+]+)$~', $value);
    return $ok ? $value : '../images/chickenBowl1.jpeg';
}

function unique_meal_code($pdo, $wanted)
{
    $wanted = strtoupper(preg_replace('/[^A-Za-z0-9\-]/', '', $wanted));
    $check  = $pdo->prepare('SELECT 1 FROM meals WHERE meal_code = ?');

    $code = $wanted !== '' ? substr($wanted, 0, 20) : 'MEAL' . random_int(100, 999);
    for ($i = 0; $i < 50; $i++) {
        $check->execute([$code]);
        if (!$check->fetch()) {
            return $code;
        }
        $code = 'MEAL' . random_int(1000, 99999);
    }
    throw new RuntimeException('Could not generate a unique meal code.');
}


$action = get_action();

switch ($action) {

    case 'get_all':
        $rows = db()->query('SELECT * FROM meals ORDER BY category, name')->fetchAll();
        ok(array_map('format_meal', $rows));
        break;


    case 'get_details':
        $id = filter_var($_GET['id'] ?? null, FILTER_VALIDATE_INT);
        if (!$id) {
            fail('Meal not found.', 404);   // mealDetails.js then falls back to searching get_all
        }

        $stmt = db()->prepare('SELECT * FROM meals WHERE id = ?');
        $stmt->execute([$id]);
        $meal = $stmt->fetch();

        if (!$meal) fail('Meal not found.', 404);
        ok(format_meal($meal));
        break;


    case 'save_meal':
        require_method('POST');
        require_admin();

        $pdo         = db();
        $id          = filter_var($_POST['id'] ?? null, FILTER_VALIDATE_INT);
        $name        = post_text('name', 100);
        $category    = post_text('category', 30);
        $description = post_text('description', 1000, true);
        $image       = clean_image($_POST['imageUrl'] ?? '');
        $available   = (($_POST['isAvailable'] ?? '1') === '1') ? 1 : 0;
        $price       = $_POST['price'] ?? '';

        if ($name === '')                                   fail('Meal name is required.', 422);
        if (!in_array($category, MEAL_CATEGORIES, true))    fail('Please choose a valid category.', 422);
        if (!is_numeric($price) || $price <= 0 || $price > 100000) {
            fail('Please enter a valid price.', 422);
        }
        $price = round((float) $price, 2);

        // Nutrition / ingredients are optional (the admin form does not send them yet)
        $extra = [];
        foreach (['calories' => 'calories', 'protein' => 'protein_g', 'carbs' => 'carbs_g', 'fat' => 'fat_g'] as $field => $col) {
            if (isset($_POST[$field]) && $_POST[$field] !== '') {
                $extra[$col] = max(0, (int) preg_replace('/\D/', '', (string) $_POST[$field]));
            }
        }
        if (isset($_POST['ingredients'])) {
            $extra['ingredients'] = post_text('ingredients', 1000);
        }

        if ($id) {
            $stmt = $pdo->prepare('SELECT id FROM meals WHERE id = ?');
            $stmt->execute([$id]);
            if (!$stmt->fetch()) fail('Meal not found.', 404);

            $set    = 'name = ?, category = ?, description = ?, price = ?, image_url = ?, is_available = ?';
            $params = [$name, $category, $description, $price, $image, $available];
            foreach ($extra as $col => $val) {
                $set .= ", $col = ?";
                $params[] = $val;
            }
            $params[] = $id;

            $pdo->prepare("UPDATE meals SET $set WHERE id = ?")->execute($params);
            ok(['id' => $id], 'Meal updated successfully.');
        }

        // New meal: the admin form invents a random meal code, we keep it only if it is free
        $code = unique_meal_code($pdo, post_text('mealCode', 20));

        $cols   = ['meal_code', 'name', 'category', 'description', 'price', 'image_url', 'is_available'];
        $params = [$code, $name, $category, $description, $price, $image, $available];
        foreach ($extra as $col => $val) {
            $cols[]   = $col;
            $params[] = $val;
        }
        $marks = implode(',', array_fill(0, count($cols), '?'));

        $pdo->prepare('INSERT INTO meals (' . implode(',', $cols) . ") VALUES ($marks)")->execute($params);
        ok(['id' => (int) $pdo->lastInsertId()], 'Meal added successfully.');
        break;


    case 'delete_meal':
        require_method('POST');
        require_admin();

        $id = filter_var($_POST['id'] ?? null, FILTER_VALIDATE_INT);
        if (!$id) fail('Invalid meal.', 422);

        // Past orders keep their item name/price (order_items.meal_id becomes NULL)
        $stmt = db()->prepare('DELETE FROM meals WHERE id = ?');
        $stmt->execute([$id]);

        if ($stmt->rowCount() === 0) fail('Meal not found.', 404);
        ok(null, 'Meal deleted successfully.');
        break;


    default:
        fail('Unknown action.', 400);
}
