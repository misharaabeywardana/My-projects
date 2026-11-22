/* =========================================
   YUMMYPREP - CUSTOMER MEALS CATALOG
   ========================================= */

// Global array to store meals loaded from MySQL
let customerMeals = [];

// Execute code when DOM content is fully loaded
document.addEventListener("DOMContentLoaded", function () {
    // Load available meals from MySQL database API
    loadMealsFromDatabase();
});


/* =========================================
   1. FETCH MEALS FROM DATABASE
   ========================================= */
function loadMealsFromDatabase() {
    fetch("../php/meals_api.php?action=get_all")
        .then(function (response) {
            return response.json();
        })
        .then(function (res) {
            if (res.status === "success" && Array.isArray(res.data)) {
                customerMeals = res.data;
                displayCustomerMeals();
            } else {
                console.error("Failed to fetch meals:", res.message);
            }
        })
        .catch(function (error) {
            console.error("Error connecting to meals API:", error);
        });
}


/* =========================================
   2. CREATE SINGLE MEAL CARD HTML
   ========================================= */
function createMealCard(meal) {
    // Construct link to mealDetails.html using meal database ID
    const detailURL = `mealDetails.html?id=${encodeURIComponent(meal.id)}`;

    // Resolve proper image URL path
    let imageSrc = meal.image_url || meal.image || "../images/chickenBowl1.jpeg";
    if (!imageSrc.startsWith("../") && !imageSrc.startsWith("http")) {
        imageSrc = "../images/" + imageSrc;
    }

    return `
        <div class="menu-card">
            <div class="menu-card-image" style="background-image: url('${imageSrc}');">
            </div>

            <div class="menu-card-content">
                <h3>${meal.name}</h3>

                <p>${meal.description}</p>

                <div class="menu-card-bottom">
                    <strong>Rs. ${parseFloat(meal.price).toFixed(2)}</strong>

                    <a href="${detailURL}">
                        View Details →
                    </a>
                </div>
            </div>
        </div>
    `;
}


/* =========================================
   3. RENDER CATEGORY GRID
   ========================================= */
function displayCategory(category, containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    // Filter available meals matching current category
    const categoryMeals = customerMeals.filter(function (meal) {
        const isAvailable = meal.is_available == 1 || meal.status === "Available";
        return meal.category === category && isAvailable;
    });

    if (categoryMeals.length === 0) {
        container.innerHTML = `
            <div class="empty-category">
                <p>No meals are currently available in this category.</p>
            </div>
        `;
        return;
    }

    let html = "";
    categoryMeals.forEach(function (meal) {
        html += createMealCard(meal);
    });

    container.innerHTML = html;
}


/* =========================================
   4. RENDER ALL CATEGORY SECTIONS
   ========================================= */
function displayCustomerMeals() {
    displayCategory("Rice Bowl", "riceBowlsGrid");
    displayCategory("Salad", "saladsGrid");
    displayCategory("Grain Bowl", "grainBowlsGrid");
    displayCategory("Bread-Based", "breadGrid");
    displayCategory("Beverage", "beveragesGrid");
}