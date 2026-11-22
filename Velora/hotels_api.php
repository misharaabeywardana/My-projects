<?php

// Return all responses as JSON.
header("Content-Type: application/json");


// Location of the JSON data file.
$jsonFile =
    __DIR__ . "/hotels.json";


// Check whether hotels.json exists.
if (!file_exists($jsonFile)) {

    echo json_encode([
        "success" => false,
        "message" => "JSON data file not found."
    ]);

    exit;
}


// Read the JSON file.
$jsonData =
    file_get_contents($jsonFile);


// Convert JSON data into a PHP array.
$hotels =
    json_decode($jsonData, true);


// Use an empty array if the JSON data is invalid.
if (!is_array($hotels)) {

    $hotels = [];

}


// 1. GET REQUEST

if ($_SERVER["REQUEST_METHOD"] === "GET") {

    // (Button-2-tittle) FIND ONE HOTEL BY ID

    if (isset($_GET["id"])) {

        // Read the ID sent from hotels.html.
        $hotelId =
            intval($_GET["id"]);


        // Search each hotel record.
        foreach ($hotels as $hotel) {

            // Check whether the IDs match.
            if (
                intval($hotel["id"]) === $hotelId
            ) {

                // Return the matching hotel.
                echo json_encode([
                    "success" => true,
                    "data" => $hotel
                ]);

                exit;
            }

        }


        // No matching ID was found.
        echo json_encode([
            "success" => false,
            "message" => "Hotel record not found."
        ]);

        exit;
    }

    // (Button-1) LOAD ALL HOTELS

    echo json_encode([
        "success" => true,
        "data" => $hotels
    ]);

    exit;
}


// 2. POST REQUEST - 
// (Button-3-tittle) ADD NEW HOTEL

if ($_SERVER["REQUEST_METHOD"] === "POST") {


    // Read Hotel Name.
    $name =
        trim(
            $_POST["name"] ?? ""
        );


    // Read Destination.
    $destination =
        trim(
            $_POST["destination"] ?? ""
        );


    // Read Room Type.
    $type =
        trim(
            $_POST["type"] ?? ""
        );


    // Read Price.
    $price =
        trim(
            $_POST["price"] ?? ""
        );


    // Read Image Path.
    $image =
        trim(
            $_POST["image"] ?? ""
        );


    // Check whether every field has a value.
    if (
        $name === "" ||
        $destination === "" ||
        $type === "" ||
        $price === "" ||
        $image === ""
    ) {

        echo json_encode([
            "success" => false,
            "message" => "Please provide all hotel details."
        ]);

        exit;
    }


    // Default ID for the first hotel.
    $newId = 1;


    // Create the next ID if records already exist.
    if (count($hotels) > 0) {

        // Get all existing IDs.
        $ids =
            array_column(
                $hotels,
                "id"
            );


        // Highest ID + 1 becomes the new ID.
        $newId =
            max($ids) + 1;

    }


    // Create the new hotel record.
    $newHotel = [

        "id" => $newId,

        "name" => $name,

        "destination" => $destination,

        "type" => $type,

        "price" => intval($price),

        "image" => $image

    ];


    // Add the new record to the array.
    $hotels[] =
        $newHotel;


    // Convert the PHP array back into JSON and save it in hotels.json
    $saved =
        file_put_contents(

            $jsonFile,

            json_encode(
                $hotels,
                JSON_PRETTY_PRINT
            )

        );


    // Check whether the JSON file was updated.
    if ($saved !== false) {

        echo json_encode([
            "success" => true,
            "message" => "Hotel added successfully.",
            "data" => $newHotel
        ]);

    } else {

        echo json_encode([
            "success" => false,
            "message" => "Unable to update JSON file."
        ]);

    }


    exit;
}


// Return an error for unsupported request methods.
echo json_encode([
    "success" => false,
    "message" => "Invalid request method."
]);

?>