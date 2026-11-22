/* =========================================
   YUMMYPREP - SHOPPING CART & PROMO CODE
   ========================================= */

let cart = JSON.parse(localStorage.getItem("yummyPrepCart")) || [];

const cartItemsContainer = document.querySelector(".cart-items");
const orderSummary = document.querySelector(".order-summary");

function getNumericPrice(priceText) {
    if (typeof priceText === "number") return priceText;
    return Number(
        String(priceText)
            .replace("Rs.", "")
            .replace(/,/g, "")
            .trim()
    ) || 0;
}

function saveCart() {
    localStorage.setItem("yummyPrepCart", JSON.stringify(cart));
}

function displayCart() {
    if (!cartItemsContainer) return;

    let cartHTML = `
        <div class="cart-title">
            <h2>Your Items</h2>
            <span>${cart.length} Item${cart.length === 1 ? "" : "s"}</span>
        </div>
    `;

    if (cart.length === 0) {
        cartHTML += `
            <div class="empty-cart">
                <h3>Your cart is empty.</h3>
                <p>Add something delicious from our menu.</p>
                <a href="meals.html">Explore Meals →</a>
            </div>
        `;
    }

    cart.forEach(function (item, index) {
        const numericPrice = getNumericPrice(item.price);
        const itemTotal = numericPrice * item.quantity;
        const itemImage = item.image || "../images/chickenBowl1.jpeg";

        cartHTML += `
            <div class="cart-card">
                <div class="cart-item-image">
                    <img src="${itemImage}" alt="${item.name}">
                </div>
                <div class="cart-item-info">
                    <div>
                        <span class="item-category">${item.category || "YUMMYPREP MEAL"}</span>
                        <h3>${item.name}</h3>
                        <p>Freshly prepared and added to your cart.</p>
                    </div>
                    <div class="item-bottom">
                        <strong>Rs. ${itemTotal.toFixed(2)}</strong>
                        <div class="quantity">
                            <button type="button" onclick="decreaseQuantity(${index})">−</button>
                            <span>${item.quantity}</span>
                            <button type="button" onclick="increaseQuantity(${index})">+</button>
                        </div>
                        <button type="button" class="remove-btn" onclick="removeItem(${index})">Remove</button>
                    </div>
                </div>
            </div>
        `;
    });

    cartHTML += `<a href="meals.html" class="continue-btn">← Continue Shopping</a>`;
    cartItemsContainer.innerHTML = cartHTML;
    updateSummary();
}

function updateSummary() {
    if (!orderSummary) return;

    let subtotal = 0;
    cart.forEach(function (item) {
        subtotal += getNumericPrice(item.price) * item.quantity;
    });

    const delivery = cart.length > 0 ? 250 : 0;
    let discount = 0;

    const appliedPromo = JSON.parse(localStorage.getItem("yummyPrepAppliedPromo"));

    if (appliedPromo && subtotal > 0) {
        if (appliedPromo.discountType === "Percentage" || appliedPromo.type === "Percentage") {
            const pct = appliedPromo.discountValue || appliedPromo.value || 0;
            discount = subtotal * (pct / 100);
        } else if (appliedPromo.discountType === "Fixed" || appliedPromo.type === "Fixed") {
            discount = appliedPromo.discountValue || appliedPromo.value || 0;
        }

        if (discount > subtotal) {
            discount = subtotal;
        }
    } else if (subtotal === 0) {
        localStorage.removeItem("yummyPrepAppliedPromo");
    }

    const total = subtotal + delivery - discount;

    orderSummary.innerHTML = `
        <h2>Order Summary</h2>

        <div class="summary-row">
            <span>Subtotal</span>
            <strong>Rs. ${subtotal.toFixed(2)}</strong>
        </div>

        <div class="summary-row">
            <span>Delivery</span>
            <strong>Rs. ${delivery.toFixed(2)}</strong>
        </div>

        <div class="summary-row">
            <span>Discount</span>
            <strong class="discount">− Rs. ${discount.toFixed(2)}</strong>
        </div>

        <div class="summary-line"></div>

        <div class="total-row">
            <span>Total</span>
            <strong>Rs. ${Math.max(0, total).toFixed(2)}</strong>
        </div>

        <!-- AVAILABLE PROMOTIONS BANNER -->
        <div id="activePromosContainer" style="background: #fdf8e2; border: 1px dashed #f57c00; padding: 12px; border-radius: 6px; margin: 15px 0;">
            <strong style="color: #d35400; font-size: 14px;">🎁 Available Offers & Coupons:</strong>
            <div id="promoList" style="margin-top: 6px; font-size: 13px; color: #333; line-height: 1.4;">
                Loading available offers...
            </div>
        </div>

        <div class="promo">
            <h4>Have a promo code?</h4>
            <div class="promo-box">
                <input type="text" id="promoInput" placeholder="Enter code" value="${appliedPromo ? appliedPromo.code : ''}">
                <button type="button" id="applyPromoBtn">Apply</button>
            </div>
            <p id="promoMessage" class="promo-message ${appliedPromo ? 'success' : ''}">
                ${appliedPromo ? `Promo code '${appliedPromo.code}' applied!` : ''}
            </p>
        </div>

        ${
            cart.length > 0
                ? `<a href="checkout.html" class="checkout-btn">
                    Proceed to Checkout <span>→</span>
                   </a>`
                : `<div class="disabled-checkout">
                    Add items to continue
                   </div>`
        }

        <p class="secure-text">🔒 Secure checkout</p>
    `;

    setupPromoButton();
    loadActivePromos();
}

