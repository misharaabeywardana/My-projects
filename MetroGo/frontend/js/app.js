const API = '/api';

let session = JSON.parse(
    localStorage.getItem('metrogo_session') || 'null'
);

// Check whether the current page is Admin page.
const isAdminPage = window.location.pathname.endsWith('/admin.html');

// Only admin users can open admin.html.
if (isAdminPage && session?.user?.role !== 'admin') {

    window.location.replace('index.html');

}

let buses = [];
let selectedBus = null;
let selectedSeat = null;


// ==========================================
// 1. ESCAPE HTML CHARACTERS
// ==========================================

const esc = s => String(s ?? '').replace(
    /[&<>"']/g,
    c => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[c])
);


// ==========================================
// 2. NOTIFICATION FUNCTION
// ==========================================

function notify(message, bad = false) {

    $('#alertBox').html(`
        <div class="alert alert-${bad ? 'danger' : 'success'} alert-dismissible fade show"
             role="alert">

            ${esc(message)}

            <button class="btn-close"
                    data-bs-dismiss="alert">
            </button>

        </div>
    `);

    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
}


// ==========================================
// 3. BACKEND API REQUEST FUNCTION
// ==========================================

async function request(url, method = 'GET', body) {

    try {

        return await $.ajax({

            url: API + url,

            method: method,

            contentType: 'application/json',

            headers: session
                ? {
                    Authorization: 'Bearer ' + session.token
                }
                : {},

            data: body
                ? JSON.stringify(body)
                : undefined

        });

    } catch (e) {

        throw Error(
            e.responseJSON?.error ||
            'Server unavailable. Check Node.js and MongoDB.'
        );

    }
}


// ==========================================
// 4. LOGIN CHECK
// ==========================================

function mustLogin() {

    if (session) {
        return true;
    }

    notify('Please sign in to continue.', true);

    new bootstrap.Modal('#authModal').show();

    return false;
}


// ==========================================
// 5. USER ROLE MANAGEMENT
// ==========================================

function roleUI() {

    $('#userLabel').text(
        session ? session.user.name : 'Sign in'
    );

    $('.auth-only').toggleClass(
        'd-none',
        !session
    );

    $('.admin-only').toggleClass(
        'd-none',
        session?.user.role !== 'admin'
    );

    $('#logoutBtn').toggleClass(
        'd-none',
        !session
    );

}


// ==========================================
// 6. BUS CARD DISPLAY
// ==========================================

function busCard(b) {

    return `

        <div class="bus-card">

            <div class="row align-items-center g-3">

                <div class="col-md-3">

                    <span class="badge-soft">
                        ${esc(b.Bus_ID)}
                    </span>

                    <h5 class="mt-3 mb-1 fw-bold">
                        ${esc(b.Bus_Name)}
                    </h5>

                    <small class="muted">
                        Air-conditioned service
                    </small>

                </div>


                <div class="col-md-4">

                    <div class="fw-bold">

                        ${esc(b.Departure)}

                        <span class="text-primary">→</span>

                        ${esc(b.Destination)}

                    </div>

                    <div class="muted small mt-2">

                        ${esc(b.Travel_Date)}

                        ·

                        ${esc(b.Departure_Time)}

                        –

                        ${esc(b.Arrival_Time || '—')}

                    </div>

                </div>


                <div class="col-md-2">

                    <div class="fw-bold">
                        ${esc(b.Available_Seats)} seats
                    </div>

                    <small class="muted">
                        available
                    </small>

                </div>


                <div class="col-md-3 text-md-end">

                    <h4 class="text-primary fw-bold mb-2">

                        LKR ${Number(b.Fare).toLocaleString()}

                    </h4>

                    <button
                        class="btn btn-outline-danger btn-sm save-bus"
                        data-id="${esc(b._id)}">

                        ♡ Save

                    </button>

                    <button
                        class="btn btn-primary btn-sm choose-bus"
                        data-id="${esc(b.Bus_ID)}">

                        Select bus

                    </button>

                </div>

            </div>

        </div>

    `;
}


// ==========================================
// 7. LOAD BUSES
// ==========================================

