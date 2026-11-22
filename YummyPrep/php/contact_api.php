<?php
/* =========================================
   YUMMYPREP - CONTACT US API
   Public:
     POST action=submit    name, email, phone, subject, message
   Admin (web master):
     GET  action=admin_get_messages
   ========================================= */

require_once __DIR__ . '/helpers.php';

$action = get_action();

switch ($action) {

    case 'submit':
        require_method('POST');

        $name    = post_text('name', 100);
        $email   = strtolower(post_text('email', 150));
        $phone   = post_text('phone', 20);
        $subject = post_text('subject', 100);
        $message = post_text('message', 3000, true);

        if (mb_strlen($name) < 2)   fail('Please enter your name.', 422);
        if (!valid_email($email))   fail('Please enter a valid email address.', 422);
        if ($phone !== '' && !valid_phone($phone)) fail('Please enter a valid phone number.', 422);
        if (mb_strlen($message) < 5) fail('Please enter your message.', 422);

        $stmt = db()->prepare(
            'INSERT INTO contact_messages (name, email, phone, subject, message) VALUES (?, ?, ?, ?, ?)'
        );
        $stmt->execute([$name, $email, $phone ?: null, $subject ?: 'Other', $message]);

        ok(null, 'Thank you! Your message has been sent. We will get back to you soon.');
        break;


    case 'admin_get_messages':
        require_admin();
        $rows = db()->query('SELECT * FROM contact_messages ORDER BY id DESC')->fetchAll();
        foreach ($rows as &$r) {
            $r['created_at'] = iso_date($r['created_at']);
        }
        unset($r);
        ok($rows);
        break;


    default:
        fail('Unknown action.', 400);
}
