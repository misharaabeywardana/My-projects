<?php

header("Content-Type: application/json");

$jsonFile = "data.json";

// GET REQUEST

if ($_SERVER["REQUEST_METHOD"] === "GET") {

    $data = file_get_contents($jsonFile);

    $records = json_decode($data, true);

    // Read one record
    if (isset($_GET["id"])) {

        $id = intval($_GET["id"]);

        foreach ($records as $record) {

            if ($record["id"] === $id) {

                echo json_encode([
                    "success" => true,
                    "data" => $record
                ]);

                exit;
            }
        }

        echo json_encode([
            "success" => false,
            "message" => "Hotel not found"
        ]);

        exit;
    }


    // Read all records
    echo json_encode([
        "success" => true,
        "data" => $records
    ]);

    exit;
}

// POST REQUEST

if ($_SERVER["REQUEST_METHOD"] === "POST") {

    $input = file_get_contents("php://input");

    $newRecord = json_decode($input, true);

    if (!$newRecord) {

        echo json_encode([
            "success" => false,
            "message" => "Invalid JSON data"
        ]);

        exit;
    }


    // Read existing records
    $data = file_get_contents($jsonFile);

    $records = json_decode($data, true);


    // Generate new ID
    $newId = 1;

    if (!empty($records)) {

        $ids = array_column($records, "id");

        $newId = max($ids) + 1;
    }


    $newRecord["id"] = $newId;


    // Add new record
    $records[] = $newRecord;


    // Save updated JSON file
    file_put_contents(
        $jsonFile,
        json_encode($records, JSON_PRETTY_PRINT)
    );


    echo json_encode([
        "success" => true,
        "message" => "New hotel added successfully",
        "data" => $newRecord
    ]);

    exit;
}

// INVALID REQUEST

echo json_encode([
    "success" => false,
    "message" => "Invalid request"
]);

?>