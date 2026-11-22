/* =========================================
   YUMMYPREP - USER MANAGEMENT (ADMIN)
   ========================================= */

// Global array to store user accounts loaded from MySQL database
let users = [];

// Execute code when DOM content is fully loaded
document.addEventListener("DOMContentLoaded", function () {

    /* =========================================
       1. DOM ELEMENTS
       ========================================= */
    const userTableBody = document.getElementById("userTableBody");
    const userSearch = document.getElementById("userSearch");
    const statusFilter = document.getElementById("statusFilter");
    const totalUsers = document.getElementById("totalUsers");
    const activeUsers = document.getElementById("activeUsers");
    const disabledUsers = document.getElementById("disabledUsers");
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
       3. FETCH USERS FROM DATABASE
       ========================================= */
    function loadUsersFromDatabase() {
        fetch("../php/users_api.php?action=admin_get_users")
            .then(function (response) {
                return response.json();
            })
            .then(function (res) {
                if (res.status === "success" && Array.isArray(res.data)) {
                    users = res.data;
                    updateUserStats();
                    displayUsers();
                } else {
                    console.error("Failed to load users:", res.message);
                }
            })
            .catch(function (error) {
                console.error("Error loading users from database:", error);
            });
    }


    /* =========================================
       4. UPDATE USER STATISTICS COUNTERS
       ========================================= */
    function updateUserStats() {
        if (totalUsers) totalUsers.textContent = users.length;

        if (activeUsers) {
            activeUsers.textContent = users.filter(function (user) {
                const status = user.status || (user.is_active == 1 ? "Active" : "Disabled");
                return status === "Active";
            }).length;
        }

        if (disabledUsers) {
            disabledUsers.textContent = users.filter(function (user) {
                const status = user.status || (user.is_active == 1 ? "Active" : "Disabled");
                return status === "Disabled";
            }).length;
        }
    }


    /* =========================================
       5. HELPER: GET USER INITIAL
       ========================================= */
    function getInitial(name) {
        if (!name) return "U";
        return name.charAt(0).toUpperCase();
    }


    /* =========================================
       6. DISPLAY & FILTER USERS TABLE
       ========================================= */
    function displayUsers() {
        if (!userTableBody) return;

        const searchValue = userSearch ? userSearch.value.toLowerCase().trim() : "";
        const selectedStatus = statusFilter ? statusFilter.value : "all";

        // Filter users by search keyword (name or email) and status
        const filteredUsers = users.filter(function (user) {
            const userName = (user.name || user.full_name || "").toLowerCase();
            const userEmail = (user.email || "").toLowerCase();
            const matchesSearch = userName.includes(searchValue) || userEmail.includes(searchValue);

            const userStatus = user.status || (user.is_active == 1 ? "Active" : "Disabled");
            const matchesStatus = selectedStatus === "all" || userStatus === selectedStatus;

            return matchesSearch && matchesStatus;
        });

        if (filteredUsers.length === 0) {
            userTableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="empty-users">No users found.</td>
                </tr>
            `;
            return;
        }

        let usersHTML = "";

        filteredUsers.forEach(function (user) {
            const userId = user.id || user.user_id || "000";
            const userName = user.name || user.full_name || "Valued Customer";
            const userEmail = user.email || "No email";
            const userPhone = user.phone || "No phone";
            const joinedDate = user.joined || user.created_at ? new Date(user.joined || user.created_at).toLocaleDateString() : "Recent";
            const orderCount = user.orders !== undefined ? user.orders : 0;
            const userStatus = user.status || (user.is_active == 1 ? "Active" : "Disabled");

            const statusClass = userStatus === "Active" ? "active" : "disabled";
            const buttonText = userStatus === "Active" ? "Disable" : "Enable";

            usersHTML += `
                <tr>
                    <td>
                        <div class="customer-cell">
                            <div class="customer-avatar">
                                ${getInitial(userName)}
                            </div>
                            <div>
                                <strong>${userName}</strong>
                                <span>#USR${userId}</span>
                            </div>
                        </div>
                    </td>

                    <td>
                        <strong>${userEmail}</strong>
                        <br>${userPhone}
                    </td>

                    <td>${joinedDate}</td>

                    <td>${orderCount}</td>

                    <td>
                        <span class="user-status ${statusClass}">
                            ${userStatus}
                        </span>
                    </td>

                    <td>
                        <div class="user-actions">
                            <button type="button" class="action-btn status-btn" onclick="toggleUserStatus(${userId})">
                                ${buttonText}
                            </button>

                            <button type="button" class="action-btn delete-btn" onclick="deleteUser(${userId})">
                                Delete
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        });

        userTableBody.innerHTML = usersHTML;
    }


    /* =========================================
       7. TOGGLE USER STATUS (ENABLE / DISABLE)
       ========================================= */
    window.toggleUserStatus = function (userId) {
        const user = users.find(function (item) {
            return (item.id == userId || item.user_id == userId);
        });

        if (!user) return;

        const currentStatus = user.status || (user.is_active == 1 ? "Active" : "Disabled");
        const newStatus = currentStatus === "Active" ? "Disabled" : "Active";
        const newIsActive = newStatus === "Active" ? 1 : 0;

        // Construct FormData payload for backend API
        const formData = new FormData();
        formData.append("action", "admin_update_user_status");
        formData.append("userId", user.id || user.user_id || userId);
        formData.append("isActive", newIsActive);

        // Send update request to PHP backend
        fetch("../php/users_api.php", {
            method: "POST",
            body: formData
        })
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            if (data.status === "success") {
                user.status = newStatus;
                user.is_active = newIsActive;
                updateUserStats();
                displayUsers();
            } else {
                alert(data.message || "Failed to update user status.");
            }
        })
        .catch(function (error) {
            console.error("Error updating user status:", error);
            alert("An error occurred while connecting to the server.");
        });
    };


    /* =========================================
       8. DELETE USER ACCOUNT
       ========================================= */
    window.deleteUser = function (userId) {
        const confirmation = confirm("Are you sure you want to delete this user?");
        if (!confirmation) return;

        const formData = new FormData();
        formData.append("action", "admin_delete_user");
        formData.append("userId", userId);

        fetch("../php/users_api.php", {
            method: "POST",
            body: formData
        })
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            if (data.status === "success") {
                alert(data.message || "User deleted successfully.");
                loadUsersFromDatabase();
            } else {
                alert(data.message || "Failed to delete user.");
            }
        })
        .catch(function (error) {
            console.error("Error deleting user:", error);
            alert("An error occurred while deleting the user.");
        });
    };


    /* =========================================
       9. SEARCH & FILTER EVENT LISTENERS
       ========================================= */
    if (userSearch) userSearch.addEventListener("input", displayUsers);
    if (statusFilter) statusFilter.addEventListener("change", displayUsers);


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
    loadUsersFromDatabase();
});