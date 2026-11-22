<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST");
header("Access-Control-Allow-Headers: Content-Type");

$jsonFile = __DIR__ . "/about.json";

function sendResponse($success, $message, $data = null, $statusCode = 200) {
    http_response_code($statusCode);
    echo json_encode([
        "success" => $success,
        "message" => $message,
        "data" => $data
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit;
}

function readRecords($jsonFile) {
    if (!file_exists($jsonFile)) {
        file_put_contents($jsonFile, "[]");
    }

    $json = file_get_contents($jsonFile);
    $records = json_decode($json, true);

    if (!is_array($records)) {
        $records = [];
    }

    return $records;
}

function saveRecords($jsonFile, $records) {
    $result = file_put_contents(
        $jsonFile,
        json_encode($records, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE),
        LOCK_EX
    );

    return $result !== false;
}

$method = $_SERVER["REQUEST_METHOD"];

if ($method === "GET") {

    $records = readRecords($jsonFile);

    // GET ?id=1 returns one record.
    if (isset($_GET["id"]) && $_GET["id"] !== "") {
        $id = (int) $_GET["id"];

        foreach ($records as $record) {
            if ((int)($record["id"] ?? 0) === $id) {
                sendResponse(true, "One record found.", $record);
            }
        }

        sendResponse(false, "Record not found.", null, 404);
    }

    // GET without id returns all records.
    sendResponse(true, "All records found.", $records);
}

if ($method === "POST") {

    // Accept normal AJAX form data or a JSON request body.
    $input = $_POST;

    if (empty($input)) {
        $rawInput = file_get_contents("php://input");
        $decodedInput = json_decode($rawInput, true);

        if (is_array($decodedInput)) {
            $input = $decodedInput;
        }
    }

    $name = trim($input["name"] ?? "");
    $email = trim($input["email"] ?? "");
    $subject = trim($input["subject"] ?? "");
    $message = trim($input["message"] ?? "");
    $phone = trim($input["phone"] ?? "");
    $city = trim($input["city"] ?? "");

    if ($name === "" || $email === "" || $subject === "" || $message === "") {
        sendResponse(
            false,
            "Name, email, subject and message are required.",
            null,
            400
        );
    }

    $records = readRecords($jsonFile);

    $maxId = 0;
    foreach ($records as $record) {
        $currentId = (int)($record["id"] ?? 0);
        if ($currentId > $maxId) {
            $maxId = $currentId;
        }
    }

    $newRecord = [
        "id" => $maxId + 1,
        "name" => $name,
        "email" => $email,
        "subject" => $subject,
        "message" => $message,
        "phone" => $phone,
        "city" => $city,
        "status" => "New",
        "created_at" => date("Y-m-d H:i:s")
    ];

    $records[] = $newRecord;

    if (!saveRecords($jsonFile, $records)) {
        sendResponse(false, "Could not update about.json.", null, 500);
    }

    sendResponse(true, "New record added successfully.", $newRecord, 201);
}

sendResponse(false, "Only GET and POST requests are supported.", null, 405);
?>