/* =========================================
   YUMMYPREP - FAQ MANAGEMENT
   ========================================= */

// Global state array to hold FAQs fetched from MySQL
let faqs = [];

// Execute code when the DOM is fully loaded
document.addEventListener("DOMContentLoaded", function () {

    /* =========================================
       1. DOM ELEMENTS
       ========================================= */
    const faqList = document.getElementById("faqList");
    const faqSearch = document.getElementById("faqSearch");
    const faqCategoryFilter = document.getElementById("faqCategoryFilter");
    const faqStatusFilter = document.getElementById("faqStatusFilter");
    const faqModal = document.getElementById("faqModal");
    const faqForm = document.getElementById("faqForm");
    const modalTitle = document.getElementById("modalTitle");
    const addFaqBtn = document.getElementById("addFaqBtn");
    const closeModalBtn = document.getElementById("closeModalBtn");
    const cancelFaqBtn = document.getElementById("cancelFaqBtn");
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
       3. FETCH FAQS FROM DATABASE
       ========================================= */
    function loadFaqsFromDatabase() {
        fetch("../php/faq_api.php?action=admin_get_faqs")
            .then(function (response) {
                return response.json();
            })
            .then(function (res) {
                if (res.status === "success") {
                    faqs = res.data;
                    updateFaqStats();
                    displayFaqs();
                } else {
                    console.error("Failed to load FAQs:", res.message);
                }
            })
            .catch(function (error) {
                console.error("Error loading FAQs:", error);
            });
    }


    /* =========================================
       4. UPDATE FAQ STATISTICS
       ========================================= */
    function updateFaqStats() {
        const totalFaqs = document.getElementById("totalFaqs");
        const activeFaqs = document.getElementById("activeFaqs");
        const inactiveFaqs = document.getElementById("inactiveFaqs");

        if (totalFaqs) totalFaqs.textContent = faqs.length;

        if (activeFaqs) {
            activeFaqs.textContent = faqs.filter(function (faq) {
                return faq.status === "Active";
            }).length;
        }

        if (inactiveFaqs) {
            inactiveFaqs.textContent = faqs.filter(function (faq) {
                return faq.status === "Inactive";
            }).length;
        }
    }


    /* =========================================
       5. DISPLAY & FILTER FAQS
       ========================================= */
    function displayFaqs() {
        if (!faqList) return;

        const searchValue = faqSearch ? faqSearch.value.toLowerCase().trim() : "";
        const category = faqCategoryFilter ? faqCategoryFilter.value : "all";
        const status = faqStatusFilter ? faqStatusFilter.value : "all";

        // Filter FAQs by search keyword, category, and status
        const filteredFaqs = faqs.filter(function (faq) {
            const matchesSearch =
                faq.question.toLowerCase().includes(searchValue) ||
                faq.answer.toLowerCase().includes(searchValue);

            const matchesCategory = category === "all" || faq.category === category;
            const matchesStatus = status === "all" || faq.status === status;

            return matchesSearch && matchesCategory && matchesStatus;
        });

        if (filteredFaqs.length === 0) {
            faqList.innerHTML = `<div class="empty-faqs">No FAQs found.</div>`;
            return;
        }

        let html = "";

        filteredFaqs.forEach(function (faq) {
            const statusClass = faq.status === "Active" ? "active" : "inactive";
            const statusButtonText = faq.status === "Active" ? "Disable" : "Enable";

            html += `
                <article class="faq-item">
                    <div class="faq-top">
                        <span class="faq-category">${faq.category}</span>
                        <span class="faq-status ${statusClass}">${faq.status}</span>
                    </div>

                    <h3>${faq.question}</h3>
                    <p class="faq-answer">${faq.answer}</p>

                    <div class="faq-actions">
                        <button type="button" class="faq-action-btn edit-btn" onclick="editFaq(${faq.id})">
                            Edit
                        </button>

                        <button type="button" class="faq-action-btn status-btn" onclick="toggleFaqStatus(${faq.id})">
                            ${statusButtonText}
                        </button>

                        <button type="button" class="faq-action-btn delete-btn" onclick="deleteFaq(${faq.id})">
                            Delete
                        </button>
                    </div>
                </article>
            `;
        });

        faqList.innerHTML = html;
    }


    /* =========================================
       6. MODAL DIALOG CONTROLS
       ========================================= */
    function openFaqModal(title) {
        if (modalTitle) modalTitle.textContent = title || "Add FAQ";
        if (faqModal) faqModal.classList.add("show");
    }

    function closeFaqModal() {
        if (faqModal) faqModal.classList.remove("show");
    }

    if (addFaqBtn) {
        addFaqBtn.addEventListener("click", function () {
            if (faqForm) faqForm.reset();
            const faqIdInput = document.getElementById("faqId");
            if (faqIdInput) faqIdInput.value = "";
            openFaqModal("Add FAQ");
        });
    }

    if (closeModalBtn) closeModalBtn.addEventListener("click", closeFaqModal);
    if (cancelFaqBtn) cancelFaqBtn.addEventListener("click", closeFaqModal);


    /* =========================================
       7. ADD / EDIT FAQ SUBMISSION
       ========================================= */
    if (faqForm) {
        faqForm.addEventListener("submit", function (event) {
            event.preventDefault();

            const id = document.getElementById("faqId") ? document.getElementById("faqId").value : "";
            const question = document.getElementById("faqQuestion") ? document.getElementById("faqQuestion").value.trim() : "";
            const answer = document.getElementById("faqAnswer") ? document.getElementById("faqAnswer").value.trim() : "";
            const category = document.getElementById("faqCategory") ? document.getElementById("faqCategory").value : "General";
            const status = document.getElementById("faqStatus") ? document.getElementById("faqStatus").value : "Active";

            if (!question || !answer) {
                alert("Please fill in both the question and answer.");
                return;
            }

            // Construct FormData payload for backend API
            const formData = new FormData();
            formData.append("action", "admin_save_faq");
            if (id) formData.append("id", id);
            formData.append("question", question);
            formData.append("answer", answer);
            formData.append("category", category);
            formData.append("status", status);

            // Send save request to PHP backend
            fetch("../php/faq_api.php", {
                method: "POST",
                body: formData
            })
            .then(function (response) {
                return response.json();
            })
            .then(function (data) {
                if (data.status === "success") {
                    alert(data.message || "FAQ saved successfully.");
                    closeFaqModal();
                    loadFaqsFromDatabase(); // Refresh list from database
                } else {
                    alert(data.message || "Failed to save FAQ.");
                }
            })
            .catch(function (error) {
                console.error("Error saving FAQ:", error);
                alert("An error occurred while saving the FAQ.");
            });
        });
    }


    /* =========================================
       8. GLOBAL ACTION HANDLERS (EDIT / STATUS / DELETE)
       ========================================= */

    // Edit FAQ - Fill modal fields with selected FAQ data
    window.editFaq = function (id) {
        const faq = faqs.find(function (item) {
            return item.id == id;
        });

        if (!faq) return;

        if (document.getElementById("faqId")) document.getElementById("faqId").value = faq.id;
        if (document.getElementById("faqQuestion")) document.getElementById("faqQuestion").value = faq.question;
        if (document.getElementById("faqAnswer")) document.getElementById("faqAnswer").value = faq.answer;
        if (document.getElementById("faqCategory")) document.getElementById("faqCategory").value = faq.category;
        if (document.getElementById("faqStatus")) document.getElementById("faqStatus").value = faq.status;

        openFaqModal("Edit FAQ");
    };

    // Toggle FAQ status between Active and Inactive
    window.toggleFaqStatus = function (id) {
        const faq = faqs.find(function (item) {
            return item.id == id;
        });

        if (!faq) return;

        const newStatus = faq.status === "Active" ? "Inactive" : "Active";

        const formData = new FormData();
        formData.append("action", "admin_save_faq");
        formData.append("id", faq.id);
        formData.append("question", faq.question);
        formData.append("answer", faq.answer);
        formData.append("category", faq.category);
        formData.append("status", newStatus);

        fetch("../php/faq_api.php", {
            method: "POST",
            body: formData
        })
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            if (data.status === "success") {
                loadFaqsFromDatabase();
            } else {
                alert(data.message || "Failed to change status.");
            }
        })
        .catch(function (error) {
            console.error("Error toggling status:", error);
        });
    };

    // Delete FAQ (Sets status to Inactive as soft-delete)
    window.deleteFaq = function (id) {
        const confirmation = confirm("Are you sure you want to delete this FAQ?");
        if (!confirmation) return;

        const faq = faqs.find(function (item) {
            return item.id == id;
        });

        if (!faq) return;

        const formData = new FormData();
        formData.append("action", "admin_save_faq");
        formData.append("id", faq.id);
        formData.append("question", faq.question);
        formData.append("answer", faq.answer);
        formData.append("category", faq.category);
        formData.append("status", "Inactive"); // Soft delete by marking inactive

        fetch("../php/faq_api.php", {
            method: "POST",
            body: formData
        })
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            if (data.status === "success") {
                alert("FAQ removed successfully.");
                loadFaqsFromDatabase();
            }
        });
    };


    /* =========================================
       9. SEARCH & FILTER EVENT LISTENERS
       ========================================= */
    if (faqSearch) faqSearch.addEventListener("input", displayFaqs);
    if (faqCategoryFilter) faqCategoryFilter.addEventListener("change", displayFaqs);
    if (faqStatusFilter) faqStatusFilter.addEventListener("change", displayFaqs);


    /* =========================================
       10. LOGOUT LISTENER
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
    loadFaqsFromDatabase();
});