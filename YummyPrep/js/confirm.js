/* =========================================
   YUMMYPREP - ORDER CONFIRMATION
   ========================================= */

// Execute code when the DOM is fully loaded
document.addEventListener("DOMContentLoaded", function () {

    // 1. Check for orderNumber query parameter in the URL
    const urlParams = new URLSearchParams(window.location.search);
    const orderNumber = urlParams.get("orderNumber");

    // 2. Read latest order data stored in LocalStorage as fallback
    const latestOrder = JSON.parse(localStorage.getItem("latestYummyPrepOrder"));

    if (orderNumber) {
        // Show the order saved at checkout straight away (no placeholder values)
        if (latestOrder && String(latestOrder.orderNumber) === String(orderNumber)) {
            displayOrderConfirmation(latestOrder);
        }

        // Then refresh with the confirmed details from the database
        fetchOrderDetails(orderNumber);
    } else if (latestOrder) {
        // Render details from LocalStorage fallback
        displayOrderConfirmation(latestOrder);
    }
});


/* =========================================
   1. FETCH ORDER DETAILS FROM DATABASE
   ========================================= */
function fetchOrderDetails(orderNumber) {
    fetch(`../php/orders_api.php?action=track_order&orderNumber=${encodeURIComponent(orderNumber)}`)
        .then(function (response) {
            return response.json();
        })
        .then(function (res) {
            if (res.status === "success" && res.data) {
                const orderData = {
                    id: res.data.order_number,
                    orderNumber: res.data.order_number,
                    total: parseFloat(res.data.total).toFixed(2),
                    deliveryDay: res.data.delivery_day
                };
                displayOrderConfirmation(orderData);
            } else {
                // Fallback to LocalStorage if API lookup fails
                const latestOrder = JSON.parse(localStorage.getItem("latestYummyPrepOrder"));
                if (latestOrder) displayOrderConfirmation(latestOrder);
            }
        })
        .catch(function (error) {
            console.error("Error fetching order confirmation details:", error);
            const latestOrder = JSON.parse(localStorage.getItem("latestYummyPrepOrder"));
            if (latestOrder) displayOrderConfirmation(latestOrder);
        });
}


/* =========================================
   2. DISPLAY CONFIRMATION ON HTML PAGE
   ========================================= */
function displayOrderConfirmation(order) {
    // Target HTML elements
    const confirmOrderId = document.getElementById("confirmOrderId");
    const confirmTotal = document.getElementById("confirmTotal");
    const confirmDeliveryDay = document.getElementById("confirmDeliveryDay");

    // Display Order Number
    if (confirmOrderId) {
        const displayId = String(order.orderNumber || order.id || "N/A");
        confirmOrderId.textContent = displayId.startsWith("#") ? displayId : "#" + displayId;
    }

    // Display Total Price
    if (confirmTotal) {
        const displayTotal = Number(order.total || 0).toFixed(2);
        confirmTotal.textContent = "Rs. " + displayTotal;
    }

    // Display Delivery Schedule Day
    if (confirmDeliveryDay) {
        if (order.deliveryDay && order.deliveryDay.toLowerCase() === "tomorrow") {
            confirmDeliveryDay.textContent = "Tomorrow";
        } else {
            confirmDeliveryDay.textContent = "Today";
        }
    }
}