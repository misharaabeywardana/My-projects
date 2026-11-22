console.log("Velora website loaded successfully!");

// ==========================================================
// Shared front-end booking store
// ==========================================================
function getBookings() {
  try { return JSON.parse(localStorage.getItem("veloraBookings")) || []; }
  catch (e) { return []; }
}

function saveBookings(bookings) {
  localStorage.setItem("veloraBookings", JSON.stringify(bookings));
}

function addBooking(booking) {
  const bookings = getBookings();
  try { const logged = JSON.parse(localStorage.getItem("veloraLoggedIn")); booking.email = logged?.email || ""; } catch(e) { booking.email = ""; }
  booking.id = "VLR" + Date.now().toString().slice(-6);
  booking.bookedOn = new Date().toLocaleDateString("en-GB", { day:"2-digit", month:"short", year:"numeric" });
  bookings.unshift(booking);
  saveBookings(bookings);
  return booking;
}

function removeBooking(id) {
  saveBookings(getBookings().filter(function (b) { return b.id !== id; }));
}

function cancelDynamicBooking(id) {
  const bookings = getBookings();
  const booking = bookings.find(function (b) { return b.id === id; });
  if (!booking) return;
  booking.status = "Cancelled";
  booking.cancelledOn = new Date().toLocaleDateString("en-GB", { day:"2-digit", month:"short", year:"numeric" });
  saveBookings(bookings);
  renderBookings();
}

// ==========================================================
// Navigation: keep active page clear and remove old dark login highlight
// ==========================================================
document.addEventListener("DOMContentLoaded", function () {
  // Start the updated front-end booking history with a clean store.
  // This removes the old demo reservations from earlier versions once only.
  if (localStorage.getItem("veloraBookingStoreVersion") !== "3") {
    localStorage.removeItem("veloraBookings");
    localStorage.removeItem("veloraPendingBooking");
    localStorage.removeItem("veloraBookingSuccess");
    localStorage.setItem("veloraBookingStoreVersion", "3");
  }

  const current = location.pathname.split("/").pop() || "index.html";
  document.querySelectorAll(".navbar .nav-link").forEach(function (link) {
    link.classList.remove("active");
    const href = (link.getAttribute("href") || "").split("?")[0];
    if (href === current) link.classList.add("active");
  });

  setupHomeSearch();
  setupHotelsPage();
  setupBookingButtons();
  setupPaymentPage();
  renderBookings();
  showBookingSuccessIfNeeded();
});

// ==========================================================
// Home page search -> Hotels & Rooms with real front-end filtering
// ==========================================================
function setupHomeSearch() {
  const form = document.getElementById("homeSearchForm");
  if (!form) return;

  form.addEventListener("submit", function (event) {
    event.preventDefault();
    event.stopPropagation();

    const destination = document.getElementById("homeDestination")?.value.trim() || "";
    if (destination && !isSupportedDestination(destination)) { showDestinationUnavailable(destination); return; }
    const checkIn = document.getElementById("homeCheckIn")?.value || "";
    const checkOut = document.getElementById("homeCheckOut")?.value || "";
    const guests = document.getElementById("homeGuests")?.value || "";

    if (guests && (Number(guests) < 1 || Number(guests) > 20)) {
      alert("Please enter a guest count between 1 and 20.");
      return;
    }

    if (checkIn && checkOut && checkOut <= checkIn) {
      alert("Check-out date must be after the check-in date.");
      return;
    }

    const params = new URLSearchParams();
    if (destination) params.set("destination", destination);
    if (checkIn) params.set("checkin", checkIn);
    if (checkOut) params.set("checkout", checkOut);
    if (guests) params.set("guests", guests);
    window.location.href = "hotels.html?" + params.toString();
  });
}

// ==========================================================
// Hotels & Rooms filtering
// ==========================================================
function guestCountToCategory(guests) {
  const count = parseInt(guests, 10);
  if (!Number.isFinite(count)) return "all";
  if (count <= 2) return "standard";
  if (count === 3) return "family";
  return "suite";
}

