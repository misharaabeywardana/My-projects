/* =========================================
   YUMMYPREP - MEAL DETAILS
   ========================================= */

// Global variable to store active meal details
let currentMeal = null;
let quantity = 1;

// Execute code when DOM content is fully loaded
document.addEventListener("DOMContentLoaded", function () {

    /* =========================================
       1. READ MEAL PARAMETER FROM URL
       ========================================= */
    const params = new URLSearchParams(window.location.search);
    const mealParam = params.get("id") || params.get("meal") || "1";

    // Load meal details from PHP backend API
    fetchMealDetails(mealParam);

    // Attach quantity adjustment listeners
    setupQuantityControls();
});


/* =========================================
   2. FETCH MEAL DETAILS FROM DATABASE
   ========================================= */
function fetchMealDetails(param) {
    // Determine if parameter is an ID or meal slug/code
    const apiUrl = `../php/meals_api.php?action=get_details&id=${encodeURIComponent(param)}`;

    fetch(apiUrl)
        .then(function (response) {
            return response.json();
        })
        .then(function (res) {
            if (res.status === "success" && res.data) {
                currentMeal = res.data;
                displayMealDetails(currentMeal);
            } else {
                // If ID lookup fails, fetch all meals and match by name/code
                fetchFallbackMeal(param);
            }
        })
        .catch(function (error) {
            console.error("Error fetching meal details:", error);
            fetchFallbackMeal(param);
        });
}

// Fallback search across all meals
function fetchFallbackMeal(param) {
    fetch("../php/meals_api.php?action=get_all")
        .then(function (res) {
            return res.json();
        })
        .then(function (res) {
            if (res.status === "success" && res.data.length > 0) {
                // Find matching meal by code or name
                const matched = res.data.find(function (item) {
                    return item.id == param ||
                           item.meal_code.toLowerCase() === param.toLowerCase() ||
                           item.name.toLowerCase().includes(param.toLowerCase().replace(/-/g, " "));
                }) || res.data[0]; // Default to first meal if match not found

                currentMeal = matched;
                displayMealDetails(currentMeal);
            }
        });
}


/* =========================================
   3. DISPLAY MEAL INFORMATION
   ========================================= */
function displayMealDetails(meal) {
    // Target HTML elements
    const mealName = document.getElementById("mealName");
    const mealDescription = document.getElementById("mealDescription");
    const mealPrice = document.getElementById("mealPrice");
    const mealImage = document.getElementById("mealImage");
    const calories = document.getElementById("calories");
    const protein = document.getElementById("protein");
    const carbs = document.getElementById("carbs");
    const fat = document.getElementById("fat");
    const ingredientsList = document.getElementById("mealIngredients");

    // Format price display
    const formattedPrice = typeof meal.price === "number"
        ? "Rs. " + meal.price.toFixed(2)
        : (String(meal.price).startsWith("Rs.") ? meal.price : "Rs. " + meal.price);

    if (mealName) mealName.textContent = meal.name;
    if (mealDescription) mealDescription.textContent = meal.description;
    if (mealPrice) mealPrice.textContent = formattedPrice;

    if (mealImage) {
        mealImage.src = meal.image_url || meal.image || "../images/chickenBowl1.jpeg";
        mealImage.alt = meal.name;
    }

    // Nutritional breakdown values
    if (calories) calories.textContent = meal.calories || "0";
    if (protein) protein.textContent = meal.protein || "0g";
    if (carbs) carbs.textContent = meal.carbs || "0g";
    if (fat) fat.textContent = meal.fat || "0g";

    // Populate ingredients list items
    if (ingredientsList) {
        ingredientsList.innerHTML = "";

        let ingredientsArray = [];
        if (Array.isArray(meal.ingredients)) {
            ingredientsArray = meal.ingredients;
        } else if (typeof meal.ingredients === "string") {
            ingredientsArray = meal.ingredients.split(",").map(function (s) {
                return s.trim();
            });
        }

        ingredientsArray.forEach(function (ingredient) {
            if (ingredient) {
                const li = document.createElement("li");
                li.textContent = ingredient;
                ingredientsList.appendChild(li);
            }
        });
    }
}


/* =========================================
   4. QUANTITY CONTROLS
   ========================================= */
function setupQuantityControls() {
    const qtyDisplay = document.getElementById("quantity");
    if (qtyDisplay) qtyDisplay.textContent = quantity;
}

function increaseQuantity() {
    quantity++;
    const qtyDisplay = document.getElementById("quantity");
    if (qtyDisplay) qtyDisplay.textContent = quantity;
}

function decreaseQuantity() {
    if (quantity > 1) {
        quantity--;
        const qtyDisplay = document.getElementById("quantity");
        if (qtyDisplay) qtyDisplay.textContent = quantity;
    }
}


/* =========================================
   5. ADD TO CART FUNCTIONALITY
   ========================================= */
function addToCart() {
    if (!currentMeal) {
        alert("Meal details are still loading. Please try again.");
        return;
    }

    // Retrieve existing cart array from LocalStorage
    let cart = JSON.parse(localStorage.getItem("yummyPrepCart")) || [];

    const mealPriceNumeric = typeof currentMeal.price === "number"
        ? currentMeal.price
        : Number(String(currentMeal.price).replace("Rs.", "").replace(/,/g, "").trim());

    // Check if meal is already inside cart
    const existingIndex = cart.findIndex(function (item) {
        return item.mealId === currentMeal.id || item.name === currentMeal.name;
    });

    if (existingIndex > -1) {
        cart[existingIndex].quantity += quantity;
    } else {
        cart.push({
            mealId: currentMeal.id,
            name: currentMeal.name,
            category: currentMeal.category || "Standard",
            price: mealPriceNumeric,
            image: currentMeal.image_url || currentMeal.image || "../images/chickenBowl1.jpeg",
            quantity: quantity
        });
    }

    // Save updated cart to LocalStorage
    localStorage.setItem("yummyPrepCart", JSON.stringify(cart));

    alert(`${currentMeal.name} × ${quantity} added to your cart!`);
}

// Bind functions to global window scope for inline HTML button click attributes
window.increaseQuantity = increaseQuantity;
window.decreaseQuantity = decreaseQuantity;
window.addToCart = addToCart;