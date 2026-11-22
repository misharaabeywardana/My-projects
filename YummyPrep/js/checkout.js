/* =========================================
   YUMMYPREP - CHECKOUT & ORDER PLACEMENT
   ========================================= */

function readJSON(key) {
    try {
        return JSON.parse(localStorage.getItem(key));
    } catch (e) {
        return null;
    }
}

const checkoutCart = readJSON("yummyPrepCart") || [];
const appliedPromo = readJSON("yummyPrepAppliedPromo");
const DELIVERY_FEE = 250;

const checkoutItems = document.getElementById("checkoutItems");
const checkoutSubtotal = document.getElementById("checkoutSubtotal");
const checkoutDelivery = document.getElementById("checkoutDelivery");
const checkoutDiscount = document.getElementById("checkoutDiscount");
const checkoutTotal = document.getElementById("checkoutTotal");
const placeOrderBtn = document.getElementById("placeOrderBtn");

const cardFields = document.getElementById("cardFields");
const cashNote = document.getElementById("cashNote");
const cardNumberInput = document.getElementById("cardNumber");
const cardExpiryInput = document.getElementById("cardExpiry");
const cardCvvInput = document.getElementById("cardCvv");

function getCheckoutPrice(price) {
    if (typeof price === "number") return price;
    return Number(
        String(price)
            .replace("Rs.", "")
            .replace(/,/g, "")
            .trim()
    ) || 0;
}

function displayCheckout() {
    let subtotal = 0;
    let checkoutHTML = "";

    if (!checkoutItems) return;

    if (checkoutCart.length === 0) {
        checkoutHTML = `
            <div class="checkout-empty">
                <p>Your cart is empty.</p>
                <a href="meals.html">Add Meals →</a>
            </div>
        `;
        checkoutItems.innerHTML = checkoutHTML;
        if (checkoutSubtotal) checkoutSubtotal.textContent = "Rs. 0.00";
        if (checkoutDelivery) checkoutDelivery.textContent = "Rs. 0.00";
        if (checkoutDiscount) checkoutDiscount.textContent = "- Rs. 0.00";
        if (checkoutTotal) checkoutTotal.textContent = "Rs. 0.00";

        if (placeOrderBtn) {
            placeOrderBtn.style.pointerEvents = "none";
            placeOrderBtn.style.opacity = "0.5";
        }
        return;
    }

    checkoutCart.forEach(function (item) {
        const price = getCheckoutPrice(item.price);
        const itemTotal = price * item.quantity;
        subtotal += itemTotal;
        const itemImage = item.image || "../images/chickenBowl1.jpeg";
        const displayName = item.item_name || item.name;

        checkoutHTML += `
            <div class="order-item">
                <div class="item-image">
                    <img src="${itemImage}" alt="${displayName}">
                </div>
                <div class="item-details">
                    <h3>${displayName}</h3>
                    <p>Fresh YummyPrep meal</p>
                    <span>Qty: ${item.quantity}</span>
                </div>
                <strong>Rs. ${itemTotal.toFixed(2)}</strong>
            </div>
        `;
    });

    checkoutItems.innerHTML = checkoutHTML;

    const delivery = DELIVERY_FEE;
    let discount = 0;

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
    }

    const total = Math.max(0, subtotal + delivery - discount);

    if (checkoutSubtotal) checkoutSubtotal.textContent = "Rs. " + subtotal.toFixed(2);
    if (checkoutDelivery) checkoutDelivery.textContent = "Rs. " + delivery.toFixed(2);
    if (checkoutDiscount) checkoutDiscount.textContent = "- Rs. " + discount.toFixed(2);
    if (checkoutTotal) checkoutTotal.textContent = "Rs. " + total.toFixed(2);
}

function fillFields(activeUser) {
    if (!activeUser) return;
    const firstNameInput = document.getElementById("firstName");
    const lastNameInput = document.getElementById("lastName");
    const emailInput = document.getElementById("email");
    const phoneInput = document.getElementById("phone");
    const addressInput = document.getElementById("address");

    const fullName = activeUser.name || activeUser.full_name;
    if (fullName) {
        const nameParts = fullName.trim().split(" ");
        if (firstNameInput && !firstNameInput.value) firstNameInput.value = nameParts[0] || "";
        if (lastNameInput && !lastNameInput.value) lastNameInput.value = nameParts.slice(1).join(" ") || "";
    }

    if (emailInput && !emailInput.value && activeUser.email) emailInput.value = activeUser.email;
    if (phoneInput && !phoneInput.value && activeUser.phone) phoneInput.value = activeUser.phone;

    const savedAddress = activeUser.address || activeUser.delivery_address;
    if (addressInput && !addressInput.value && savedAddress) addressInput.value = savedAddress;
}

function autoFillUserData() {
    if (window.YummyAuth) {
        window.YummyAuth.getUser(fillFields);
        return;
    }
    fillFields(readJSON("yummyPrepUser"));
}

function isCardPayment() {
    const selected = document.querySelector('input[name="paymentMethod"]:checked');
    return !selected || selected.value !== "Cash on Delivery";
}

function updatePaymentFields() {
    const card = isCardPayment();
    if (cardFields) cardFields.hidden = !card;
    if (cashNote) cashNote.hidden = card;

    [cardNumberInput, cardExpiryInput, cardCvvInput].forEach(function (input) {
        if (!input) return;
        input.disabled = !card;
        input.required = card;
        input.classList.remove("field-error");
        if (!card) input.value = "";
    });
}