async function loadBuses(q = '') {

    try {

        buses = await request('/buses' + q);

        $('#resultsCount').text(
            buses.length + ' buses found'
        );


        $('#results').html(

            buses.length

                ? buses.map(busCard).join('')

                : '<div class="panel text-center muted">No matching buses. Try another route or date.</div>'

        );


        // Admin Bus List

        $('#adminBusList').html(

            buses.map(b => `

                <tr>

                    <td>${esc(b.Bus_ID)}</td>

                    <td>
                        ${esc(b.Departure)}
                        →
                        ${esc(b.Destination)}
                    </td>

                    <td>${esc(b.Travel_Date)}</td>

                    <td>

                        <button
                            class="btn btn-sm btn-outline-primary edit-bus"
                            data-id="${esc(b.Bus_ID)}">

                            Edit

                        </button>

                        <button
                            class="btn btn-sm btn-outline-danger delete-bus"
                            data-id="${esc(b.Bus_ID)}">

                            Delete

                        </button>

                    </td>

                </tr>

            `).join('')

        );

    } catch (e) {

        notify(e.message, true);

    }

}


// ==========================================
// 8. LOAD SAVED TRIPS
// ==========================================

async function loadSaved() {

    if (!session) {

        $('#savedList').html(
            '<p class="muted">Sign in to view saved trips.</p>'
        );

        return;
    }

    try {

        const trips = await request('/saved-trips');


        $('#savedList').html(

            trips.length

                ? trips.map(t => `

                    <div class="bus-card">

                        <div class="d-flex justify-content-between flex-wrap gap-2">

                            <div>

                                <b>
                                    ${esc(t.bus?.Departure)}
                                    →
                                    ${esc(t.bus?.Destination)}
                                </b>

                                <div class="small muted">

                                    ${esc(t.bus?.Travel_Date)}

                                    ·

                                    ${esc(t.bus?.Departure_Time)}

                                    ·

                                    ${esc(t.bus?.Bus_ID)}

                                </div>

                                <div class="mt-2">
                                    ${esc(t.note)}
                                </div>

                            </div>


                            <div>

                                <button
                                    class="btn btn-sm btn-outline-primary edit-trip"
                                    data-id="${esc(t._id)}"
                                    data-note="${esc(t.note)}">

                                    Edit note

                                </button>

                                <button
                                    class="btn btn-sm btn-outline-danger remove-trip"
                                    data-id="${esc(t._id)}">

                                    Remove

                                </button>

                            </div>

                        </div>

                    </div>

                `).join('')

                : '<p class="muted">No saved trips yet. Search and save a bus above.</p>'

        );

    } catch (e) {

        notify(e.message, true);

    }

}


// ==========================================
// 9. PAGE NAVIGATION
// ==========================================

function showPage(page) {

    $('.page').addClass('d-none');

    $('#page-' + page).removeClass('d-none');


    $('.nav-link[data-page]').removeClass('active');

    $(`.nav-link[data-page="${page}"]`).addClass('active');


    if (page === 'saved') {
        loadSaved();
    }

    if (page === 'bookings') {
        loadBookings();
    }

    if (page === 'plans') {
        loadPlans();
    }

    if (page === 'info') {
        loadInfo();
    }


    window.scrollTo(0, 0);

}


// ==========================================
// 10. SELECT BUS
// ==========================================

async function selectBus(id) {

    selectedBus = buses.find(
        b => b.Bus_ID === id
    ) || await request(
        '/buses/' + encodeURIComponent(id)
    );

    selectedSeat = null;


    $('#selectedBusName').text(
        selectedBus.Bus_Name +
        ' · ' +
        selectedBus.Departure +
        ' → ' +
        selectedBus.Destination
    );


    $('#bookingBus').text(selectedBus.Bus_ID);

    await loadSeats();

    showPage('seats');

}


// ==========================================
// 11. LOAD AVAILABLE SEATS
// ==========================================

async function loadSeats() {

    const data = await request(
        '/seats/' + encodeURIComponent(selectedBus.Bus_ID)
    );


    $('#seatGrid').html(

        Array.from(
            { length: data.total },
            (_, i) => {

                const s = 'S' + (i + 1);

                const unavailable =
                    data.booked.includes(s) ||
                    data.blocked.includes(s);


                return `

                    <button
                        class="seat ${selectedSeat === s ? 'selected' : ''}"
                        data-seat="${s}"
                        ${unavailable ? 'disabled' : ''}>

                        ${s}

                    </button>

                `;

            }

        ).join('')

    );


    $('#seatHint').text(
        'Selected: ' + (selectedSeat || 'None')
    );

}



 // ==========================================
 // 12. LOAD BOOKINGS
 // ==========================================

