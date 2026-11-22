/* =========================================
   YUMMYPREP - LIVE ORDER TRACKING (FIXED)
   ========================================= */

document.addEventListener("DOMContentLoaded", function () {

    /* =========================================
       1. READ ORDER NUMBER FROM URL OR USER PROFILE
       ========================================= */
    const trackingParams = new URLSearchParams(window.location.search);
    const orderNumber = trackingParams.get("orderNumber") || trackingParams.get("order");

    if (orderNumber) {
        // Fetch specific order details from MySQL database API
        fetchTrackingDataFromDatabase(orderNumber);
    } else {
        // Fallback 1: Check LocalStorage saved orders
        const savedOrders = JSON.parse(localStorage.getItem("yummyPrepOrders")) || [];
        if (savedOrders.length > 0) {
            renderOrderTracking(savedOrders[savedOrders.length - 1]); // Show latest order
        } else {
            // Fallback 2: Fetch user's latest order from Database API automatically
            fetchLatestUserOrderFromDatabase();
        }
    }
});


/* =========================================
   2. FETCH TRACKING DATA FROM PHP API
   ========================================= */
function fetchTrackingDataFromDatabase(orderNumber) {
    fetch(`../php/orders_api.php?action=track_order&orderNumber=${encodeURIComponent(orderNumber)}`)
        .then(function (response) {
            return response.json();
        })
        .then(function (res) {
            if (res.status === "success" && res.data) {
                renderOrderTracking(res.data);
            } else {
                fallbackToLocalStorageTracking(orderNumber);
            }
        })
        .catch(function (error) {
            console.error("Error fetching order tracking:", error);
            fallbackToLocalStorageTracking(orderNumber);
        });
}

// Fetch user's latest orders if no orderNumber is in URL
function fetchLatestUserOrderFromDatabase() {
    fetch(`../php/orders_api.php?action=get_user_orders`)
        .then(function (response) {
            return response.json();
        })
        .then(function (res) {
            if (res.status === "success" && res.data && res.data.length > 0) {
                // Get the most recent order
                const latestOrder = res.data[res.data.length - 1];
                renderOrderTracking(latestOrder);
            } else {
                showNoOrderFound();
            }
        })
        .catch(function (error) {
            console.error("Error fetching user orders:", error);
            showNoOrderFound();
        });
}

// Fallback search in LocalStorage orders array
function fallbackToLocalStorageTracking(orderNumber) {
    const savedOrders = JSON.parse(localStorage.getItem("yummyPrepOrders")) || [];
    const found = savedOrders.find(function (order) {
        return order.id == orderNumber || order.order_number == orderNumber;
    });

    if (found) {
        renderOrderTracking(found);
    } else {
        showNoOrderFound();
    }
}


/* =========================================
   3. RENDER ORDER TRACKING UI
   ========================================= */
function renderOrderTracking(order) {
    const orderId = order.order_number || order.id || "000000";
    const totalAmount = parseFloat(order.total || 0).toFixed(2);
    const deliveryDayVal = order.delivery_day || order.deliveryDay || "today";
    const orderStatus = order.status || "Preparing";

    // Set Order ID text
    const trackingOrderId = document.getElementById("trackingOrderId");
    if (trackingOrderId) trackingOrderId.textContent = "#" + orderId;

    // Set Total Amount text
    const trackingTotal = document.getElementById("trackingTotal");
    if (trackingTotal) trackingTotal.textContent = "Rs. " + totalAmount;

    // Set Delivery Schedule text
    const trackingDeliveryDay = document.getElementById("trackingDeliveryDay");
    if (trackingDeliveryDay) {
        trackingDeliveryDay.textContent = deliveryDayVal.toLowerCase() === "tomorrow" ? "Tomorrow" : "Today";
    }

    // Set Order Status text
    const trackingStatus = document.getElementById("trackingStatus");
    if (trackingStatus) {
        trackingStatus.textContent = orderStatus;
        trackingStatus.className = "status-badge " + orderStatus.toLowerCase().replace(/ /g, "-");
    }


    /* =====================================
       4. RENDER ORDER ITEMS LIST
       ===================================== */
    const trackingItems = document.getElementById("trackingItems");
    if (trackingItems) {
        let itemsHTML = "";
        const itemsArray = order.items || [];

        itemsArray.forEach(function (item) {
            const rawPrice = item.price || 0;
            const price = typeof rawPrice === "number"
                ? rawPrice
                : Number(String(rawPrice).replace("Rs.", "").replace(/,/g, "").trim()) || 0;

            const quantity = item.quantity || 1;
            const itemTotal = price * quantity;
            const itemName = item.item_name || item.name || "YummyPrep Meal";
            const itemImage = item.image || "../images/chickenBowl1.jpeg";

            itemsHTML += `
                <div class="tracking-order-item">
                    <img src="${itemImage}" alt="${itemName}" class="tracking-item-image">

                    <div class="tracking-item-info">
                        <h3>${itemName}</h3>
                        <p>Quantity: ${quantity}</p>
                    </div>

                    <strong>Rs. ${itemTotal.toFixed(2)}</strong>
                </div>
            `;
        });

        trackingItems.innerHTML = itemsHTML;
    }


    /* =====================================
       5. UPDATE PROGRESS BAR STEPS
       ===================================== */
    const statusOrder = ["Confirmed", "Preparing", "Packed", "Out for Delivery", "Delivered"];
    let currentIndex = statusOrder.indexOf(orderStatus);

    if (currentIndex === -1) {
        currentIndex = 1; // Default to Preparing if status doesn't match
    }

    const steps = document.querySelectorAll(".tracking-step");
    const lines = document.querySelectorAll(".progress-line");

    steps.forEach(function (step, index) {
        const stepStatus = step.getAttribute("data-status");
        const stepIdx = statusOrder.indexOf(stepStatus);

        if (stepIdx !== -1) {
            if (stepIdx <= currentIndex) {
                step.classList.add("active");
            } else {
                step.classList.remove("active");
            }
        } else if (index <= currentIndex) {
            step.classList.add("active");
        }
    });

    lines.forEach(function (line, index) {
        if (index < currentIndex) {
            line.classList.add("active");
        } else {
            line.classList.remove("active");
        }
    });
}


/* =========================================
   6. NO ORDER FOUND STATE
   ========================================= */
function showNoOrderFound() {
    const trackingContainer = document.querySelector(".tracking-container");
    if (trackingContainer) {
        trackingContainer.innerHTML = `
            <div style="background:white; padding:60px 30px; border-radius:20px; text-align:center;">
                <h2>No order found</h2>
                <p style="color:#777; margin:10px 0 25px;">
                    You don't have an order available to track.
                </p>
                <a href="meals.html" style="background:#738c52; color:white; text-decoration:none; padding:12px 24px; border-radius:25px;">
                    Browse Meals →
                </a>
            </div>
        `;
    }
}