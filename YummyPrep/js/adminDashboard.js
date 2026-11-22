/* =========================================
   YUMMYPREP - ADMIN DASHBOARD SCRIPT
   ========================================= */

document.addEventListener("DOMContentLoaded", function () {
    checkAdminSession();
    loadDashboardStats();
    loadRecentOrders();
    setupLogout();
});

/* 1. Verify Admin Session */
function checkAdminSession() {
    fetch("../php/auth_api.php?action=check_session")
        .then(res => res.json())
        .then(data => {
            if (data.status !== "success" || data.data.type !== "admin") {
                window.location.href = "login.html";
            }
        })
        .catch(err => {
            console.error("Session check error:", err);
            window.location.href = "login.html";
        });
}

/* 2. Load Dashboard Statistics */
function loadDashboardStats() {
    fetch("../php/admin_stats_api.php")
        .then(res => res.json())
        .then(res => {
            if (res.status === "success") {
                const data = res.data;

                // Update Overview Cards
                const totalUsersEl = document.getElementById("totalUsers");
                const totalMealsEl = document.getElementById("totalMeals");
                const totalOrdersEl = document.getElementById("totalOrders");
                const totalRevenueEl = document.getElementById("totalRevenue");

                if (totalUsersEl) totalUsersEl.textContent = data.totalUsers || 0;
                if (totalMealsEl) totalMealsEl.textContent = data.totalMeals || 0;
                if (totalOrdersEl) totalOrdersEl.textContent = data.totalOrders || 0;
                if (totalRevenueEl) totalRevenueEl.textContent = data.totalRevenue || "Rs. 0.00";

                // Update Status Summary Counts
                const statusMap = {
                    "Preparing": "preparingCount",
                    "Packed": "packedCount",
                    "Out for Delivery": "deliveryCount",
                    "Delivered": "deliveredCount"
                };

                for (const [statusKey, elementId] of Object.entries(statusMap)) {
                    const el = document.getElementById(elementId);
                    if (el) {
                        el.textContent = data.statusSummary[statusKey] || 0;
                    }
                }
            }
        })
        .catch(err => {
            console.error("Error loading dashboard stats:", err);
        });
}

/* 3. Load Recent Orders & Handle Status Update */
function loadRecentOrders() {
    const recentOrdersContainer = document.getElementById("recentOrders");
    if (!recentOrdersContainer) return;

    fetch("../php/orders_api.php?action=admin_get_orders")
        .then(res => res.json())
        .then(res => {
            if (res.status === "success" && res.data.length > 0) {
                let html = "";
                const latestOrders = res.data.slice(0, 5); // Show top 5

                latestOrders.forEach(order => {
                    const createdDate = new Date(order.created_at).toLocaleDateString();

                    html += `
                        <div style="display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #eee;">
                            <div>
                                <strong style="font-size: 15px;">#${order.order_number}</strong><br>
                                <small style="color: #666;">${order.customer_name || 'Customer'} • ${createdDate}</small>
                            </div>
                            <strong style="color: #4CAF50;">Rs. ${parseFloat(order.total).toFixed(2)}</strong>
                            <div>
                                <select onchange="updateOrderStatus(${order.id}, this.value)" style="padding: 6px 10px; border-radius: 6px; border: 1px solid #ccc; font-weight: 500;">
                                    <option value="Preparing" ${order.status === 'Preparing' ? 'selected' : ''}>Preparing</option>
                                    <option value="Packed" ${order.status === 'Packed' ? 'selected' : ''}>Packed</option>
                                    <option value="Out for Delivery" ${order.status === 'Out for Delivery' ? 'selected' : ''}>Out for Delivery</option>
                                    <option value="Delivered" ${order.status === 'Delivered' ? 'selected' : ''}>Delivered</option>
                                </select>
                            </div>
                        </div>
                    `;
                });

                recentOrdersContainer.innerHTML = html;
            } else {
                recentOrdersContainer.innerHTML = `
                    <div style="text-align: center; padding: 30px; color: #777;">
                        <h3>No orders yet</h3>
                        <p>Customer orders will appear here once they are placed.</p>
                    </div>
                `;
            }
        })
        .catch(err => {
            console.error("Error loading recent orders:", err);
        });
}

/* Update Order Status */
function updateOrderStatus(orderId, newStatus) {
    const formData = new FormData();
    formData.append("action", "admin_update_status");
    formData.append("orderId", orderId);
    formData.append("status", newStatus);

    fetch("../php/orders_api.php", {
        method: "POST",
        body: formData
    })
    .then(res => res.json())
    .then(data => {
        if (data.status === "success") {
            alert(`Order status updated to ${newStatus}`);
            loadDashboardStats(); // Refresh dashboard numbers
        } else {
            alert(data.message || "Failed to update status.");
        }
    })
    .catch(err => {
        console.error("Status update error:", err);
        alert("Error updating status.");
    });
}

/* 4. Logout Setup */
function setupLogout() {
    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", function () {
            fetch("../php/auth_api.php?action=logout")
                .then(() => {
                    sessionStorage.clear();
                    window.location.href = "login.html";
                })
                .catch(() => {
                    window.location.href = "login.html";
                });
        });
    }
}