async function loadBookings() {

    if (!session) {
        $('#bookingList').html(
            '<p>Sign in to view bookings.</p>'
        );
        return;
    }

    try {

        const data = await request('/bookings');

        $('#bookingList').html(

            data.length

                ? data.map(b => `

                    <div class="bus-card">

                        <div class="d-flex justify-content-between flex-wrap gap-2">

                            <div>

                                <b>${esc(b.Booking_ID)}</b>

                                <span class="badge ${
                                    b.Booking_Status === 'Confirmed'
                                        ? 'bg-success'
                                        : 'bg-secondary'
                                }">
                                    ${esc(b.Booking_Status)}
                                </span>

                                <div class="small muted">
                                    ${esc(b.Passenger_Name)}
                                    · ${esc(b.Bus_ID)}
                                    · ${esc(b.Travel_Date)}
                                    · ${esc(b.Seat_Number)}
                                </div>

                                <div>
                                    ${esc(b.Phone)}
                                    · ${esc(b.Email)}
                                </div>

                                <div class="small muted">
                                    Boarding: ${esc(b.Boarding_Point)}
                                </div>

                            </div>

                            <div>

                                <button
                                    class="btn btn-sm btn-outline-primary edit-booking"
                                    data-id="${esc(b.Booking_ID)}"
                                    data-name="${esc(b.Passenger_Name)}"
                                    data-phone="${esc(b.Phone)}"
                                    data-email="${esc(b.Email)}"
                                    data-boarding="${esc(b.Boarding_Point)}"
                                    ${b.Booking_Status !== 'Confirmed' ? 'disabled' : ''}>

                                    Edit

                                </button>

                                <button
                                    class="btn btn-sm btn-outline-danger cancel-booking"
                                    data-id="${esc(b.Booking_ID)}"
                                    ${b.Booking_Status !== 'Confirmed' ? 'disabled' : ''}>

                                    Cancel

                                </button>

                            </div>

                        </div>

                    </div>

                `).join('')

                : '<p>No bookings yet.</p>'

        );

    } catch (e) {

        notify(e.message, true);

    }

}



// ==========================================
// 13. LOAD BUS INFORMATION
// ==========================================

async function loadInfo() {

    try {

        const all = await request('/buses');


        $('#infoList').html(

            all.slice(0, 18).map(b => `

                <option value="${esc(b.Bus_ID)}">

                    ${esc(b.Bus_Name)}

                    ·

                    ${esc(b.Departure)}
                    →
                    ${esc(b.Destination)}

                    (${esc(b.Bus_ID)})

                </option>

            `).join('')

        );


        loadReviews();

    } catch (e) {

        notify(e.message, true);

    }

}


// ==========================================
// 14. LOAD REVIEWS
// ==========================================

async function loadReviews() {

    const id = $('#infoList').val();

    if (!id) return;


    try {

        const data = await request(
            '/reviews/' + encodeURIComponent(id)
        );


        $('#reviewsList').html(

            data.length

                ? data.map(r => `

                    <div class="border-bottom py-3">

                        <b>
                            ${esc(r.owner?.name || 'Passenger')}
                        </b>

                        <span class="text-warning">
                            ${'★'.repeat(r.rating)}
                        </span>

                        <p class="mb-1">
                            ${esc(r.comment)}
                        </p>


                        ${
                            session &&
                            (
                                r.owner?._id === session.user.id ||
                                session.user.role === 'admin'
                            )

                            ? `

                                <button
                                    class="btn btn-sm btn-outline-danger delete-review"
                                    data-id="${esc(r._id)}">

                                    Delete

                                </button>

                            `

                            : ''

                        }

                    </div>

                `).join('')

                : '<p class="muted">No reviews yet.</p>'

        );

    } catch (e) {

        notify(e.message, true);

    }

}


// ==========================================
// 15. LOAD TRAVEL PLANS
// ==========================================

