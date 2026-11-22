/* =========================================
   YUMMYPREP - CONTACT US FORM
   ========================================= */

document.addEventListener("DOMContentLoaded", function () {

    const submitBtn = document.querySelector(".send-message-btn");
    const contactForm = submitBtn ? submitBtn.closest("form") : null;
    if (!contactForm) return;

    contactForm.addEventListener("submit", function (event) {
        event.preventDefault();

        const name = document.getElementById("name").value.trim();
        const email = document.getElementById("email").value.trim();
        const phone = document.getElementById("phone").value.trim();
        const subject = document.getElementById("subject").value;
        const message = document.getElementById("message").value.trim();

        /* ---- Client-side validation ---- */
        if (name.length < 2) {
            alert("Please enter your name.");
            return;
        }

        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            alert("Please enter a valid email address.");
            return;
        }

        if (phone !== "" && !/^[0-9+\-\s()]{7,20}$/.test(phone)) {
            alert("Please enter a valid phone number.");
            return;
        }

        if (subject === "") {
            alert("Please select a subject.");
            return;
        }

        if (message.length < 5) {
            alert("Please enter your message.");
            return;
        }

        /* ---- Send to PHP backend ---- */
        const formData = new FormData();
        formData.append("action", "submit");
        formData.append("name", name);
        formData.append("email", email);
        formData.append("phone", phone);
        formData.append("subject", subject);
        formData.append("message", message);

        submitBtn.disabled = true;

        fetch("../php/contact_api.php", {
            method: "POST",
            body: formData
        })
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            alert(data.message || (data.status === "success" ? "Message sent!" : "Could not send your message."));
            if (data.status === "success") {
                contactForm.reset();
            }
        })
        .catch(function (error) {
            console.error("Contact form error:", error);
            alert("An error occurred while connecting to the server.");
        })
        .finally(function () {
            submitBtn.disabled = false;
        });
    });
});
