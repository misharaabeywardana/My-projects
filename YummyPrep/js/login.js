/* =========================================
   YUMMYPREP - LOGIN SCRIPT (ROLE-BASED REDIRECT)
   ========================================= */

// Execute code when the DOM content is fully loaded
document.addEventListener("DOMContentLoaded", function () {

    /* =========================================
       1. GET DOM ELEMENTS
       ========================================= */
    const loginForm = document.getElementById("loginForm");
    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");
    const rememberCheckbox = document.getElementById("remember");


    /* =========================================
       2. LOGIN SUBMISSION & AUTHENTICATION
       ========================================= */
    if (loginForm) {
        loginForm.addEventListener("submit", function (event) {
            event.preventDefault(); // Prevent page refresh on form submit

            const email = emailInput ? emailInput.value.trim() : "";
            const password = passwordInput ? passwordInput.value.trim() : "";

            // Client-side empty field validation
            if (email === "" || password === "") {
                alert("Please enter both your email address and password.");
                return;
            }

            // Create FormData object to send to PHP API
            const formData = new FormData();
            formData.append("action", "login");
            formData.append("email", email);
            formData.append("password", password);
            formData.append("remember", rememberCheckbox && rememberCheckbox.checked ? "1" : "0");

            // Send AJAX POST request to backend API
            fetch("../php/auth_api.php", {
                method: "POST",
                body: formData
            })
            .then(function (response) {
                return response.json();
            })
            .then(function (data) {
                if (data.status === "success") {

                    // Clear any previous login storage items
                    localStorage.removeItem("yummyPrepLoggedIn");
                    localStorage.removeItem("yummyPrepCurrentUser");
                    localStorage.removeItem("yummyPrepUser");
                    sessionStorage.removeItem("yummyPrepLoggedIn");
                    sessionStorage.removeItem("yummyPrepCurrentUser");
                    sessionStorage.removeItem("yummyPrepUser");

                    // Save customer/admin user object returned from database
                    const userData = data.data;

                    // Handle "Remember Me" checkbox selection
                    if (rememberCheckbox && rememberCheckbox.checked) {
                        localStorage.setItem("yummyPrepLoggedIn", "true");
                        localStorage.setItem("yummyPrepUser", JSON.stringify(userData));
                    } else {
                        sessionStorage.setItem("yummyPrepLoggedIn", "true");
                        sessionStorage.setItem("yummyPrepUser", JSON.stringify(userData));
                    }

                    alert("Login successful! Welcome back to YummyPrep.");

                    // Role-based Redirect (Admin -> adminDashboard.html | Customer -> profile.html)
                    if (userData && userData.type === "admin") {
                        window.location.href = "adminDashboard.html";
                    } else {
                        window.location.href = "profile.html";
                    }

                } else {
                    // Show error message returned by PHP backend
                    alert(data.message || "Invalid email address or password.");
                }
            })
            .catch(function (error) {
                console.error("Login Error:", error);
                alert("An error occurred during login. Please check your connection.");
            });
        });
    }
});