async function loadPlans() {

    if (!session) {

        $('#plansList').html(
            'Sign in to view plans.'
        );

        return;
    }


    try {

        const data = await request('/plans');


        $('#plansList').html(

            data.length

                ? data.map(p => `

                    <div class="bus-card">

                        <b>
                            ${esc(p.departure)}
                            →
                            ${esc(p.destination)}
                        </b>

                        <p class="muted">

                            ${esc(p.date)}
                            ${esc(p.time)}
                            ·
                            ${esc(p.note)}

                        </p>


                        <button
                            class="btn btn-sm btn-outline-primary edit-plan"
                            data-id="${esc(p._id)}">

                            Edit

                        </button>

                        <button
                            class="btn btn-sm btn-outline-danger delete-plan"
                            data-id="${esc(p._id)}">

                            Delete

                        </button>

                    </div>

                `).join('')

                : 'No travel plans yet.'

        );

    } catch (e) {

        notify(e.message, true);

    }

}


// ==========================================
// 16. DOCUMENT READY
// ==========================================

$(function () {

    // Stop non-admin users from using Admin page scripts.
if (isAdminPage && session?.user?.role !== 'admin') {
    return;
}

    // Initial user interface
    roleUI();


    // Set minimum travel date
    $('#travelDate, #planDate').attr(
        'min',
        new Date().toLocaleDateString('en-CA')
    );


    // Set today's date
    $('#travelDate').val(
        new Date().toLocaleDateString('en-CA')
    );


    // Load buses
if (isAdminPage) {

    // Admin must see all buses.
    loadBuses();

} else {

    // Customers see available buses only.
    loadBuses('?available=true');

}


    // ======================================
    // 17. NAVIGATION BUTTONS
    // ======================================

    $(document).on(
        'click',
        '[data-page]',
        function (e) {

            e.preventDefault();

            showPage($(this).data('page'));

        }
    );


    // ======================================
    // 18. BUS SEARCH
    // ======================================

    $('#searchForm').on('submit', async function (e) {

        e.preventDefault();


        const p = new URLSearchParams();


        [
            'Departure',
            'Destination',
            'Travel_Date',
            'time'
        ].forEach(k => {

            const v = $(`[name="${k}"]`, this).val();

            if (v) {
                p.set(k, v);
            }

        });


        p.set('available', 'true');


        await loadBuses('?' + p);


        $('#resultsSection')[0].scrollIntoView({
            behavior: 'smooth'
        });

    });


    // ======================================
    // 19. SWAP DEPARTURE AND DESTINATION
    // ======================================

    $('#swapBtn').click(() => {

        const a = $('#from').val();

        $('#from').val($('#to').val());

        $('#to').val(a);

    });


    // ======================================
    // 20. LOGIN AND REGISTER
    // ======================================

    $('#authForm').on('submit', async function (e) {

        e.preventDefault();


        const d = Object.fromEntries(
            new FormData(this)
        );


        try {

            session = await request(
                '/auth/' + $('#authMode').val(),
                'POST',
                d
            );


            
localStorage.setItem(
    'metrogo_session',
    JSON.stringify(session)
);


// ADMIN LOGIN REDIRECT
if (session?.user?.role === 'admin') {

    window.location.href = 'admin.html';

    return;

}


// CUSTOMER LOGIN
roleUI();


bootstrap.Modal.getInstance(
    $('#authModal')[0]
).hide();


notify('Welcome to MetroGo!');

loadSaved();



        } catch (e) {

            notify(e.message, true);

        }

    });


    // Show or hide registration name field
    $('#authMode').change(function () {

        $('#registerName').toggleClass(
            'd-none',
            this.value !== 'register'
        );

    });


    // ======================================
    // 21. LOGOUT
    // ======================================

    
$('#logoutBtn').click(() => {

    session = null;

    localStorage.removeItem('metrogo_session');

    // If logout happens on Admin page,
    // return to Customer website.
    if (isAdminPage) {

        window.location.href = 'index.html';

        return;

    }

    roleUI();

    showPage('home');

    notify('Signed out');

});



    // ======================================
    // 22. SAVE BUS
    // ======================================

    $(document).on('click', '.save-bus', async function () {

        if (!mustLogin()) return;


        try {

            await request(
                '/saved-trips',
                'POST',
                {
                    busId: $(this).data('id'),
                    note: ''
                }
            );


            notify('Trip saved! Find it under Saved Trips.');


        } catch (e) {

            notify(e.message, true);

        }

    });


    // ======================================
    // 23. SELECT BUS
    // ======================================

    $(document).on('click', '.choose-bus', function () {

        selectBus($(this).data('id')).catch(
            e => notify(e.message, true)
        );

    });


    // ======================================
    // 24. EDIT SAVED TRIP
    // ======================================

    $(document).on('click', '.edit-trip', async function () {

        const note = prompt(
            'Update trip note:',
            $(this).attr('data-note')
        );


        if (note === null) return;


        try {

            await request(
                '/saved-trips/' + $(this).data('id'),
                'PUT',
                { note }
            );


            loadSaved();


        } catch (e) {

            notify(e.message, true);

        }

    });


    // ======================================
    // 25. REMOVE SAVED TRIP
    // ======================================

    $(document).on('click', '.remove-trip', async function () {

        if (!confirm('Remove this saved trip?')) return;


        try {

            await request(
                '/saved-trips/' + $(this).data('id'),
                'DELETE'
            );


            loadSaved();


        } catch (e) {

            notify(e.message, true);

        }

    });


    // ======================================
    // 26. SELECT SEAT
    // ======================================

    $(document).on('click', '.seat', function () {

        selectedSeat = $(this).data('seat');


        $('.seat').removeClass('selected');

        $(this).addClass('selected');


        $('#seatHint').text(
            'Selected: ' + selectedSeat
        );

    });


    // ======================================
    // 27. CONTINUE BOOKING
    // ======================================

    $('#continueBooking').click(() => {

        if (!selectedSeat) {

            return notify(
                'Select an available seat.',
                true
            );

        }


        if (!mustLogin()) return;


        showPage('passenger');

    });

    // ======================================
// RESERVE SEAT AND SAVE TO MONGODB
// ======================================

$('#reserveSeatBtn').on('click', async function () {

    // Check bus selection
    if (!selectedBus) {
        return notify('Please select a bus first.', true);
    }

    // Check seat selection
    if (!selectedSeat) {
        return notify('Please select an available seat.', true);
    }

    // Check login
    if (!mustLogin()) return;

    try {

        // Send reservation details to backend
        const result = await request(
            '/seat-reservations',
            'POST',
            {
                busId: selectedBus.Bus_ID,
                travelDate: selectedBus.Travel_Date,
                seatNumber: selectedSeat
            }
        );

        // Display selected seat in popup
        $('#popupSeatNumber').text(selectedSeat);

        // Show success popup only after database save succeeds
        $('#reservationPopup').css('display', 'flex');

        // Refresh seat availability
        await loadSeats();

    } catch (e) {

        notify(e.message, true);

    }

});


// Close reservation popup

function hideReservationPopup() {
    $('#reservationPopup').hide();
}

$('#closePopup, #popupOkBtn').on('click', hideReservationPopup);

$('#reservationPopup').on('click', function (e) {
    if (e.target === this) {
        hideReservationPopup();
    }
});


    
    // ======================================
    // 28. PASSENGER BOOKING
    // ======================================

    $('#passengerForm').submit(async function (e) {

        e.preventDefault();

        if (!mustLogin()) return;

        if (!selectedBus || !selectedSeat) {
            notify('Please select a bus and seat first.', true);
            return;
        }

        try {

            // Get passenger details from the form.
            const d = Object.fromEntries(
                new FormData(this)
            );

            // Save booking in MongoDB.
            const booking = await request(
                '/bookings',
                'POST',
                {
                    ...d,
                    busId: selectedBus.Bus_ID,
                    seat: selectedSeat
                }
            );

            // Display saved booking information.
            $('#popupBookingID').text(booking.Booking_ID);
            $('#popupPassengerName').text(booking.Passenger_Name);
            $('#popupBusName').text(selectedBus.Bus_Name);
            $('#popupBusID').text(booking.Bus_ID);
            $('#popupDeparture').text(selectedBus.Departure);
            $('#popupDestination').text(booking.Destination);
            $('#popupTravelDate').text(booking.Travel_Date);
            $('#popupDepartureTime').text(selectedBus.Departure_Time);
            $('#popupBookingSeat').text(booking.Seat_Number);
            $('#popupPhone').text(booking.Phone);
            $('#popupEmail').text(booking.Email);

            $('#popupFare').text(
                'LKR ' + Number(booking.Fare).toLocaleString()
            );

            $('#popupBookingStatus').text(booking.Booking_Status);

            // Show popup after successful database save.
            $('#bookingSuccessPopup').css('display', 'flex');

            // Clear passenger form.
            this.reset();

            // Refresh bus and booking information.
            await loadBuses();
            await loadBookings();

        } catch (e) {

            notify(e.message, true);

        }

    });


    // ======================================
    // CLOSE BOOKING SUCCESS POPUP
    // ======================================

    function hideBookingPopup() {
        $('#bookingSuccessPopup').hide();
        showPage('bookings');
    }

    $('#closeBookingPopup, #bookingPopupOkBtn').on(
        'click',
        hideBookingPopup
    );

    $('#bookingSuccessPopup').on('click', function (e) {

        if (e.target === this) {
            hideBookingPopup();
        }

    });

    
    // ======================================
    // MY BOOKINGS EDIT AND CANCEL
    // ======================================

    let cancelBookingId = null;


    // Show a success popup.
    function showBookingMessage(title, message) {

        $('#bookingMessageTitle').text(title);

        $('#bookingMessageText').text(message);

        bootstrap.Modal
            .getOrCreateInstance(
                document.getElementById('bookingMessageModal')
            )
            .show();

    }


    // ======================================
    // EDIT BOOKING BUTTON
    // ======================================

    $(document).on('click', '.edit-booking', function () {

        const button = $(this);

        // Fill the edit form with current details.
        $('#editBookingId').val(button.attr('data-id'));

        $('#editPassengerName').val(button.attr('data-name'));

        $('#editPhone').val(button.attr('data-phone'));

        $('#editEmail').val(button.attr('data-email'));

        $('#editBoardingPoint').val(button.attr('data-boarding'));

        // Open edit popup.
        bootstrap.Modal
            .getOrCreateInstance(
                document.getElementById('bookingEditModal')
            )
            .show();

    });


    // ======================================
    // SAVE EDITED BOOKING
    // ======================================

    $('#bookingEditForm').on('submit', async function (e) {

        e.preventDefault();

        const id = $('#editBookingId').val();

        const updatedData = {

            Passenger_Name: $('#editPassengerName').val().trim(),

            Phone: $('#editPhone').val().trim(),

            Email: $('#editEmail').val().trim(),

            Boarding_Point: $('#editBoardingPoint').val().trim()

        };

        if (
            !updatedData.Passenger_Name ||
            !updatedData.Phone ||
            !/^\S+@\S+\.\S+$/.test(updatedData.Email)
        ) {

            return notify(
                'Please enter valid passenger details.',
                true
            );

        }

        try {

            // Update booking in MongoDB.
            const result = await request(
                '/bookings/' + encodeURIComponent(id),
                'PUT',
                updatedData
            );

            // Close edit popup.
            bootstrap.Modal
                .getOrCreateInstance(
                    document.getElementById('bookingEditModal')
                )
                .hide();

            // Refresh booking list.
            await loadBookings();

            // Show success message.
            showBookingMessage(
                'Booking Updated Successfully!',
                'Your booking details have been updated successfully. Thank you for choosing MetroGo!'
            );

            this.reset();

        } catch (e) {

            notify(e.message, true);

        }

    });


    // ======================================
    // CANCEL BOOKING BUTTON
    // ======================================

    $(document).on('click', '.cancel-booking', function () {

        cancelBookingId = $(this).attr('data-id');

        // Show confirmation popup.
        bootstrap.Modal
            .getOrCreateInstance(
                document.getElementById('cancelBookingModal')
            )
            .show();

    });


    // ======================================
    // CONFIRM BOOKING CANCELLATION
    // ======================================

    $('#confirmCancelBooking').on('click', async function () {

        if (!cancelBookingId) return;

        const id = cancelBookingId;

        try {

            // Cancel booking in MongoDB.
            await request(
                '/bookings/' + encodeURIComponent(id),
                'DELETE'
            );

            // Close confirmation popup.
            bootstrap.Modal
                .getOrCreateInstance(
                    document.getElementById('cancelBookingModal')
                )
                .hide();

            cancelBookingId = null;

            // Refresh booking and bus information.
            await loadBookings();

            await loadBuses();

            // Show cancellation success popup.
            showBookingMessage(
                'Booking Cancelled!',
                'Your booking has been cancelled successfully. Thank you for choosing MetroGo!'
            );

        } catch (e) {

            notify(e.message, true);

        }

    });


    // ======================================
    // 31. LOAD REVIEWS WHEN BUS CHANGES
    // ======================================

    $('#infoList').change(loadReviews);


    // ======================================
    // 32. SUBMIT REVIEW
    // ======================================

    $('#reviewForm').submit(async function (e) {

        e.preventDefault();


        if (!mustLogin()) return;


        try {

            await request(
                '/reviews',
                'POST',
                {
                    busId: $('#infoList').val(),
                    rating: Number($('#rating').val()),
                    comment: $('#reviewComment').val()
                }
            );


            $('#reviewComment').val('');

            loadReviews();

            notify('Review submitted');


        } catch (e) {

            notify(e.message, true);

        }

    });


    // ======================================
    // 33. DELETE REVIEW
    // ======================================

    $(document).on('click', '.delete-review', async function () {

        if (!confirm('Delete review?')) return;


        try {

            await request(
                '/reviews/' + $(this).data('id'),
                'DELETE'
            );


            loadReviews();


        } catch (e) {

            notify(e.message, true);

        }

    });


    // ======================================
    // 34. ADD TRAVEL PLAN
    // ======================================

    $('#planForm').submit(async function (e) {

        e.preventDefault();


        if (!mustLogin()) return;


        const d = Object.fromEntries(
            new FormData(this)
        );


        try {

            await request(
                '/plans',
                'POST',
                d
            );


            this.reset();

            loadPlans();

            notify('Travel plan saved');


        } catch (e) {

            notify(e.message, true);

        }

    });


    // ======================================
    // 35. DELETE TRAVEL PLAN
    // ======================================

    $(document).on('click', '.delete-plan', async function () {

        if (!confirm('Delete plan?')) return;


        try {

            await request(
                '/plans/' + $(this).data('id'),
                'DELETE'
            );


            loadPlans();


        } catch (e) {

            notify(e.message, true);

        }

    });


    // ======================================
    // 36. EDIT TRAVEL PLAN
    // ======================================

    $(document).on('click', '.edit-plan', async function () {

        const note = prompt('New plan note:');

        if (note === null) return;


        try {

            await request(
                '/plans/' + $(this).data('id'),
                'PUT',
                { note }
            );


            loadPlans();


        } catch (e) {

            notify(e.message, true);

        }

    });


    // ======================================
    // 37. ADD OR UPDATE BUS (ADMIN)
    // ======================================

    $('#busForm').submit(async function (e) {

        e.preventDefault();


        if (session?.user.role !== 'admin') return;


        const d = Object.fromEntries(
            new FormData(this)
        );


        d.Total_Seats = Number(d.Total_Seats);

        d.Available_Seats = Number(d.Available_Seats);

        d.Fare = Number(d.Fare);


        try {

            const old = $('#busEditId').val();


            await request(

                '/buses' +
                (
                    old
                        ? '/' + encodeURIComponent(old)
                        : ''
                ),

                old ? 'PUT' : 'POST',

                d

            );


            this.reset();

            $('#busEditId').val('');

            $('#busFormTitle').text('Add new bus');


            loadBuses();

            notify('Bus saved');


        } catch (e) {

            notify(e.message, true);

        }

    });


    // ======================================
    // 38. EDIT BUS (ADMIN)
    // ======================================

    $(document).on('click', '.edit-bus', async function () {

        try {

            const b = await request(
                '/buses/' + encodeURIComponent(
                    $(this).data('id')
                )
            );


            $('#busEditId').val(b.Bus_ID);


            Object.entries(b).forEach(([k, v]) => {

                $('#busForm [name="' + k + '"]').val(v);

            });


            $('#busFormTitle').text(
                'Edit ' + b.Bus_ID
            );


            $('#adminPanel')[0].scrollIntoView({
                behavior: 'smooth'
            });


        } catch (e) {

            notify(e.message, true);

        }

    });


    // ======================================
    // 39. DELETE BUS (ADMIN)
    // ======================================

    $(document).on('click', '.delete-bus', async function () {

        if (!confirm('Delete bus?')) return;


        try {

            await request(
                '/buses/' + encodeURIComponent(
                    $(this).data('id')
                ),
                'DELETE'
            );


            loadBuses();

            notify('Bus deleted');


        } catch (e) {

            notify(e.message, true);

        }

    });

});

