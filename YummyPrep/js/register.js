/* =========================================
   YUMMYPREP - CUSTOMER REGISTRATION (ALERT -> AUTO-LOGIN TO PROFILE)
   ========================================= */

document.addEventListener("DOMContentLoaded", function () {

    const registerForm = document.getElementById("registerForm");
    if (!registerForm) return;

    registerForm.addEventListener("submit", function (event) {
        event.preventDefault();

        const fullName = document.getElementById("fullName").value.trim();
        const email = document.getElementById("email").value.trim();
        const phone = document.getElementById("phone").value.trim();
        const password = document.getElementById("password").value;
        const confirmPassword = document.getElementById("confirmPassword").value;
        const terms = document.getElementById("terms");

        if (fullName.length < 2) {
            alert("Please enter your full name.");
            return;
        }

        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            alert("Please enter a valid email address.");
            return;
        }

        if (password.length < 8) {
            alert("Password must be at least 8 characters long.");
            return;
        }

        if (password !== confirmPassword) {
            alert("Passwords do not match.");
            return;
        }

        if (terms && !terms.checked) {
            alert("Please accept the Terms & Conditions.");
            return;
        }

        const formData = new FormData();
        formData.append("action", "register");
        formData.append("fullName", fullName);
        formData.append("email", email);
        formData.append("phone", phone);
        formData.append("password", password);
        formData.append("confirmPassword", confirmPassword);

        fetch("../php/auth_api.php", {
            method: "POST",
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.status === "success") {

                const userData = data.data;

                // Storage keys සියල්ල Save කිරීම (profile.js එකෙන් Redirect නොවීම සඳහා)
                sessionStorage.setItem("yummyPrepLoggedIn", "true");
                sessionStorage.setItem("yummyPrepUser", JSON.stringify(userData));
                sessionStorage.setItem("yummyPrepCurrentUser", JSON.stringify(userData));

                localStorage.setItem("yummyPrepLoggedIn", "true");
                localStorage.setItem("yummyPrepUser", JSON.stringify(userData));
                localStorage.setItem("yummyPrepCurrentUser", JSON.stringify(userData));

                // 1. Alert එක පෙන්වීම
                alert("Account created successfully! Welcome to YummyPrep.");

                // 2. Alert එක OK කළ පසු කෙළින්ම Profile පිටුවට යොමු කිරීම (Login Form එකට නොගොස්)
                window.location.href = "profile.html";

            } else {
                alert(data.message || "Registration failed.");
            }
        })
        .catch(error => {
            console.error("Registration Error:", error);
            alert("An error occurred while connecting to the server.");
        });
    });
});