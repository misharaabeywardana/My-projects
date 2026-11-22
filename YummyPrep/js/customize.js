/* =========================================
   YUMMYPREP - CUSTOMIZE MEAL
   ========================================= */

document.addEventListener("DOMContentLoaded", function () {

    const estimatedTotal = document.getElementById("estimatedTotal");
    const addCustomMealBtn = document.getElementById("addCustomMealBtn");
    const allOptions = document.querySelectorAll(".option-card input");
    const radioOptionCards = document.querySelectorAll(".option-card");

    function getOptionCard(input) {
        return input ? input.closest(".option-card") : null;
    }

    function getOptionName(input) {
        const card = getOptionCard(input);
        if (!card) return "";
        const heading = card.querySelector("h3");
        return heading ? heading.textContent.trim() : "";
    }

    function getOptionPrice(input) {
        const card = getOptionCard(input);
        if (!card) return 0;
        const priceElement = card.querySelector("strong");
        if (!priceElement) return 0;
        const priceText = priceElement.textContent.trim().toLowerCase();

        if (
            priceText.includes("free") ||
            priceText.includes("included") ||
            priceText.includes("no extra charge")
        ) {
            return 0;
        }

        const numericMatch = priceText.replace(/,/g, "").match(/\d+/);
        return numericMatch ? Number(numericMatch[0]) : 0;
    }

    function calculateTotal() {
        let total = 0;
        allOptions.forEach(function (input) {
            if (input.checked) {
                total += getOptionPrice(input);
            }
        });

        if (estimatedTotal) {
            estimatedTotal.textContent = "Rs. " + total.toLocaleString();
        }
        return total;
    }

    allOptions.forEach(function (input) {
        input.addEventListener("change", function () {
            calculateTotal();
        });
    });

    radioOptionCards.forEach(function (card) {
        const radio = card.querySelector('input[type="radio"]');
        if (!radio) return;

        card.addEventListener("click", function (event) {
            event.preventDefault();
            const wasChecked = radio.checked;

            if (wasChecked) {
                radio.checked = false;
            } else {
                const groupName = radio.name;
                document.querySelectorAll(`input[type="radio"][name="${groupName}"]`).forEach(function (otherRadio) {
                    otherRadio.checked = false;
                });
                radio.checked = true;
            }
            calculateTotal();
        });
    });

    function getSelectedItems() {
        const selectedItems = [];
        allOptions.forEach(function (input) {
            if (input.checked) {
                const name = getOptionName(input);
                if (name) selectedItems.push(name);
            }
        });
        return selectedItems;
    }

    function getSelectedMealType() {
        const selectedMealType = document.querySelector('input[name="meal-type"]:checked');
        if (!selectedMealType) return null;
        return {
            input: selectedMealType,
            name: getOptionName(selectedMealType)
        };
    }

    function loadCart() {
        try {
            const savedCart = JSON.parse(localStorage.getItem("yummyPrepCart"));
            if (Array.isArray(savedCart)) return savedCart;
        } catch (error) {
            console.error("Error reading cart:", error);
        }
        return [];
    }

    function saveCart(cart) {
        localStorage.setItem("yummyPrepCart", JSON.stringify(cart));
    }

    if (addCustomMealBtn) {
        addCustomMealBtn.addEventListener("click", function () {
            const mealType = getSelectedMealType();
            if (!mealType) {
                alert("Please choose your meal type first.");
                return;
            }

            const selectedItems = getSelectedItems();
            if (selectedItems.length <= 1) {
                alert("Please select ingredients for your meal.");
                return;
            }

            const total = calculateTotal();
            if (total <= 0) {
                alert("Please select priced ingredients before adding your meal to the cart.");
                return;
            }

            const customMealName = "Customized " + mealType.name;
            const cart = loadCart();
            const selectionsDescription = selectedItems.join(", ");

            const customMeal = {
                id: "CUSTOM-" + Date.now(),
                name: customMealName,
                item_name: customMealName + " — [" + selectionsDescription + "]", // සියලුම ingredients සහ beverages මෙහි ඇතුළත් වේ
                category: "Customized",
                price: total,
                image: "../images/customizeFood.jpeg",
                quantity: 1,
                customized: true,
                mealType: mealType.name,
                selections: selectedItems
            };

            cart.push(customMeal);
            saveCart(cart);

            alert(customMealName + " with your selections added to your cart!");
            window.location.href = "cart.html";
        });
    }

    calculateTotal();
});