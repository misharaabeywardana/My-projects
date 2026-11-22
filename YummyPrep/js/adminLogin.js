/* =========================================
   YUMMYPREP - ADMIN LOGIN
   ========================================= */

// Execute code when DOM content is fully loaded
document.addEventListener("DOMContentLoaded", function () {

    // Get DOM elements
    const adminLoginForm = document.getElementById("adminLoginForm");
    const adminEmail = document.getElementById("adminEmail");
    const adminPassword = document.getElementById("adminPassword");
    const loginMessage = document.getElementById("loginMessage");
    const togglePassword = document.getElementById("togglePassword");


    /* =========================================
       1. SHOW / HIDE PASSWORD TOGGLE
       ========================================= */
    if (togglePassword && adminPassword) {
        togglePassword.addEventListener("click", function () {
            if (adminPassword.type === "password") {
                adminPassword.type = "text";
                togglePassword.textContent = "Hide";
            } else {
                adminPassword.type = "password";
                togglePassword.textContent = "Show";
            }
        });
    }


    /* =========================================
       2. ADMIN LOGIN SUBMISSION
       ========================================= */
    if (adminLoginForm) {
        adminLoginForm.addEventListener("submit", function (event) {
            event.preventDefault(); // Prevent default page refresh

            const email = adminEmail ? adminEmail.value.trim() : "";
            const password = adminPassword ? adminPassword.value.trim() : "";

            // Client-side empty input validation
            if (email === "" || password === "") {
                if (loginMessage) {
                    loginMessage.textContent = "Please enter your admin email and password.";
                    loginMessage.className = "login-message error";
                }
                return;
            }

            // Prepare Form Data payload for PHP API
            const formData = new FormData();
            formData.append("action", "admin_login");
            formData.append("adminEmail", email);
            formData.append("adminPassword", password);

            // Display loading indicator in message box
            if (loginMessage) {
                loginMessage.textContent = "Authenticating admin credentials...";
                loginMessage.className = "login-message";
            }

            // Send AJAX POST request to auth_api.php
            fetch("../php/auth_api.php", {
                method: "POST",
                body: formData
            })
            .then(function (response) {
                return response.json();
            })
            .then(function (data) {
                if (data.status === "success") {
                    // Show success message
                    if (loginMessage) {
                        loginMessage.textContent = "Login successful. Opening dashboard...";
                        loginMessage.className = "login-message success";
                    }

                    // Save login state in sessionStorage
                    sessionStorage.setItem("yummyPrepAdminLoggedIn", "true");

                    // Redirect to Admin Dashboard after a brief delay
                    setTimeout(function () {
                        window.location.href = "adminDashboard.html";
                    }, 700);

                } else {
                    // Show error message returned by PHP backend
                    if (loginMessage) {
                        loginMessage.textContent = data.message || "Invalid administrator credentials.";
                        loginMessage.className = "login-message error";
                    }
                }
            })
            .catch(function (error) {
                console.error("Admin Login Error:", error);
                if (loginMessage) {
                    loginMessage.textContent = "Server error. Please check your XAMPP connection.";
                    loginMessage.className = "login-message error";
                }
            });
        });
    }
});