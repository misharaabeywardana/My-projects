/* =========================================
   YUMMYPREP - MEAL MANAGEMENT (ADMIN)
   ========================================= */

// Global array to store meals loaded from MySQL
let meals = [];

// Execute code when DOM content is fully loaded
document.addEventListener("DOMContentLoaded", function () {

    /* =========================================
       1. DOM ELEMENTS
       ========================================= */
    const mealGrid = document.getElementById("mealGrid");
    const mealSearch = document.getElementById("mealSearch");
    const categoryFilter = document.getElementById("categoryFilter");
    const availabilityFilter = document.getElementById("availabilityFilter");
    const mealModal = document.getElementById("mealModal");
    const mealForm = document.getElementById("mealForm");
    const modalTitle = document.getElementById("modalTitle");
    const addMealBtn = document.getElementById("addMealBtn");
    const closeModalBtn = document.getElementById("closeModalBtn");
    const cancelMealBtn = document.getElementById("cancelMealBtn");
    const logoutBtn = document.getElementById("logoutBtn");


    /* =========================================
       2. ADMIN SESSION CHECK
       ========================================= */
    function checkAdminSession() {
        fetch("../php/auth_api.php?action=check_session")
            .then(function (response) {
                return response.json();
            })
            .then(function (data) {
                if (data.status !== "success" || data.data.type !== "admin") {
                    window.location.href = "adminLogin.html";
                }
            })
            .catch(function (error) {
                console.error("Session check error:", error);
                window.location.href = "adminLogin.html";
            });
    }


    /* =========================================
       3. FETCH MEALS FROM DATABASE
       ========================================= */
    function loadMealsFromDatabase() {
        fetch("../php/meals_api.php?action=get_all")
            .then(function (response) {
                return response.json();
            })
            .then(function (res) {
                if (res.status === "success") {
                    meals = res.data;
                    updateMealStats();
                    displayMeals();
                } else {
                    console.error("Failed to load meals:", res.message);
                }
            })
            .catch(function (error) {
                console.error("Error loading meals from database:", error);
            });
    }


    /* =========================================
       4. UPDATE MEAL STATS / COUNTERS
       ========================================= */
    function updateMealStats() {
        const totalMeals = document.getElementById("totalMeals");
        const availableMeals = document.getElementById("availableMeals");
        const unavailableMeals = document.getElementById("unavailableMeals");

        if (totalMeals) totalMeals.textContent = meals.length;

        if (availableMeals) {
            availableMeals.textContent = meals.filter(function (meal) {
                return meal.is_available == 1 || meal.status === "Available";
            }).length;
        }

        if (unavailableMeals) {
            unavailableMeals.textContent = meals.filter(function (meal) {
                return meal.is_available == 0 || meal.status === "Unavailable";
            }).length;
        }
    }


    /* =========================================
       5. DISPLAY & FILTER MEAL CARDS
       ========================================= */
    function displayMeals() {
        if (!mealGrid) return;

        const searchValue = mealSearch ? mealSearch.value.toLowerCase().trim() : "";
        const category = categoryFilter ? categoryFilter.value : "all";
        const availability = availabilityFilter ? availabilityFilter.value : "all";

        // Filter meal list based on search term, category, and availability status
        const filteredMeals = meals.filter(function (meal) {
            const matchesSearch = meal.name.toLowerCase().includes(searchValue);
            const matchesCategory = category === "all" || meal.category === category;

            const isAvailableStr = (meal.is_available == 1 || meal.status === "Available") ? "Available" : "Unavailable";
            const matchesAvailability = availability === "all" || isAvailableStr === availability;

            return matchesSearch && matchesCategory && matchesAvailability;
        });

        if (filteredMeals.length === 0) {
            mealGrid.innerHTML = `<div class="empty-meals">No meals found.</div>`;
            return;
        }

        let html = "";

        filteredMeals.forEach(function (meal) {
            const isAvailable = meal.is_available == 1 || meal.status === "Available";
            const availabilityClass = isAvailable ? "available" : "unavailable";
            const availabilityStatusText = isAvailable ? "Available" : "Unavailable";
            const availabilityButtonText = isAvailable ? "Disable" : "Enable";

            // Resolve proper image URL
            let imageSrc = meal.image_url || meal.image || "../images/chickenBowl1.jpeg";
            if (!imageSrc.startsWith("../") && !imageSrc.startsWith("http")) {
                imageSrc = "../images/" + imageSrc;
            }

            html += `
                <article class="admin-meal-card">
                    <img src="${imageSrc}" alt="${meal.name}" class="admin-meal-image">

                    <div class="admin-meal-content">
                        <div class="meal-card-top">
                            <span class="meal-category">${meal.category}</span>
                            <span class="availability-badge ${availabilityClass}">
                                ${availabilityStatusText}
                            </span>
                        </div>

                        <h3>${meal.name}</h3>
                        <p>${meal.description}</p>
                        <strong class="meal-price">Rs. ${parseFloat(meal.price).toFixed(2)}</strong>

                        <div class="meal-actions">
                            <button type="button" class="meal-action-btn edit-btn" onclick="editMeal(${meal.id})">
                                Edit
                            </button>

                            <button type="button" class="meal-action-btn status-btn" onclick="toggleMealStatus(${meal.id})">
                                ${availabilityButtonText}
                            </button>

                            <button type="button" class="meal-action-btn delete-btn" onclick="deleteMeal(${meal.id})">
                                Delete
                            </button>
                        </div>
                    </div>
                </article>
            `;
        });

        mealGrid.innerHTML = html;
    }


    /* =========================================
       6. MODAL DIALOG CONTROLS
       ========================================= */
    function openModal(title) {
        if (modalTitle) modalTitle.textContent = title || "Add New Meal";
        if (mealModal) mealModal.classList.add("show");
    }

    function closeModal() {
        if (mealModal) mealModal.classList.remove("show");
    }

    if (addMealBtn) {
        addMealBtn.addEventListener("click", function () {
            if (mealForm) mealForm.reset();
            const mealIdInput = document.getElementById("mealId");
            if (mealIdInput) mealIdInput.value = "";
            openModal("Add New Meal");
        });
    }

    if (closeModalBtn) closeModalBtn.addEventListener("click", closeModal);
    if (cancelMealBtn) cancelMealBtn.addEventListener("click", closeModal);


    /* =========================================
       7. ADD / UPDATE MEAL SUBMISSION
       ========================================= */
    if (mealForm) {
        mealForm.addEventListener("submit", function (event) {
            event.preventDefault();

            const id = document.getElementById("mealId") ? document.getElementById("mealId").value : "";
            const name = document.getElementById("mealName") ? document.getElementById("mealName").value.trim() : "";
            const price = document.getElementById("mealPrice") ? document.getElementById("mealPrice").value : 0;
            const category = document.getElementById("mealCategory") ? document.getElementById("mealCategory").value : "Rice Bowl";
            const status = document.getElementById("mealStatus") ? document.getElementById("mealStatus").value : "Available";
            const image = document.getElementById("mealImage") ? document.getElementById("mealImage").value.trim() : "";
            const description = document.getElementById("mealDescription") ? document.getElementById("mealDescription").value.trim() : "";

            if (!name || !price) {
                alert("Please provide at least a meal name and price.");
                return;
            }

            // Generate unique meal code for new items
            const mealCode = "MEAL" + Math.floor(100 + Math.random() * 900);
            const isAvailable = status === "Available" ? 1 : 0;

            // Construct FormData object to send to PHP API
            const formData = new FormData();
            formData.append("action", "save_meal");
            if (id) formData.append("id", id);
            formData.append("mealCode", mealCode);
            formData.append("name", name);
            formData.append("price", price);
            formData.append("category", category);
            formData.append("description", description);
            formData.append("imageUrl", image || "../images/chickenBowl1.jpeg");
            formData.append("isAvailable", isAvailable);

            // Send save request to PHP backend
            fetch("../php/meals_api.php", {
                method: "POST",
                body: formData
            })
            .then(function (response) {
                return response.json();
            })
            .then(function (data) {
                if (data.status === "success") {
                    alert(data.message || "Meal saved successfully.");
                    closeModal();
                    loadMealsFromDatabase(); // Reload meals list from database
                } else {
                    alert(data.message || "Failed to save meal.");
                }
            })
            .catch(function (error) {
                console.error("Error saving meal:", error);
                alert("An error occurred while saving the meal.");
            });
        });
    }


    /* =========================================
       8. GLOBAL ACTION HANDLERS (EDIT / STATUS / DELETE)
       ========================================= */

    // Edit Meal - Fill modal inputs with selected meal data
    window.editMeal = function (id) {
        const meal = meals.find(function (item) {
            return item.id == id;
        });

        if (!meal) return;

        if (document.getElementById("mealId")) document.getElementById("mealId").value = meal.id;
        if (document.getElementById("mealName")) document.getElementById("mealName").value = meal.name;
        if (document.getElementById("mealPrice")) document.getElementById("mealPrice").value = meal.price;
        if (document.getElementById("mealCategory")) document.getElementById("mealCategory").value = meal.category;
        if (document.getElementById("mealStatus")) {
            document.getElementById("mealStatus").value = (meal.is_available == 1 || meal.status === "Available") ? "Available" : "Unavailable";
        }
        if (document.getElementById("mealImage")) document.getElementById("mealImage").value = meal.image_url || meal.image || "";
        if (document.getElementById("mealDescription")) document.getElementById("mealDescription").value = meal.description;

        openModal("Edit Meal");
    };

    // Toggle meal availability status
    window.toggleMealStatus = function (id) {
        const meal = meals.find(function (item) {
            return item.id == id;
        });

        if (!meal) return;

        const currentAvailable = meal.is_available == 1 || meal.status === "Available";
        const newAvailable = currentAvailable ? 0 : 1;

        const formData = new FormData();
        formData.append("action", "save_meal");
        formData.append("id", meal.id);
        formData.append("mealCode", meal.meal_code || ("MEAL" + meal.id));
        formData.append("name", meal.name);
        formData.append("category", meal.category);
        formData.append("price", meal.price);
        formData.append("description", meal.description);
        formData.append("imageUrl", meal.image_url || meal.image || "../images/chickenBowl1.jpeg");
        formData.append("isAvailable", newAvailable);

        fetch("../php/meals_api.php", {
            method: "POST",
            body: formData
        })
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            if (data.status === "success") {
                loadMealsFromDatabase();
            } else {
                alert(data.message || "Failed to update meal status.");
            }
        })
        .catch(function (error) {
            console.error("Error toggling status:", error);
        });
    };

    // Delete meal record from MySQL database
    window.deleteMeal = function (id) {
        const confirmation = confirm("Are you sure you want to delete this meal?");
        if (!confirmation) return;

        const formData = new FormData();
        formData.append("action", "delete_meal");
        formData.append("id", id);

        fetch("../php/meals_api.php", {
            method: "POST",
            body: formData
        })
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            if (data.status === "success") {
                alert(data.message || "Meal deleted successfully.");
                loadMealsFromDatabase();
            } else {
                alert(data.message || "Failed to delete meal.");
            }
        })
        .catch(function (error) {
            console.error("Error deleting meal:", error);
            alert("An error occurred while deleting the meal.");
        });
    };


    /* =========================================
       9. SEARCH & FILTER EVENT LISTENERS
       ========================================= */
    if (mealSearch) mealSearch.addEventListener("input", displayMeals);
    if (categoryFilter) categoryFilter.addEventListener("change", displayMeals);
    if (availabilityFilter) availabilityFilter.addEventListener("change", displayMeals);


    /* =========================================
       10. LOGOUT HANDLER
       ========================================= */
    if (logoutBtn) {
        logoutBtn.addEventListener("click", function () {
            fetch("../php/auth_api.php?action=logout")
                .then(function () {
                    sessionStorage.removeItem("yummyPrepAdminLoggedIn");
                    window.location.href = "adminLogin.html";
                })
                .catch(function () {
                    window.location.href = "adminLogin.html";
                });
        });
    }


    /* =========================================
       11. INITIALIZATION
       ========================================= */
    checkAdminSession();
    loadMealsFromDatabase();
});