function applyRoomFilters(destination, category) {
  const cards = document.querySelectorAll(".room-col");
  const filterButtons = document.querySelectorAll(".room-filters button");
  filterButtons.forEach(function (b) { b.classList.remove("active"); });
  const activeButton = document.querySelector('.room-filters button[data-filter="' + category + '"]');
  if (activeButton) activeButton.classList.add("active");

  const searchText = (destination || "").trim().toLowerCase();
  let visible = 0;
  cards.forEach(function (card) {
    const categoryMatch = category === "all" || card.getAttribute("data-category") === category;
    const roomDestination = (card.getAttribute("data-destination") || "").trim().toLowerCase();
    const destinationMatch = !searchText || roomDestination === searchText;
    const show = categoryMatch && destinationMatch;
    card.style.display = show ? "" : "none";
    if (show) visible++;
  });
  return visible;
}

function setupHotelsPage() {
  const roomsSection = document.getElementById("roomsSection");
  const filterButtons = document.querySelectorAll(".room-filters button");
  const searchForm = document.getElementById("roomSearchForm");
  if (!roomsSection && !searchForm) return;

  filterButtons.forEach(function (button) {
    button.addEventListener("click", function () {
      applyRoomFilters("", button.getAttribute("data-filter"));
      roomsSection?.scrollIntoView({ behavior:"smooth", block:"start" });
    });
  });

  if (searchForm) {
    searchForm.addEventListener("submit", function (event) {
      event.preventDefault();
      event.stopPropagation();
      if (!searchForm.checkValidity()) {
        searchForm.classList.add("was-validated");
        return;
      }
      const checkIn = document.getElementById("checkIn").value;
      const checkOut = document.getElementById("checkOut").value;
      if (checkOut <= checkIn) {
        document.getElementById("checkOut").setCustomValidity("Check-out must be after check-in.");
        searchForm.classList.add("was-validated");
        return;
      }
      document.getElementById("checkOut").setCustomValidity("");
      searchForm.classList.remove("was-validated");
      const guests = document.getElementById("guestCount").value;
      const destination = document.getElementById("hotelDestination")?.value.trim() || "";
      if (destination && !isSupportedDestination(destination)) { showDestinationUnavailable(destination); return; }
      applyRoomFilters(destination, "all");
      roomsSection?.scrollIntoView({ behavior:"smooth", block:"start" });
    });
  }

  const params = new URLSearchParams(window.location.search);
  const destination = params.get("destination") || "";
  const checkIn = params.get("checkin") || "";
  const checkOut = params.get("checkout") || "";
  const guests = params.get("guests") || "";
  const alertZone = document.getElementById("bookingAlertZone");

  if (destination && document.getElementById("hotelDestination")) document.getElementById("hotelDestination").value = destination;
  if (checkIn && document.getElementById("checkIn")) document.getElementById("checkIn").value = checkIn;
  if (checkOut && document.getElementById("checkOut")) document.getElementById("checkOut").value = checkOut;
  if (guests && document.getElementById("guestCount")) document.getElementById("guestCount").value = guests;

  if (destination || guests) {
    const count = applyRoomFilters(destination, "all");
    if (alertZone) {
      alertZone.innerHTML = '<div class="alert alert-info alert-dismissible fade show" role="alert"><i class="bi bi-search me-2"></i>' +
        (destination ? 'Showing stays matching <strong>' + escapeHtml(destination) + '</strong>' : 'Showing stays matching your guest preference') +
        (count === 0 ? ' — no exact matches were found, so try another destination or guest count.' : ' — ' + count + ' stay' + (count === 1 ? '' : 's') + ' available.') +
        '<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>';
    }
    roomsSection?.scrollIntoView({ behavior:"smooth", block:"start" });
  }
}

