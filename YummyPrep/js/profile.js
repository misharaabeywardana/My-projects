/* =========================================
   YUMMYPREP - PROFILE SCRIPT (SUPER FIXED)
   ========================================= */

document.addEventListener("DOMContentLoaded", function () {
    const editProfileBtn = document.getElementById("editProfileBtn");

    // Load user profile and order summaries on page load
    loadUserProfile();
    loadOrderSummary();

    function loadUserProfile() {
        fetch("../php/users_api.php?action=get_profile")
            .then(res => res.json())
            .then(data => {
                if (data.status === "success" && data.data) {
                    const user = data.data;

                    // Set Text
                    document.getElementById("nameText").textContent = user.full_name || user.name || "Not provided";
                    document.getElementById("emailText").textContent = user.email || "Not provided";
                    document.getElementById("phoneText").textContent = user.phone || "Not provided";
                    document.getElementById("addressText").textContent = user.delivery_address || "Not provided";

                    document.getElementById("cardProfileName").textContent = user.full_name || user.name || "User";
                    document.getElementById("cardProfileEmail").textContent = user.email || "";
                    document.getElementById("cardProfilePhone").textContent = user.phone || "";

                    // Set Inputs
                    document.getElementById("nameInput").value = user.full_name || user.name || "";
                    document.getElementById("emailInput").value = user.email || "";
                    document.getElementById("phoneInput").value = user.phone || "";
                    document.getElementById("addressInput").value = user.delivery_address || "";

                    // Avatar
                    const profileImage = document.getElementById("profileImage");
                    const defaultIcon = document.getElementById("defaultAvatarIcon");
                    if (user.avatar && profileImage && defaultIcon) {
                        profileImage.src = user.avatar;
                        profileImage.style.display = "block";
                        defaultIcon.style.display = "none";
                    }
                } else {
                    window.location.href = "login.html";
                }
            })
            .catch(err => console.error("Error loading profile:", err));
    }

    function loadOrderSummary() {
        fetch("../php/orders_api.php?action=get_user_orders")
            .then(res => res.json())
            .then(data => {
                if (data.status === "success" && data.data) {
                    const orders = data.data;
                    let total = orders.length;
                    let completed = orders.filter(o => o.status === 'Completed' || o.status === 'Delivered').length;
                    let pending = total - completed;

                    const totalEl = document.getElementById("totalOrdersCount");
                    const compEl = document.getElementById("completedOrdersCount");
                    const pendEl = document.getElementById("pendingOrdersCount");

                    if (totalEl) totalEl.textContent = total;
                    if (compEl) compEl.textContent = completed;
                    if (pendEl) pendEl.textContent = pending;
                }
            })
            .catch(err => console.error("Error loading orders summary:", err));
    }

    // Toggle Edit Mode & Create Buttons
    if (editProfileBtn) {
        editProfileBtn.addEventListener("click", function () {
            document.body.classList.add("edit-mode");
            document.querySelectorAll(".information-box").forEach(box => {
                box.classList.add("editing");
            });
            editProfileBtn.style.display = "none";

            let actionContainer = document.getElementById("dynamicEditActions");
            if (!actionContainer) {
                actionContainer = document.createElement("div");
                actionContainer.id = "dynamicEditActions";
                actionContainer.style.cssText = "display: flex; gap: 15px; margin-top: 25px;";

                actionContainer.innerHTML = `
                    <button type="button" id="dynamicCancelBtn" style="padding: 12px 25px; background: #e0e0e0; color: #333; border: none; border-radius: 6px; cursor: pointer; font-weight: 600;">Cancel</button>
                    <button type="button" id="dynamicSaveBtn" style="padding: 12px 25px; background: #4CAF50; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: 600;">Save Changes</button>
                `;

                const infoGrid = document.getElementById("infoGridContainer");
                infoGrid.parentNode.insertBefore(actionContainer, infoGrid.nextSibling);

                // Cancel Button Action
                document.getElementById("dynamicCancelBtn").addEventListener("click", function () {
                    document.body.classList.remove("edit-mode");
                    document.querySelectorAll(".information-box").forEach(box => {
                        box.classList.remove("editing");
                    });
                    editProfileBtn.style.display = "block";
                    actionContainer.remove();
                    loadUserProfile();
                });

                // Save Changes Button Action
                document.getElementById("dynamicSaveBtn").addEventListener("click", function () {
                    // Extract exact values at the time of clicking
                    const updatedName = document.getElementById("nameInput").value.trim();
                    const updatedPhone = document.getElementById("phoneInput").value.trim();
                    const updatedAddress = document.getElementById("addressInput").value.trim();

                    if (!updatedName) {
                        alert("Please enter your name.");
                        return;
                    }

                    const formData = new URLSearchParams();
                    formData.append("action", "update_profile");
                    
                    // Sending both 'full_name' and 'name' to prevent PHP errors
                    formData.append("full_name", updatedName);
                    formData.append("name", updatedName);
                    formData.append("phone", updatedPhone);
                    formData.append("delivery_address", updatedAddress);

                    fetch("../php/users_api.php?action=update_profile", {
                        method: "POST",
                        headers: { "Content-Type": "application/x-www-form-urlencoded" },
                        body: formData.toString()
                    })
                    .then(res => res.json())
                    .then(data => {
                        if (data.status === "success") {
                            alert("Profile updated successfully!");
                            document.body.classList.remove("edit-mode");
                            document.querySelectorAll(".information-box").forEach(box => {
                                box.classList.remove("editing");
                            });
                            editProfileBtn.style.display = "block";
                            actionContainer.remove();
                            loadUserProfile();
                        } else {
                            alert(data.message || "Failed to update profile.");
                        }
                    })
                    .catch(err => {
                        console.error("Error updating profile:", err);
                        alert("An error occurred while saving changes.");
                    });
                });
            }
        });
    }

    // Logout handling
    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", function () {
            fetch("../php/auth_api.php?action=logout")
                .then(() => { window.location.href = "login.html"; });
        });
    }
});