<?php
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

$jsonFile = __DIR__ . DIRECTORY_SEPARATOR . 'my_bookings.json';

function sendResponse($success, $message, $data = null, $statusCode = 200) {
    http_response_code($statusCode);
    echo json_encode([
        'success' => $success,
        'message' => $message,
        'data' => $data
    ], JSON_PRETTY_PRINT);
    exit;
}

function readBookings($jsonFile) {
    if (!file_exists($jsonFile)) {
        return [];
    }

    $jsonText = file_get_contents($jsonFile);
    if ($jsonText === false) {
        sendResponse(false, 'Unable to read the JSON file.', null, 500);
    }

    // Required class-task concept: convert JSON text into a PHP array.
    $bookings = json_decode($jsonText, true);

    if (!is_array($bookings)) {
        sendResponse(false, 'The JSON file contains invalid data.', null, 500);
    }

    return $bookings;
}

// GET request: return all records, or one record when an ID is supplied.
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $bookings = readBookings($jsonFile);

    if (isset($_GET['id']) && trim($_GET['id']) !== '') {
        $requestedId = trim($_GET['id']);

        foreach ($bookings as $booking) {
            if (isset($booking['id']) && strcasecmp($booking['id'], $requestedId) === 0) {
                sendResponse(true, 'Booking found.', $booking);
            }
        }

        sendResponse(false, 'No booking found with ID ' . $requestedId . '.', null, 404);
    }

    sendResponse(true, 'All bookings returned.', $bookings);
}

// POST request: receive JSON from AJAX and append a new record.
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $rawInput = file_get_contents('php://input');
    $newBooking = json_decode($rawInput, true);

    if (!is_array($newBooking)) {
        sendResponse(false, 'Invalid JSON was sent to the server.', null, 400);
    }

    $requiredFields = ['hotel', 'room', 'checkIn', 'checkOut', 'guests', 'destination', 'status'];
    foreach ($requiredFields as $field) {
        if (!isset($newBooking[$field]) || trim((string)$newBooking[$field]) === '') {
            sendResponse(false, 'Please complete all booking fields.', null, 400);
        }
    }

    if ($newBooking['checkOut'] <= $newBooking['checkIn']) {
        sendResponse(false, 'Check-out date must be after check-in date.', null, 400);
    }

    $bookings = readBookings($jsonFile);

    // Generate a simple unique booking ID on the server.
    do {
        $newId = 'VLR' . random_int(1000, 9999);
        $idExists = false;
        foreach ($bookings as $booking) {
            if (($booking['id'] ?? '') === $newId) {
                $idExists = true;
                break;
            }
        }
    } while ($idExists);

    $record = [
        'id' => $newId,
        'hotel' => trim($newBooking['hotel']),
        'room' => trim($newBooking['room']),
        'checkIn' => $newBooking['checkIn'],
        'checkOut' => $newBooking['checkOut'],
        'guests' => (int)$newBooking['guests'],
        'destination' => trim($newBooking['destination']),
        'status' => trim($newBooking['status'])
    ];

    $bookings[] = $record;

    $saved = file_put_contents(
        $jsonFile,
        json_encode($bookings, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES),
        LOCK_EX
    );

    if ($saved === false) {
        sendResponse(false, 'Could not update my_bookings.json. Check file permissions.', null, 500);
    }

    sendResponse(true, 'Booking added successfully.', $record, 201);
}

sendResponse(false, 'Only GET and POST requests are supported.', null, 405);
?>