// ==========================================================
// Book Now: save selected reservation details, then go to Account
// ==========================================================
function setupBookingButtons() {
  document.querySelectorAll(".btn-book-now").forEach(function (button) {
    button.addEventListener("click", function (event) {
      event.preventDefault();
      const modal = button.closest(".modal");
      const title = modal?.querySelector(".modal-title")?.textContent.trim() || "Selected Stay";
      const priceEl = modal?.querySelector(".modal-price");
      const price = priceEl ? priceEl.childNodes[0].textContent.trim() : "";
      const page = location.pathname.split("/").pop();
      let hotel = title;
      let room = "Selected Room";

      if (page === "index.html" || page === "") {
        const map = { room1Modal:"Seaside Haven Hotel & Resort", room2Modal:"Sunset Cove Retreat", room3Modal:"Royal Bay Hotel" };
        hotel = map[modal?.id] || title;
        room = "Premium Guest Room";
      } else {
        room = title;
        const card = document.querySelector('[data-bs-target="#' + (modal?.id || '') + '"]')?.closest(".room-col");
        const locationText = card?.querySelector(".text-muted")?.textContent.trim();
        hotel = locationText ? "Velora Stay – " + locationText.replace(/,\s*Sri Lanka/i, "") : "Velora Hotel";
      }

      const params = new URLSearchParams(window.location.search);
      const checkIn = params.get("checkin") || document.getElementById("checkIn")?.value || document.getElementById("homeCheckIn")?.value || "";
      const checkOut = params.get("checkout") || document.getElementById("checkOut")?.value || document.getElementById("homeCheckOut")?.value || "";
      const guests = params.get("guests") || document.getElementById("guestCount")?.value || document.getElementById("homeGuests")?.value || "2";
      const destination = params.get("destination") || document.getElementById("hotelDestination")?.value.trim() || "";

      localStorage.setItem("veloraPendingBooking", JSON.stringify({
        hotel: hotel, room: room, price: price, checkIn: checkIn, checkOut: checkOut,
        guests: guests, destination: destination, status: "Pending Payment"
      }));

      const loggedIn = localStorage.getItem("veloraLoggedIn");
      window.location.href = loggedIn ? "my_bookings.html?payment=1" : "account.html";
    });
  });
}

// ==========================================================
// Payment page in My Bookings
// ==========================================================
function setupPaymentPage() {
  const section = document.getElementById("paymentSection");
  const form = document.getElementById("paymentForm");
  if (!section || !form) return;

  const pending = getPendingBooking();
  const params = new URLSearchParams(window.location.search);
  if (!pending || params.get("payment") !== "1") return;

  section.classList.remove("d-none");
  const summary = document.getElementById("paymentSummary");
  if (summary) summary.innerHTML = '<div class="payment-summary-title">Your Reservation</div>' +
    '<div class="payment-summary-row"><span>Hotel</span><strong>' + escapeHtml(pending.hotel) + '</strong></div>' +
    '<div class="payment-summary-row"><span>Room</span><strong>' + escapeHtml(pending.room) + '</strong></div>' +
    '<div class="payment-summary-row"><span>Check-in</span><strong>' + formatDate(pending.checkIn) + '</strong></div>' +
    '<div class="payment-summary-row"><span>Check-out</span><strong>' + formatDate(pending.checkOut) + '</strong></div>' +
    '<div class="payment-summary-row total"><span>Rate</span><strong>' + escapeHtml(pending.price || "LKR —") + '</strong></div>';

  document.querySelectorAll(".payment-method").forEach(function (btn) {
    btn.addEventListener("click", function () {
      document.querySelectorAll(".payment-method").forEach(function (b) { b.classList.remove("active"); });
      btn.classList.add("active");
      const card = btn.dataset.method === "card";
      document.getElementById("cardFields")?.classList.toggle("d-none", !card);
      document.getElementById("walletFields")?.classList.toggle("d-none", card);
    });
  });

  form.addEventListener("submit", function (event) {
    event.preventDefault();
    const method = document.querySelector(".payment-method.active")?.dataset.method || "card";
    if (method === "card") {
      const fields = ["cardName","cardNumber","cardExpiry","cardCvv"];
      if (fields.some(function (id) { return !document.getElementById(id)?.value.trim(); })) {
        alert("Please complete the card payment details."); return;
      }
    } else if (!document.getElementById("walletContact")?.value.trim()) {
      alert("Please enter your digital wallet contact."); return;
    }

    const booking = addBooking({ ...pending, paymentMethod: method === "card" ? "Card Payment" : "Digital Wallet", status: "Confirmed" });
    localStorage.removeItem("veloraPendingBooking");
    localStorage.setItem("veloraBookingSuccess", JSON.stringify(booking));
    window.location.href = "my_bookings.html?success=1";
  });
}