function validateCardDetails() {
    if (!isCardPayment()) return true;
    const number = cardNumberInput ? cardNumberInput.value.replace(/\s+/g, "") : "";
    const expiry = cardExpiryInput ? cardExpiryInput.value.replace(/\s+/g, "") : "";
    const cvv = cardCvvInput ? cardCvvInput.value.trim() : "";
    let valid = true;

    function mark(input, bad) {
        if (input) input.classList.toggle("field-error", bad);
        if (bad) valid = false;
    }

    mark(cardNumberInput, !/^\d{13,19}$/.test(number));

    const match = expiry.match(/^(\d{2})\/(\d{2})$/);
    let expiryBad = !match || Number(match[1]) < 1 || Number(match[1]) > 12;
    if (!expiryBad) {
        const now = new Date();
        const year = 2000 + Number(match[2]);
        const month = Number(match[1]);
        expiryBad = year < now.getFullYear() || (year === now.getFullYear() && month < now.getMonth() + 1);
    }
    mark(cardExpiryInput, expiryBad);
    mark(cardCvvInput, !/^\d{3,4}$/.test(cvv));

    if (!valid) {
        alert("Please enter valid card details, or choose Cash on Delivery.");
    }
    return valid;
}

function initPaymentToggle() {
    document.querySelectorAll('input[name="paymentMethod"]').forEach(function (radio) {
        radio.addEventListener("change", updatePaymentFields);
    });

    if (cardNumberInput) {
        cardNumberInput.addEventListener("input", function () {
            const digits = cardNumberInput.value.replace(/\D/g, "").slice(0, 16);
            cardNumberInput.value = digits.replace(/(.{4})/g, "$1 ").trim();
        });
    }

    if (cardExpiryInput) {
        cardExpiryInput.addEventListener("input", function () {
            const digits = cardExpiryInput.value.replace(/\D/g, "").slice(0, 4);
            cardExpiryInput.value = digits.length > 2 ? digits.slice(0, 2) + " / " + digits.slice(2) : digits;
        });
    }

    if (cardCvvInput) {
        cardCvvInput.addEventListener("input", function () {
            cardCvvInput.value = cardCvvInput.value.replace(/\D/g, "").slice(0, 4);
        });
    }

    updatePaymentFields();
}

if (placeOrderBtn) {
    placeOrderBtn.addEventListener("click", function (event) {
        event.preventDefault();

        if (checkoutCart.length === 0) {
            alert("Your cart is empty. Please add meals before placing an order.");
            return;
        }

        const firstName = document.getElementById("firstName") ? document.getElementById("firstName").value.trim() : "";
        const lastName = document.getElementById("lastName") ? document.getElementById("lastName").value.trim() : "";
        const email = document.getElementById("email") ? document.getElementById("email").value.trim() : "";
        const phone = document.getElementById("phone") ? document.getElementById("phone").value.trim() : "";
        const address = document.getElementById("address") ? document.getElementById("address").value.trim() : "";

        if (!firstName || !lastName || !email || !phone || !address) {
            alert("Please fill in all required customer details (Name, Email, Phone, Address).");
            return;
        }

        if (!validateCardDetails()) {
            return;
        }

        const deliveryOption = document.querySelector('input[name="deliveryDay"]:checked');
        const deliveryDay = deliveryOption ? deliveryOption.value : "today";

        const paymentOption = document.querySelector('input[name="paymentMethod"]:checked');
        const paymentMethod = paymentOption ? paymentOption.value : "Cash on Delivery";

        const subtotal = checkoutCart.reduce(function (sum, item) {
            return sum + getCheckoutPrice(item.price) * item.quantity;
        }, 0);

        let discount = 0;
        if (appliedPromo) {
            if (appliedPromo.discountType === "Percentage" || appliedPromo.type === "Percentage") {
                const pct = appliedPromo.discountValue || appliedPromo.value || 0;
                discount = subtotal * (pct / 100);
            } else if (appliedPromo.discountType === "Fixed" || appliedPromo.type === "Fixed") {
                discount = appliedPromo.discountValue || appliedPromo.value || 0;
            }
        }

        const orderPayload = {
            firstName: firstName,
            lastName: lastName,
            email: email,
            phone: phone,
            address: address,
            deliveryDay: deliveryDay,
            payment: paymentMethod,
            subtotal: subtotal,
            discount: discount,
            promoCode: appliedPromo ? appliedPromo.code : null,
            items: checkoutCart.map(function (item) {
                return {
                    mealId: item.mealId || null,
                    name: item.item_name || item.name, // සම්පූර්ණ customized විස්තර API වෙත යවයි
                    category: item.category || "Standard",
                    price: getCheckoutPrice(item.price),
                    quantity: item.quantity
                };
            })
        };

        placeOrderBtn.disabled = true;
        placeOrderBtn.textContent = "Processing Order...";

        fetch("../php/orders_api.php?action=place_order", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(orderPayload)
        })
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            if (data.status === "success") {
                localStorage.setItem("latestYummyPrepOrder", JSON.stringify({
                    orderNumber: data.data.orderNumber,
                    total: data.data.total,
                    deliveryDay: data.data.deliveryDay,
                    items: checkoutCart
                }));

                localStorage.removeItem("yummyPrepCart");
                localStorage.removeItem("yummyPrepAppliedPromo");

                window.location.href = `confirm.html?orderNumber=${data.data.orderNumber}`;
            } else {
                alert(data.message || "Failed to place order. Please try again.");
                placeOrderBtn.disabled = false;
                placeOrderBtn.textContent = "Place Order";
            }
        })
        .catch(function (error) {
            console.error("Order Placement Error:", error);
            alert("An error occurred while connecting to the server.");
            placeOrderBtn.disabled = false;
            placeOrderBtn.textContent = "Place Order";
        });
    });
}

displayCheckout();
autoFillUserData();
initPaymentToggle();