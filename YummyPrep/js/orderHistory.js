/* =========================================
   YUMMYPREP - CUSTOMER ORDER HISTORY
   ========================================= */

// Target container element from orderHistory.html
const historyContainer = document.getElementById("orderHistoryContainer");

// Execute code when DOM content is fully loaded
document.addEventListener("DOMContentLoaded", function () {
    // Fetch user orders from database API
    fetchCustomerOrders();
});


/* =========================================
   1. FETCH ORDERS FROM DATABASE
   ========================================= */
function fetchCustomerOrders() {
    fetch("../php/orders_api.php?action=get_user_orders")
        .then(function (response) {
            return response.json();
        })
        .then(function (res) {
            if (res.status === "success" && Array.isArray(res.data)) {
                displayOrderHistory(res.data);
            } else {
                // Fallback to LocalStorage orders if API lookup fails
                const localOrders = JSON.parse(localStorage.getItem("yummyPrepOrders")) || [];
                displayOrderHistory(localOrders);
            }
        })
        .catch(function (error) {
            console.error("Error fetching customer orders:", error);
            // Fallback to LocalStorage orders on connection error
            const localOrders = JSON.parse(localStorage.getItem("yummyPrepOrders")) || [];
            displayOrderHistory(localOrders);
        });
}


/* =========================================
   2. DISPLAY ORDER CARDS
   ========================================= */
function displayOrderHistory(orders) {
    if (!historyContainer) return;

    historyContainer.innerHTML = "";

    /* EMPTY ORDER HISTORY */
    if (!orders || orders.length === 0) {
        historyContainer.innerHTML = `
            <div class="empty-history">
                <h2>No orders yet</h2>
                <p>You haven't placed any YummyPrep orders yet.</p>
                <a href="meals.html">Explore Meals →</a>
            </div>
        `;
        return;
    }

    /* DISPLAY EACH ORDER */
    orders.forEach(function (order) {
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

            itemsHTML += `
                <div class="order-item">
                    <img src="${itemImage}" alt="${itemName}" class="order-item-image">

                    <div class="item-info">
                        <h4>${itemName}</h4>
                        <p>Quantity: ${quantity}</p>
                    </div>

                    <strong>Rs. ${itemTotal.toFixed(2)}</strong>
                </div>
            `;
        });

        /* DELIVERY DAY */
        let deliveryText = "Today";
        const dayVal = order.delivery_day || order.deliveryDay || "today";
        if (dayVal.toLowerCase() === "tomorrow") {
            deliveryText = "Tomorrow";
        }

        /* FORMAT ORDER NUMBER & DATE */
        const orderNumber = order.order_number || order.id || "YP000000";
        const orderDate = order.created_at
            ? new Date(order.created_at).toLocaleDateString()
            : (order.date || new Date().toLocaleDateString());

        const orderStatus = order.status || "Preparing";
        const statusClass = orderStatus.toLowerCase().replace(/ /g, "-");
        const totalAmount = parseFloat(order.total || 0).toFixed(2);

        /* ORDER CARD HTML */
        historyContainer.innerHTML += `
            <div class="order-card">
                <div class="order-top">
                    <div>
                        <p class="order-label">ORDER ID</p>
                        <h3>#${orderNumber}</h3>
                    </div>

                    <div class="order-status ${statusClass}">
                        ${orderStatus}
                    </div>
                </div>

                <div class="order-date">
                    Ordered on <strong>${orderDate}</strong>
                </div>

                <div class="delivery-history">
                    Delivery: <strong>${deliveryText}</strong>
                </div>

                <div class="order-items">
                    ${itemsHTML}
                </div>

                <div class="order-bottom">
                    <div>
                        <span>Total</span>
                        <strong>Rs. ${totalAmount}</strong>
                    </div>

                    <div class="order-actions">
                        <a href="orderTracking.html?orderNumber=${encodeURIComponent(orderNumber)}" class="details-btn">
                            Track Order
                        </a>

                        <a href="meals.html" class="reorder-btn">
                            Order Again →
                        </a>
                    </div>
                </div>
            </div>
        `;
    });
}