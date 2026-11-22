<?php
header('Content-Type: application/json; charset=utf-8');

$jsonFile = __DIR__ . DIRECTORY_SEPARATOR . 'offers.json';

function sendResponse($success, $message, $extra = [], $status = 200) {
    http_response_code($status);
    echo json_encode(array_merge([
        'success' => $success,
        'message' => $message
    ], $extra), JSON_PRETTY_PRINT);
    exit;
}

function readRecords($jsonFile) {
    if (!file_exists($jsonFile)) {
        return [];
    }

    $json = file_get_contents($jsonFile);
    $records = json_decode($json, true);

    return is_array($records) ? $records : [];
}

function nextId($records) {
    $maxId = 0;
    foreach ($records as $record) {
        if (isset($record['id']) && is_numeric($record['id'])) {
            $maxId = max($maxId, (int)$record['id']);
        }
    }
    return $maxId + 1;
}

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $records = readRecords($jsonFile);

    if (isset($_GET['id']) && $_GET['id'] !== '') {
        $requestedId = (int)$_GET['id'];

        foreach ($records as $record) {
            if (isset($record['id']) && (int)$record['id'] === $requestedId) {
                sendResponse(true, 'One deal record loaded successfully.', ['record' => $record]);
            }
        }

        sendResponse(false, 'No deal record was found for ID ' . $requestedId . '.', [], 404);
    }

    sendResponse(true, 'All deal records loaded successfully.', [
        'count' => count($records),
        'records' => $records
    ]);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $required = ['name', 'discount', 'price', 'duration', 'breakfast', 'wifi', 'late_checkout'];

    foreach ($required as $field) {
        if (!isset($_POST[$field]) || trim((string)$_POST[$field]) === '') {
            sendResponse(false, 'Missing required field: ' . $field . '.', [], 400);
        }
    }

    $records = readRecords($jsonFile);

    $newRecord = [
        'id' => nextId($records),
        'name' => trim((string)$_POST['name']),
        'discount' => trim((string)$_POST['discount']),
        'price' => (float)$_POST['price'],
        'duration' => trim((string)$_POST['duration']),
        'breakfast' => trim((string)$_POST['breakfast']),
        'wifi' => trim((string)$_POST['wifi']),
        'late_checkout' => trim((string)$_POST['late_checkout'])
    ];

    $records[] = $newRecord;

    $json = json_encode($records, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
    if ($json === false || file_put_contents($jsonFile, $json, LOCK_EX) === false) {
        sendResponse(false, 'The JSON file could not be updated.', [], 500);
    }

    sendResponse(true, 'New deal record added and offers.json updated successfully.', [
        'record' => $newRecord,
        'count' => count($records)
    ], 201);
}

sendResponse(false, 'Only GET and POST requests are supported.', [], 405);
?>