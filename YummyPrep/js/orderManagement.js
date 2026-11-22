/* =========================================
   YUMMYPREP - ADMIN ORDER MANAGEMENT
   ========================================= */

// Global array to store orders loaded from MySQL database
let orders = [];

// Execute code when DOM content is fully loaded
document.addEventListener("DOMContentLoaded", function () {

    /* =========================================
       1. DOM ELEMENTS
       ========================================= */
    const ordersContainer = document.getElementById("ordersContainer");
    const orderSearch = document.getElementById("orderSearch");
    const statusFilter = document.getElementById("statusFilter");
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
       3. FETCH ORDERS FROM DATABASE
       ========================================= */
    function loadOrdersFromDatabase() {
        fetch("../php/orders_api.php?action=admin_get_orders")
            .then(function (response) {
                return response.json();
            })
            .then(function (res) {
                if (res.status === "success" && Array.isArray(res.data)) {
                    orders = res.data;
                    updateOrderStats();
                    displayOrders();
                } else {
                    console.error("Failed to load orders:", res.message);
                }
            })
            .catch(function (error) {
                console.error("Error loading orders from database:", error);
            });
    }


    /* =========================================
       4. GET STATUS CSS CLASS
       ========================================= */
    function getStatusClass(status) {
        if (status === "Preparing") return "preparing";
        if (status === "Packed") return "packed";
        if (status === "Out for Delivery") return "out-for-delivery";
        if (status === "Delivered") return "delivered";
        return "preparing";
    }


    /* =========================================
       5. UPDATE ORDER STATS / COUNTERS
       ========================================= */
    function updateOrderStats() {
        const totalOrders = document.getElementById("totalOrders");
        const preparingOrders = document.getElementById("preparingOrders");
        const deliveryOrders = document.getElementById("deliveryOrders");
        const deliveredOrders = document.getElementById("deliveredOrders");

        if (totalOrders) totalOrders.textContent = orders.length;

        if (preparingOrders) {
            preparingOrders.textContent = orders.filter(function (order) {
                return order.status === "Preparing";
            }).length;
        }

        if (deliveryOrders) {
            deliveryOrders.textContent = orders.filter(function (order) {
                return order.status === "Out for Delivery";
            }).length;
        }

        if (deliveredOrders) {
            deliveredOrders.textContent = orders.filter(function (order) {
                return order.status === "Delivered";
            }).length;
        }
    }


    /* =========================================
       6. DISPLAY & FILTER ORDERS
       ========================================= */
    function displayOrders() {
        if (!ordersContainer) return;

        const searchValue = orderSearch ? orderSearch.value.toLowerCase().trim() : "";
        const selectedStatus = statusFilter ? statusFilter.value : "all";

        // Filter orders by search keyword (order number or customer name) and status
        const filteredOrders = orders.filter(function (order) {
            const orderNum = String(order.order_number || order.id || "").toLowerCase();
            const customerName = (order.first_name || order.customer_name || "").toLowerCase();
            const matchesSearch = orderNum.includes(searchValue) || customerName.includes(searchValue);

            const matchesStatus = selectedStatus === "all" || order.status === selectedStatus;

            return matchesSearch && matchesStatus;
        });

        if (filteredOrders.length === 0) {
            ordersContainer.innerHTML = `<div class="empty-orders">No orders found.</div>`;
            return;
        }

        let html = "";

        filteredOrders.forEach(function (order) {
            let itemsHTML = "";
            const orderItems = order.items || [];

            orderItems.forEach(function (item) {
                const rawPrice = item.price || 0;
                const price = typeof rawPrice === "number"
                    ? rawPrice
                    : Number(String(rawPrice).replace("Rs.", "").replace(/,/g, "").trim()) || 0;

                const quantity = item.quantity || 1;
                const itemTotal = price * quantity;
                const itemName = item.item_name || item.name || "YummyPrep Meal";
                const itemImage = item.image || "../images/chickenBowl1.jpeg";
                const itemCategory = item.category || "Standard";

                // If it's a customized meal, format the name/ingredients cleanly for admin view
                let customizationDetailsHTML = "";
                if (itemCategory === "Customized") {
                    customizationDetailsHTML = `<div style="font-size: 12px; color: #555; margin-top: 3px; line-height: 1.4; background: #f9f9f9; padding: 6px; border-radius: 4px; border-left: 3px solid #4CAF50;">
                        <strong>Customizations / Ingredients:</strong><br>${itemName}
                    </div>`;
                }

                itemsHTML += `
                    <div class="admin-order-item" style="display: flex; flex-direction: column; align-items: flex-start; border-bottom: 1px solid #eee; padding-bottom: 10px; margin-bottom: 10px;">
                        <div style="display: flex; align-items: center; width: 100%; justify-content: space-between;">
                            <div style="display: flex; align-items: center; gap: 10px;">
                                <img src="${itemImage}" alt="${itemName}" style="width: 45px; height: 45px; object-fit: cover; border-radius: 6px;">
                                <div>
                                    <strong style="font-size: 14px;">${itemCategory === "Customized" ? "Customized Meal" : itemName}</strong>
                                    <span style="display: block; font-size: 12px; color: #777;">Quantity: ${quantity} • Category: ${itemCategory}</span>
                                </div>
                            </div>
                            <strong style="color: #2e7d32;">Rs. ${itemTotal.toFixed(2)}</strong>
                        </div>
                        ${customizationDetailsHTML}
                    </div>
                `;
            });

            const orderId = order.order_number || order.id || "0000";
            const orderDate = order.created_at
                ? new Date(order.created_at).toLocaleDateString()
                : (order.date || "Recent");
            const customerFullName = `${order.first_name || ""} ${order.last_name || ""}`.trim() || order.customer_name || "Valued Customer";
            const customerPhone = order.phone || "";
            const customerAddress = order.address || "";
            const orderTotal = parseFloat(order.total || 0).toFixed(2);
            const currentStatus = order.status || "Preparing";

            html += `
                <article class="admin-order-card" style="background: #fff; padding: 20px; border-radius: 10px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);">
                    <div class="order-top" style="display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 1px solid #eee; padding-bottom: 12px; margin-bottom: 12px;">
                        <div>
                            <p class="order-id-label" style="font-size: 11px; color: #888; font-weight: 600; margin-bottom: 2px;">ORDER ID & CUSTOMER</p>
                            <h3 style="margin: 0; font-size: 18px; color: #333;">#${orderId}</h3>
                            <span class="order-date" style="font-size: 13px; color: #555;"><strong>${customerFullName}</strong> (${customerPhone}) • ${orderDate}</span><br>
                            <small style="color: #666; font-size: 12px;">📍 ${customerAddress}</small>
                        </div>

                        <span class="order-status ${getStatusClass(currentStatus)}" style="padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600;">
                            ${currentStatus}
                        </span>
                    </div>

                    <div class="admin-order-items" style="margin-bottom: 15px;">
                        <p style="font-size: 12px; font-weight: 600; color: #444; margin-bottom: 8px;">ORDER ITEMS & INGREDIENTS:</p>
                        ${itemsHTML}
                    </div>

                    <div class="order-bottom" style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #eee; pt: 12px; padding-top: 12px;">
                        <div class="order-total">
                            <span style="font-size: 11px; color: #888; display: block;">TOTAL AMOUNT</span>
                            <strong style="font-size: 16px; color: #2e7d32;">Rs. ${orderTotal}</strong>
                        </div>

                        <div class="status-control" style="display: flex; align-items: center; gap: 8px;">
                            <label style="font-size: 12px; font-weight: 500;">Update Status:</label>
                            <select onchange="updateOrderStatus('${order.id || orderId}', this.value)" style="padding: 6px 10px; border-radius: 6px; border: 1px solid #ccc; font-size: 13px;">
                                <option value="Preparing" ${currentStatus === "Preparing" ? "selected" : ""}>Preparing</option>
                                <option value="Packed" ${currentStatus === "Packed" ? "selected" : ""}>Packed</option>
                                <option value="Out for Delivery" ${currentStatus === "Out for Delivery" ? "selected" : ""}>Out for Delivery</option>
                                <option value="Delivered" ${currentStatus === "Delivered" ? "selected" : ""}>Delivered</option>
                            </select>
                        </div>
                    </div>
                </article>
            `;
        });

        ordersContainer.innerHTML = html;
    }


    /* =========================================
       7. UPDATE ORDER STATUS VIA PHP API
       ========================================= */
    window.updateOrderStatus = function (orderIdentifier, newStatus) {
        // Find order record in local array
        const order = orders.find(function (item) {
            return item.id == orderIdentifier || item.order_number == orderIdentifier;
        });

        if (!order) return;

        // Construct FormData payload for backend API
        const formData = new FormData();
        formData.append("action", "admin_update_status");
        formData.append("orderId", order.id || orderIdentifier);
        formData.append("status", newStatus);

        // Send update request to PHP backend
        fetch("../php/orders_api.php", {
            method: "POST",
            body: formData
        })
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            if (data.status === "success") {
                order.status = newStatus;
                updateOrderStats();
                displayOrders();
            } else {
                alert(data.message || "Failed to update order status.");
            }
        })
        .catch(function (error) {
            console.error("Error updating order status:", error);
            alert("An error occurred while connecting to the server.");
        });
    };


    /* =========================================
       8. SEARCH & FILTER EVENT LISTENERS
       ========================================= */
    if (orderSearch) orderSearch.addEventListener("input", displayOrders);
    if (statusFilter) statusFilter.addEventListener("change", displayOrders);


    /* =========================================
       9. LOGOUT HANDLER
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
       10. INITIALIZATION
       ========================================= */
    checkAdminSession();
    loadOrdersFromDatabase();
});