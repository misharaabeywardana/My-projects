<?php

header('Content-Type: application/json');
$file = 'account.json';


/* ---------- Helpers ---------- */

function respond($code, $arr) {
    http_response_code($code);
    echo json_encode($arr);
    exit;
}

function loadAccounts($file) {
    if (!file_exists($file)) return [];
    $d = json_decode(file_get_contents($file), true);
    return is_array($d) ? $d : [];
}

function saveAccounts($file, $data) {
    file_put_contents($file, json_encode($data, JSON_PRETTY_PRINT), LOCK_EX);
}

// Never send the password (or its hash) to the browser
function publicAccount($acc) {
    unset($acc['password']);
    return $acc;
}

// Works with hashed passwords AND old plain-text ones already in your JSON
function passwordMatches($stored, $plain) {
    $info = password_get_info($stored);
    if (!empty($info['algo'])) {
        return password_verify($plain, $stored);
    }
    return hash_equals((string)$stored, (string)$plain);
}

function findIndex($data, $email) {
    foreach ($data as $i => $item) {
        if (strtolower($item['email']) === strtolower($email)) return $i;
    }
    return -1;
}


/* ---------- GET: list accounts (without passwords) ---------- */

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    echo json_encode(array_map('publicAccount', loadAccounts($file)));
    exit;
}


/* ---------- POST ---------- */

if ($_SERVER['REQUEST_METHOD'] === 'POST') {

    $req = json_decode(file_get_contents('php://input'), true);
    if (!$req || empty($req['action'])) {
        respond(400, ["success" => false, "message" => "Invalid request."]);
    }

    $data   = loadAccounts($file);
    $action = $req['action'];


    // LOGIN

    if ($action === 'login') {
        $email = trim($req['email'] ?? '');
        $pass  = $req['password'] ?? '';

        $i = findIndex($data, $email);
        if ($i === -1) {
            respond(404, ["success" => false, "message" => "Account not found."]);
        }
        if (!passwordMatches($data[$i]['password'], $pass)) {
            respond(401, ["success" => false, "message" => "Incorrect password."]);
        }

        // Upgrade old plain-text passwords to hashes on successful login
        if (empty(password_get_info($data[$i]['password'])['algo'])) {
            $data[$i]['password'] = password_hash($pass, PASSWORD_DEFAULT);
            saveAccounts($file, $data);
        }

        respond(200, ["success" => true, "account" => publicAccount($data[$i])]);
    }


    // REGISTER

    if ($action === 'register') {
        $name  = trim($req['full_name'] ?? '');
        $email = trim($req['email'] ?? '');
        $pass  = $req['password'] ?? '';

        if ($name === '' || $email === '' || $pass === '') {
            respond(400, ["success" => false, "message" => "Please fill all fields."]);
        }
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            respond(400, ["success" => false, "message" => "Invalid email address."]);
        }
        if (strlen($pass) < 6) {
            respond(400, ["success" => false, "message" => "Password must be at least 6 characters."]);
        }
        if (findIndex($data, $email) !== -1) {
            respond(409, ["success" => false, "message" => "An account with this email already exists."]);
        }

        // Sequential ID: 001, 002, 003 ...
        $maxId = 0;
        foreach ($data as $item) {
            if (isset($item['account_id']) && intval($item['account_id']) > $maxId) {
                $maxId = intval($item['account_id']);
            }
        }

        $account = [
            "account_id"        => str_pad($maxId + 1, 3, '0', STR_PAD_LEFT),
            "full_name"         => $name,
            "email"             => $email,
            "password"          => password_hash($pass, PASSWORD_DEFAULT),
            "phone"             => "",
            "address"           => "",
            "account_type"      => "Customer",
            "membership_status" => "Active"
        ];

        $data[] = $account;
        saveAccounts($file, $data);
        respond(200, ["success" => true, "account" => publicAccount($account)]);
    }


    // UPDATE PROFILE (never touches the password)

    if ($action === 'update_profile') {
        $i = findIndex($data, $req['email'] ?? '');
        if ($i === -1) {
            respond(404, ["success" => false, "message" => "Account not found."]);
        }

        if (isset($req['full_name'])) $data[$i]['full_name'] = trim($req['full_name']);
        if (isset($req['phone']))     $data[$i]['phone']     = trim($req['phone']);
        if (isset($req['address']))   $data[$i]['address']   = trim($req['address']);

        saveAccounts($file, $data);
        respond(200, ["success" => true, "account" => publicAccount($data[$i])]);
    }


    // CHANGE PASSWORD (current password is verified here, on the server)

    if ($action === 'change_password') {
        $email   = $req['email'] ?? '';
        $current = $req['current_password'] ?? '';
        $new     = $req['new_password'] ?? '';

        $i = findIndex($data, $email);
        if ($i === -1) {
            respond(404, ["success" => false, "message" => "Account not found."]);
        }
        if (!passwordMatches($data[$i]['password'], $current)) {
            respond(401, ["success" => false, "message" => "Current password is incorrect."]);
        }
        if (strlen($new) < 6) {
            respond(400, ["success" => false, "message" => "New password must be at least 6 characters."]);
        }
        if ($new === $current) {
            respond(400, ["success" => false, "message" => "New password must be different from the current one."]);
        }

        $data[$i]['password'] = password_hash($new, PASSWORD_DEFAULT);
        saveAccounts($file, $data);
        respond(200, ["success" => true, "message" => "Password updated!"]);
    }

    respond(400, ["success" => false, "message" => "Unknown action."]);
}

?>