function setupPromoButton() {
    const applyPromoBtn = document.getElementById("applyPromoBtn");
    const promoInput = document.getElementById("promoInput");
    const promoMessage = document.getElementById("promoMessage");

    if (!applyPromoBtn || !promoInput) return;

    applyPromoBtn.addEventListener("click", function () {
        const code = promoInput.value.trim();

        if (code === "") {
            if (promoMessage) {
                promoMessage.textContent = "Please enter a valid promo code.";
                promoMessage.className = "promo-message error";
            }
            return;
        }

        let subtotal = 0;
        cart.forEach(function (item) {
            subtotal += getNumericPrice(item.price) * item.quantity;
        });

        // Updated to use promo_api.php
        fetch(`../php/promo_api.php?action=validate_promo&code=${encodeURIComponent(code)}&subtotal=${subtotal}`)
            .then(res => res.json())
            .then(data => {
                if (data.status === "success") {
                    localStorage.setItem("yummyPrepAppliedPromo", JSON.stringify(data.data));
                    if (promoMessage) {
                        promoMessage.textContent = data.message || "Promo code applied successfully!";
                        promoMessage.className = "promo-message success";
                    }
                    updateSummary();
                } else {
                    localStorage.removeItem("yummyPrepAppliedPromo");
                    if (promoMessage) {
                        promoMessage.textContent = data.message || "Invalid or expired promo code.";
                        promoMessage.className = "promo-message error";
                    }
                    updateSummary();
                }
            })
            .catch(err => {
                console.error("Promo code error:", err);
            });
    });
}

function loadActivePromos() {
    // Updated to use promo_api.php and get_active_promos action
    fetch("../php/promo_api.php?action=get_active_promos")
        .then(res => res.json())
        .then(data => {
            const promoListDiv = document.getElementById("promoList");
            if (!promoListDiv) return;

            if (data.status === "success" && data.data && data.data.length > 0) {
                let html = "";
                data.data.forEach(promo => {
                    let discountText = promo.discount_type === 'Percentage' ? `${promo.discount_value}% OFF` : `Rs. ${promo.discount_value} OFF`;
                    html += `<div style="margin-bottom: 4px;">
                        🔹 <b>${promo.code}</b>: ${promo.title} (${discountText}) - <i>${promo.description || ''}</i>
                    </div>`;
                });
                promoListDiv.innerHTML = html;
            } else {
                promoListDiv.innerHTML = "No active promotions at the moment.";
            }
        })
        .catch(err => {
            console.error("Error loading active promos:", err);
            const promoListDiv = document.getElementById("promoList");
            if (promoListDiv) {
                promoListDiv.innerHTML = "Could not load offers.";
            }
        });
}

function increaseQuantity(index) {
    if (cart[index]) {
        cart[index].quantity++;
        saveCart();
        displayCart();
    }
}

function decreaseQuantity(index) {
    if (cart[index]) {
        if (cart[index].quantity > 1) {
            cart[index].quantity--;
        } else {
            cart.splice(index, 1);
        }
        saveCart();
        displayCart();
    }
}

function removeItem(index) {
    if (cart[index]) {
        cart.splice(index, 1);
        saveCart();
        displayCart();
    }
}

window.increaseQuantity = increaseQuantity;
window.decreaseQuantity = decreaseQuantity;
window.removeItem = removeItem;

displayCart();