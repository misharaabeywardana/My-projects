<?php
/* =========================================
   YUMMYPREP - FAQ API
   Public:
     GET  action=get_faqs                (Active FAQs only, for public pages)
   Admin (web master):
     GET  action=admin_get_faqs
     POST action=admin_save_faq    [id], question, answer, category, status
     POST action=admin_delete_faq  id
   ========================================= */

require_once __DIR__ . '/helpers.php';

function format_faq($f)
{
    return [
        'id'       => (int) $f['id'],
        'question' => $f['question'],
        'answer'   => $f['answer'],
        'category' => $f['category'],
        'status'   => $f['status'],
    ];
}

$action = get_action();

switch ($action) {

    case 'get_faqs':
        $rows = db()->query("SELECT * FROM faqs WHERE status = 'Active' ORDER BY category, id")->fetchAll();
        ok(array_map('format_faq', $rows));
        break;


    case 'admin_get_faqs':
        require_admin();
        $rows = db()->query('SELECT * FROM faqs ORDER BY id DESC')->fetchAll();
        ok(array_map('format_faq', $rows));
        break;


    case 'admin_save_faq':
        require_method('POST');
        require_admin();

        $pdo      = db();
        $id       = filter_var($_POST['id'] ?? null, FILTER_VALIDATE_INT);
        $question = post_text('question', 255);
        $answer   = post_text('answer', 2000, true);
        $category = post_text('category', 30);
        $status   = post_text('status', 10);

        if ($question === '' || $answer === '')                 fail('Please fill in both the question and answer.', 422);
        if (!in_array($category, FAQ_CATEGORIES, true))         fail('Invalid category.', 422);
        if (!in_array($status, ['Active', 'Inactive'], true))   fail('Invalid status.', 422);

        if ($id) {
            $stmt = $pdo->prepare('UPDATE faqs SET question=?, answer=?, category=?, status=? WHERE id=?');
            $stmt->execute([$question, $answer, $category, $status, $id]);
            ok(['id' => $id], 'FAQ updated successfully.');
        }

        $stmt = $pdo->prepare('INSERT INTO faqs (question, answer, category, status) VALUES (?, ?, ?, ?)');
        $stmt->execute([$question, $answer, $category, $status]);
        ok(['id' => (int) $pdo->lastInsertId()], 'FAQ added successfully.');
        break;


    case 'admin_delete_faq':
        require_method('POST');
        require_admin();

        $id = filter_var($_POST['id'] ?? null, FILTER_VALIDATE_INT);
        if (!$id) fail('Invalid FAQ.', 422);

        $stmt = db()->prepare('DELETE FROM faqs WHERE id = ?');
        $stmt->execute([$id]);
        if ($stmt->rowCount() === 0) fail('FAQ not found.', 404);

        ok(null, 'FAQ deleted successfully.');
        break;


    default:
        fail('Unknown action.', 400);
}
