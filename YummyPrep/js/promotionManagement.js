/* =========================================
   YUMMYPREP - PROMOTION MANAGEMENT (ADMIN)
   ========================================= */

// Global array to store promotions loaded from MySQL database
let promotions = [];

// Execute code when DOM content is fully loaded
document.addEventListener("DOMContentLoaded", function () {

    /* =========================================
       1. DOM ELEMENTS
       ========================================= */
    const promotionGrid = document.getElementById("promotionGrid");
    const promotionSearch = document.getElementById("promotionSearch");
    const promotionStatusFilter = document.getElementById("promotionStatusFilter");
    const promotionModal = document.getElementById("promotionModal");
    const promotionForm = document.getElementById("promotionForm");
    const modalTitle = document.getElementById("modalTitle");
    const addPromotionBtn = document.getElementById("addPromotionBtn");
    const closeModalBtn = document.getElementById("closeModalBtn");
    const cancelPromotionBtn = document.getElementById("cancelPromotionBtn");
    const logoutBtn = document.getElementById("logoutBtn");


    /* =========================================
       2. ADMIN SESSION VERIFICATION
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
                console.error("Session verification error:", error);
                window.location.href = "adminLogin.html";
            });
    }


    /* =========================================
       3. FETCH PROMOTIONS FROM DATABASE
       ========================================= */
    function loadPromotionsFromDatabase() {
        fetch("../php/promo_api.php?action=admin_get_promos")
            .then(function (response) {
                return response.json();
            })
            .then(function (res) {
                if (res.status === "success" && Array.isArray(res.data)) {
                    promotions = res.data;
                    updatePromotionStats();
                    displayPromotions();
                } else {
                    console.error("Failed to load promotions:", res.message);
                }
            })
            .catch(function (error) {
                console.error("Error loading promotions from database:", error);
            });
    }


    /* =========================================
       4. UPDATE PROMOTION STATISTICS
       ========================================= */
    function updatePromotionStats() {
        const totalPromotions = document.getElementById("totalPromotions");
        const activePromotions = document.getElementById("activePromotions");
        const inactivePromotions = document.getElementById("inactivePromotions");

        if (totalPromotions) totalPromotions.textContent = promotions.length;

        if (activePromotions) {
            activePromotions.textContent = promotions.filter(function (promo) {
                return promo.status === "Active";
            }).length;
        }

        if (inactivePromotions) {
            inactivePromotions.textContent = promotions.filter(function (promo) {
                return promo.status === "Inactive";
            }).length;
        }
    }


    /* =========================================
       5. FORMAT OFFER TEXT DISPLAY
       ========================================= */
    function getOfferText(promotion) {
        const type = promotion.discount_type || promotion.type;
        const value = promotion.discount_value || promotion.value || 0;

        if (type === "Percentage") {
            return value + "% OFF";
        }
        return "Rs. " + value + " OFF";
    }


    /* =========================================
       6. DISPLAY & FILTER PROMOTIONS
       ========================================= */
    function displayPromotions() {
        if (!promotionGrid) return;

        const searchValue = promotionSearch ? promotionSearch.value.toLowerCase().trim() : "";
        const status = promotionStatusFilter ? promotionStatusFilter.value : "all";

        // Filter promotions by search keyword (title or code) and status filter
        const filteredPromotions = promotions.filter(function (promo) {
            const title = (promo.title || "").toLowerCase();
            const code = (promo.code || "").toLowerCase();
            const matchesSearch = title.includes(searchValue) || code.includes(searchValue);

            const matchesStatus = status === "all" || promo.status === status;

            return matchesSearch && matchesStatus;
        });

        if (filteredPromotions.length === 0) {
            promotionGrid.innerHTML = `<div class="empty-promotions">No promotions found.</div>`;
            return;
        }

        let html = "";

        filteredPromotions.forEach(function (promo) {
            const statusClass = promo.status === "Active" ? "active" : "inactive";
            const statusButtonText = promo.status === "Active" ? "Disable" : "Enable";
            const expiryDate = promo.expiry_date || promo.expiry || "2026-12-31";

            html += `
                <article class="promotion-card">
                    <div class="promotion-card-top">
                        <span class="promotion-code">${promo.code}</span>
                        <span class="promotion-status ${statusClass}">${promo.status}</span>
                    </div>

                    <h3>${promo.title}</h3>
                    <p class="promotion-description">${promo.description || ""}</p>

                    <div class="promotion-offer">
                        <strong>${getOfferText(promo)}</strong>
                        <span>Promotional discount</span>
                    </div>

                    <p class="promotion-expiry">Expires: ${expiryDate}</p>

                    <div class="promotion-actions">
                        <button type="button" class="promotion-action-btn edit-btn" onclick="editPromotion(${promo.id})">
                            Edit
                        </button>

                        <button type="button" class="promotion-action-btn status-btn" onclick="togglePromotionStatus(${promo.id})">
                            ${statusButtonText}
                        </button>

                        <button type="button" class="promotion-action-btn delete-btn" onclick="deletePromotion(${promo.id})">
                            Delete
                        </button>
                    </div>
                </article>
            `;
        });

        promotionGrid.innerHTML = html;
    }


    /* =========================================
       7. MODAL DIALOG CONTROLS
       ========================================= */
    function openPromotionModal(title) {
        if (modalTitle) modalTitle.textContent = title || "Add Promotion";
        if (promotionModal) promotionModal.classList.add("show");
    }

    function closePromotionModal() {
        if (promotionModal) promotionModal.classList.remove("show");
    }

    if (addPromotionBtn) {
        addPromotionBtn.addEventListener("click", function () {
            if (promotionForm) promotionForm.reset();
            const promoIdInput = document.getElementById("promotionId");
            if (promoIdInput) promoIdInput.value = "";
            openPromotionModal("Add Promotion");
        });
    }

    if (closeModalBtn) closeModalBtn.addEventListener("click", closePromotionModal);
    if (cancelPromotionBtn) cancelPromotionBtn.addEventListener("click", closePromotionModal);


    /* =========================================
       8. ADD / EDIT PROMOTION SUBMISSION
       ========================================= */
    if (promotionForm) {
        promotionForm.addEventListener("submit", function (event) {
            event.preventDefault();

            const id = document.getElementById("promotionId") ? document.getElementById("promotionId").value : "";
            const title = document.getElementById("promotionTitle") ? document.getElementById("promotionTitle").value.trim() : "";
            const code = document.getElementById("promotionCode") ? document.getElementById("promotionCode").value.trim().toUpperCase() : "";
            const discountType = document.getElementById("discountType") ? document.getElementById("discountType").value : "Percentage";
            const discountValue = document.getElementById("discountValue") ? document.getElementById("discountValue").value : 0;
            const expiryDate = document.getElementById("promotionExpiry") ? document.getElementById("promotionExpiry").value : "";
            const status = document.getElementById("promotionStatus") ? document.getElementById("promotionStatus").value : "Active";
            const description = document.getElementById("promotionDescription") ? document.getElementById("promotionDescription").value.trim() : "";

            if (!title || !code || !discountValue) {
                alert("Please provide a title, promo code, and discount value.");
                return;
            }

            // Construct FormData payload for backend API
            const formData = new FormData();
            formData.append("action", "admin_save_promo");
            if (id) formData.append("id", id);
            formData.append("title", title);
            formData.append("code", code);
            formData.append("discountType", discountType);
            formData.append("discountValue", discountValue);
            formData.append("expiryDate", expiryDate || "2026-12-31");
            formData.append("status", status);
            formData.append("description", description);

            // Send save request to PHP backend
            fetch("../php/promo_api.php", {
                method: "POST",
                body: formData
            })
            .then(function (response) {
                return response.json();
            })
            .then(function (data) {
                if (data.status === "success") {
                    alert(data.message || "Promotion saved successfully.");
                    closePromotionModal();
                    loadPromotionsFromDatabase(); // Refresh promotions list from database
                } else {
                    alert(data.message || "Failed to save promotion.");
                }
            })
            .catch(function (error) {
                console.error("Error saving promotion:", error);
                alert("An error occurred while connecting to the server.");
            });
        });
    }


    /* =========================================
       9. GLOBAL ACTION HANDLERS (EDIT / STATUS / DELETE)
       ========================================= */

    // Edit Promotion - Populate modal inputs with selected promotion data
    window.editPromotion = function (id) {
        const promo = promotions.find(function (item) {
            return item.id == id;
        });

        if (!promo) return;

        if (document.getElementById("promotionId")) document.getElementById("promotionId").value = promo.id;
        if (document.getElementById("promotionTitle")) document.getElementById("promotionTitle").value = promo.title;
        if (document.getElementById("promotionCode")) document.getElementById("promotionCode").value = promo.code;
        if (document.getElementById("discountType")) document.getElementById("discountType").value = promo.discount_type || promo.type;
        if (document.getElementById("discountValue")) document.getElementById("discountValue").value = promo.discount_value || promo.value;
        if (document.getElementById("promotionExpiry")) document.getElementById("promotionExpiry").value = promo.expiry_date || promo.expiry;
        if (document.getElementById("promotionStatus")) document.getElementById("promotionStatus").value = promo.status;
        if (document.getElementById("promotionDescription")) document.getElementById("promotionDescription").value = promo.description;

        openPromotionModal("Edit Promotion");
    };

    // Toggle promotion status between Active and Inactive
    window.togglePromotionStatus = function (id) {
        const promo = promotions.find(function (item) {
            return item.id == id;
        });

        if (!promo) return;

        const newStatus = promo.status === "Active" ? "Inactive" : "Active";

        const formData = new FormData();
        formData.append("action", "admin_save_promo");
        formData.append("id", promo.id);
        formData.append("title", promo.title);
        formData.append("code", promo.code);
        formData.append("discountType", promo.discount_type || promo.type);
        formData.append("discountValue", promo.discount_value || promo.value);
        formData.append("expiryDate", promo.expiry_date || promo.expiry || "2026-12-31");
        formData.append("status", newStatus);
        formData.append("description", promo.description || "");

        fetch("../php/promo_api.php", {
            method: "POST",
            body: formData
        })
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            if (data.status === "success") {
                loadPromotionsFromDatabase();
            } else {
                alert(data.message || "Failed to update status.");
            }
        })
        .catch(function (error) {
            console.error("Error toggling promotion status:", error);
        });
    };

    // Delete promotion record from database (Soft delete by setting status Inactive)
    window.deletePromotion = function (id) {
        const confirmation = confirm("Are you sure you want to delete this promotion?");
        if (!confirmation) return;

        const promo = promotions.find(function (item) {
            return item.id == id;
        });

        if (!promo) return;

        const formData = new FormData();
        formData.append("action", "admin_save_promo");
        formData.append("id", promo.id);
        formData.append("title", promo.title);
        formData.append("code", promo.code);
        formData.append("discountType", promo.discount_type || promo.type);
        formData.append("discountValue", promo.discount_value || promo.value);
        formData.append("expiryDate", promo.expiry_date || promo.expiry || "2026-12-31");
        formData.append("status", "Inactive"); // Soft delete
        formData.append("description", promo.description || "");

        fetch("../php/promo_api.php", {
            method: "POST",
            body: formData
        })
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            if (data.status === "success") {
                alert("Promotion removed successfully.");
                loadPromotionsFromDatabase();
            }
        });
    };


    /* =========================================
       10. SEARCH & FILTER EVENT LISTENERS
       ========================================= */
    if (promotionSearch) promotionSearch.addEventListener("input", displayPromotions);
    if (promotionStatusFilter) promotionStatusFilter.addEventListener("change", displayPromotions);


    /* =========================================
       11. LOGOUT HANDLER
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
       12. INITIALIZATION
       ========================================= */
    checkAdminSession();
    loadPromotionsFromDatabase();
});