function getPendingBooking() {
  try { return JSON.parse(localStorage.getItem("veloraPendingBooking")); }
  catch (e) { return null; }
}

// ==========================================================
// My Bookings table + View modal + Cancel state
// ==========================================================
function renderBookings() {
  const tbody = document.getElementById("bookingsTableBody");
  if (!tbody) return;
  const allBookings = getBookings();
  let loggedEmail = "";
  try { loggedEmail = (JSON.parse(localStorage.getItem("veloraLoggedIn")) || {}).email || ""; } catch (e) {}
  // Show only reservations belonging to the currently signed-in email.
  const bookings = loggedEmail
    ? allBookings.filter(function (booking) {
        return String(booking.email || "").trim().toLowerCase() === loggedEmail.trim().toLowerCase();
      })
    : [];
  const countEl = document.getElementById("bookingCount");
  if (countEl) countEl.textContent = bookings.length + (bookings.length === 1 ? " Booking" : " Bookings");

  if (!bookings.length) {
    tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5"><div class="empty-bookings"><i class="bi bi-calendar2-check"></i><h5>No bookings yet</h5><p>Choose a hotel, complete payment, and your reservation will appear here.</p><a href="hotels.html" class="btn btn-primary">Explore Hotels</a></div></td></tr>';
    return;
  }

  tbody.innerHTML = bookings.map(function (booking) {
    const statusClass = booking.status === "Cancelled" ? "cancelled" : booking.status === "Pending" ? "pending" : "confirmed";
    const canCancel = booking.status !== "Cancelled";
    return '<tr>' +
      '<td><span class="booking-id">#' + escapeHtml(booking.id) + '</span></td>' +
      '<td><div class="hotel-name"><i class="bi bi-building"></i><div><strong>' + escapeHtml(booking.hotel || booking.name || "Velora Hotel") + '</strong><small>' + escapeHtml(booking.destination || "Sri Lanka") + '</small></div></div></td>' +
      '<td>' + escapeHtml(booking.room || booking.name || "Selected Room") + '</td>' +
      '<td><i class="bi bi-calendar3 me-1"></i>' + formatDate(booking.checkIn) + '</td>' +
      '<td><i class="bi bi-calendar3 me-1"></i>' + formatDate(booking.checkOut) + '</td>' +
      '<td><span class="badge ' + statusClass + '">' + escapeHtml(booking.status || "Confirmed") + '</span></td>' +
      '<td><div class="action-buttons"><button class="btn view-btn" onclick="viewBooking(\'' + booking.id + '\')"><i class="bi bi-eye"></i> View</button>' +
      (canCancel ? '<button class="btn cancel-btn" onclick="askCancelBooking(\'' + booking.id + '\')">Cancel</button>' : '') + '</div></td>' +
      '</tr>';
  }).join("");
}

function viewBooking(id) {
  const booking = getBookings().find(function (b) { return b.id === id; });
  if (!booking) return;
  const body = document.getElementById("bookingModalBody");
  if (!body) return;
  body.innerHTML = '<div class="detail-row"><span>Booking ID</span><strong>#' + escapeHtml(booking.id) + '</strong></div>' +
    '<div class="detail-row"><span>Hotel</span><strong>' + escapeHtml(booking.hotel || booking.name) + '</strong></div>' +
    '<div class="detail-row"><span>Room</span><strong>' + escapeHtml(booking.room || booking.name) + '</strong></div>' +
    '<div class="detail-row"><span>Guests</span><strong>' + escapeHtml(booking.guests || "2") + '</strong></div>' +
    '<div class="detail-row"><span>Check-in</span><strong>' + formatDate(booking.checkIn) + '</strong></div>' +
    '<div class="detail-row"><span>Check-out</span><strong>' + formatDate(booking.checkOut) + '</strong></div>' +
    '<div class="detail-row"><span>Payment</span><strong>' + escapeHtml(booking.paymentMethod || "Not specified") + '</strong></div>' +
    '<div class="detail-row"><span>Status</span><span class="badge ' + (booking.status === "Cancelled" ? "cancelled" : booking.status === "Pending" ? "pending" : "confirmed") + '">' + escapeHtml(booking.status) + '</span></div>';
  bootstrap.Modal.getOrCreateInstance(document.getElementById("bookingModal")).show();
}

let bookingToCancel = null;
function askCancelBooking(id) {
  bookingToCancel = id;
  const modal = document.getElementById("cancelModal");
  bootstrap.Modal.getOrCreateInstance(modal).show();
}

function confirmCancelBooking() {
  if (!bookingToCancel) return;
  cancelDynamicBooking(bookingToCancel);
  bookingToCancel = null;
  bootstrap.Modal.getInstance(document.getElementById("cancelModal"))?.hide();
}

// ==========================================================
// Success popup after payment
// ==========================================================
function showBookingSuccessIfNeeded() {
  const params = new URLSearchParams(window.location.search);
  if (params.get("success") !== "1") return;
  let booking = null;
  try { booking = JSON.parse(localStorage.getItem("veloraBookingSuccess")); } catch (e) {}
  if (!booking) return;
  const modal = document.getElementById("successModal");
  if (!modal) return;
  const detail = document.getElementById("successBookingText");
  if (detail) detail.innerHTML = '<strong>' + escapeHtml(booking.hotel) + '</strong><br>' + escapeHtml(booking.room) + '<br><span>Booking ID: #' + escapeHtml(booking.id) + '</span>';
  bootstrap.Modal.getOrCreateInstance(modal).show();
  localStorage.removeItem("veloraBookingSuccess");
  history.replaceState({}, document.title, "my_bookings.html");
}

// ==========================================================
// Offers / contact forms from the original project
// ==========================================================
function selectOffer(offerName) {
  const packageSelect = document.getElementById("package");
  if (packageSelect) packageSelect.value = offerName;
  document.getElementById("dealForm")?.scrollIntoView({ behavior:"smooth" });
  document.getElementById("guestName")?.focus();
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>'"]/g, function (c) { return ({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;","\"":"&quot;"})[c]; });
}

function formatDate(value) {
  if (!value) return "Not selected";
  const date = new Date(value + "T00:00:00");
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("en-GB", { day:"2-digit", month:"short", year:"numeric" });
}


// ==========================================================
// Account / Login functionality
// ==========================================================
/* ================= HELPERS ================= */

function setHeading(badge, heading, subtext) {

    document.getElementById("pageBadge").innerText = badge;
    document.getElementById("pageHeading").innerText = heading;
    document.getElementById("pageSubtext").innerText = subtext;

}

function hideAllViews() {

    document.getElementById("loginView").classList.add("d-none");
    document.getElementById("registerView").classList.add("d-none");
    document.getElementById("profileView").classList.add("d-none");

}



/* ================= SHOW REGISTER ================= */

function showRegister() {

    hideAllViews();

    document.getElementById("registerView")
        .classList.remove("d-none");

    setHeading(
        "Join Velora",
        "Create an Account",
        "Join Velora and start planning your perfect stay."
    );

    hideMessage();

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });

}



/* ================= SHOW LOGIN ================= */

function showLogin() {

    hideAllViews();

    document.getElementById("loginView")
        .classList.remove("d-none");

    setHeading(
        "Your Travel Account",
        "Welcome to Velora",
        "Your perfect stay is just a sign in away."
    );

    hideMessage();

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });

}



/* ================= SHOW PROFILE ================= */

function showProfile() {

    hideAllViews();

    document.getElementById("profileView")
        .classList.remove("d-none");

    setHeading(
        "My Account",
        "My Profile",
        "Manage your personal information and account details."
    );

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });

}



/* ================= SHOW MESSAGE ================= */

function showMessage(message, type) {

    let messageBox =
        document.getElementById("messageBox");

    let messageText =
        document.getElementById("messageText");


    messageText.innerText = message;


    messageBox.className =
        "alert alert-" + type;


    messageBox.classList.remove("d-none");


    messageBox.scrollIntoView({
        behavior: "smooth",
        block: "center"
    });

}



/* ================= HIDE MESSAGE ================= */

function hideMessage() {

    document.getElementById("messageBox")
        .classList.add("d-none");

}



/* ================= ACCOUNT STORE (front-end demo) ================= */

function getAccounts() {

    try {
        return JSON.parse(localStorage.getItem("veloraAccounts")) || [];
    } catch (e) {
        return [];
    }

}

function saveAccounts(accounts) {

    localStorage.setItem("veloraAccounts", JSON.stringify(accounts));

}

function findAccountByEmail(email) {

    const accounts = getAccounts();

    return accounts.find(function (acc) {
        return acc.email.toLowerCase() === email.toLowerCase();
    });

}

function populateKnownEmails() {

    const datalist = document.getElementById("knownEmails");
    if (!datalist) return;

    const accounts = getAccounts();

    datalist.innerHTML = "";

    accounts.forEach(function (acc) {
        const option = document.createElement("option");
        option.value = acc.email;
        datalist.appendChild(option);
    });

}

function nameFromEmail(email) {

    const localPart = email.split("@")[0];

    return localPart
        .replace(/[._]+/g, " ")
        .split(" ")
        .filter(Boolean)
        .map(function (word) {
            return word.charAt(0).toUpperCase() + word.slice(1);
        })
        .join(" ");

}

function applyProfileData(name, email, isRegistered) {

    document.getElementById("profileAvatar").innerText = name.charAt(0).toUpperCase();
    document.getElementById("profileNameHeading").innerText = name;
    document.getElementById("profileTypeText").innerText = "Velora Customer";
    document.getElementById("profileFullName").value = name;
    document.getElementById("profileEmailAddress").value = email;

}



/* ================= SIGN IN ================= */

function signIn() {

    let email =
        document.getElementById("loginEmail").value.trim();

    let password =
        document.getElementById("loginPassword").value.trim();


    if (email === "" || password === "") {

        showMessage(
            "Please enter your email and password.",
            "danger"
        );

        return;

    }


    const account = findAccountByEmail(email);

    if (account && account.password !== password) {

        showMessage(
            "Incorrect password for this account. Please try again.",
            "danger"
        );

        return;

    }

    const displayName = account ? account.name : nameFromEmail(email);

    applyProfileData(displayName, email, Boolean(account));
    localStorage.setItem("veloraLoggedIn", JSON.stringify({name: displayName, email: email}));

    showProfile();

    showMessage(
        "Sign in successful! Welcome back to Velora, " + displayName + ".",
        "success"
    );

}



/* ================= CREATE ACCOUNT ================= */

function createAccount() {

    let name =
        document.getElementById("fullName").value.trim();

    let email =
        document.getElementById("registerEmail").value.trim();

    let password =
        document.getElementById("registerPassword").value.trim();

    let confirmPassword =
        document.getElementById("confirmPassword").value.trim();


    if (
        name === "" ||
        email === "" ||
        password === "" ||
        confirmPassword === ""
    ) {

        showMessage(
            "Please fill in all the fields.",
            "danger"
        );

        return;

    }


    if (password !== confirmPassword) {

        showMessage(
            "Passwords do not match.",
            "danger"
        );

        return;

    }


    const accounts = getAccounts();

    const existingIndex = accounts.findIndex(function (acc) {
        return acc.email.toLowerCase() === email.toLowerCase();
    });

    const accountRecord = { name: name, email: email, password: password };

    if (existingIndex >= 0) {
        accounts[existingIndex] = accountRecord;
    } else {
        accounts.push(accountRecord);
    }

    saveAccounts(accounts);
    populateKnownEmails();
    localStorage.setItem("veloraLoggedIn", JSON.stringify({name: name, email: email}));
    applyProfileData(name, email, true);
    showProfile();

    showMessage(
        "Account created successfully! Welcome to Velora.",
        "success"
    );


    /* Clear form */

    document.getElementById("fullName").value = "";

    document.getElementById("registerEmail").value = "";

    document.getElementById("registerPassword").value = "";

    document.getElementById("confirmPassword").value = "";

}



document.addEventListener("DOMContentLoaded", function () { populateKnownEmails(); restoreLoggedInProfile(); });



function goToPayment() {
    if (!localStorage.getItem("veloraPendingBooking")) {
        showMessage("Please choose a hotel and room first by clicking Book Now.", "info");
        return;
    }
    window.location.href = "my_bookings.html?payment=1";
}

function restoreLoggedInProfile() {
    try {
        const saved = JSON.parse(localStorage.getItem("veloraLoggedIn"));
        if (saved && saved.name && saved.email) {
            applyProfileData(saved.name, saved.email, true);
            showProfile();
        }
    } catch (e) {}
}

/* ================= FORGOT PASSWORD ================= */

function forgotPassword() {

    showMessage(
        "Please contact Velora support to reset your password.",
        "info"
    );

}



/* ================= LOGOUT ================= */

function logoutUser() {

    localStorage.removeItem("veloraLoggedIn");
    showLogin();

    showMessage(
        "You have been logged out successfully.",
        "success"
    );

}

// Search helpers (kept at global scope for both search forms)
function supportedDestinations() {
  return Array.from(document.querySelectorAll('datalist option')).map(o => o.value.toLowerCase());
}
function isSupportedDestination(value) {
  return supportedDestinations().includes(String(value).trim().toLowerCase());
}
function showDestinationUnavailable(destination) {
  alert('Sorry, ' + destination + ' is not available in the Velora system. Please choose another destination.');
}

function showDealMessage(offerName) {

    // Selected deal name
    document.getElementById("selectedDeal").textContent = offerName;

    // Open Bootstrap modal
    const modal = new bootstrap.Modal(
        document.getElementById("dealMessageModal")
    );

    modal.show();
}


// ================= REQUEST OFFER =================

const dealForm = document.getElementById("dealForm");

if (dealForm) {

    dealForm.addEventListener("submit", function(event) {

        event.preventDefault();

        const name = document.getElementById("guestName").value.trim();
        const email = document.getElementById("guestEmail").value.trim();
        const selectedPackage = document.getElementById("package").value;

        // Check required fields
        if (!name || !email || !selectedPackage) {

            alert("Please fill in your name, email and preferred package.");

            return;
        }

        // Put information into popup
        document.getElementById("requestName").textContent = name;

        document.getElementById("requestPackage").textContent =
            selectedPackage;

        // Show Bootstrap popup
        const modal = new bootstrap.Modal(
            document.getElementById("offerRequestModal")
        );

        modal.show();

        // Clear form after submission
        dealForm.reset();

    });

}

// ================= CONTACT MESSAGE =================

const contactForm = document.getElementById("contactForm");

if (contactForm) {

    contactForm.addEventListener("submit", function(event) {

        event.preventDefault();

        const modal = new bootstrap.Modal(
            document.getElementById("messageModal")
        );

        modal.show();

        contactForm.reset();

    });

}