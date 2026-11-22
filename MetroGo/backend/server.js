
require('dotenv').config();

const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const path = require('path');

// Import database models.
const {
  User,
  Bus,
  SavedTrip,
  Booking,
  Review,
  Plan,
  Block,
  SeatReservation,
  Schedule
} = require('./models');

// Import authentication middleware.
const { auth, admin } = require('./middleware/auth');

// Create Express application.
const app = express();

// Middleware configuration.
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, '../frontend')));

// Helper function to handle asynchronous errors.
const wrap = (f) => (req, res, next) =>
  Promise.resolve(f(req, res, next)).catch(next);

// Select only the allowed fields from an object.
const pick = (o, keys) =>
  Object.fromEntries(
    keys
      .filter((k) => o[k] !== undefined)
      .map((k) => [k, o[k]])
  );

// Fields accepted for bus records.
const busKeys = [
  'Bus_ID',
  'Bus_Name',
  'Route',
  'Departure',
  'Destination',
  'Travel_Date',
  'Departure_Time',
  'Arrival_Time',
  'Total_Seats',
  'Available_Seats',
  'Fare'
];

// Validate bus information.
const validBus = (b) =>
  b.Bus_ID &&
  b.Bus_Name &&
  b.Departure &&
  b.Destination &&
  /^\d{4}-\d\d-\d\d$/.test(b.Travel_Date || '') &&
  /^\d\d:\d\d$/.test(b.Departure_Time || '') &&
  Number.isInteger(Number(b.Total_Seats)) &&
  Number(b.Total_Seats) > 0 &&
  Number(b.Available_Seats) >= 0 &&
  Number(b.Available_Seats) <= Number(b.Total_Seats) &&
  Number(b.Fare) >= 0;


// ==================================================
// USER REGISTRATION
// ==================================================

app.post('/api/auth/register', wrap(async (req, res) => {
  const { name, email, password } = req.body;

  // Validate registration details.
  if (
    !name?.trim() ||
    !/^\S+@\S+\.\S+$/.test(email || '') ||
    !password ||
    password.length < 8
  ) {
    return res.status(400).json({
      error: 'Name, valid email and password (8+ characters) required'
    });
  }

  // Check whether the email is already registered.
  if (await User.exists({ email: email.toLowerCase() })) {
    return res.status(409).json({
      error: 'Email already registered'
    });
  }

  // Hash the password before saving.
  const u = await User.create({
    name: name.trim(),
    email: email.toLowerCase(),
    passwordHash: await bcrypt.hash(password, 10)
  });

  // Generate an authentication token.
  res.status(201).json({
    token: jwt.sign(
      { id: u.id, role: u.role },
      process.env.JWT_SECRET,
      { expiresIn: '12h' }
    ),
    user: {
      name: u.name,
      email: u.email,
      role: u.role
    }
  });
}));


// ==================================================
// USER LOGIN
// ==================================================

app.post('/api/auth/login', wrap(async (req, res) => {
  // Find the user by email.
  const u = await User.findOne({
    email: String(req.body.email || '').toLowerCase()
  });

  // Validate the password.
  if (
    !u ||
    !await bcrypt.compare(req.body.password || '', u.passwordHash)
  ) {
    return res.status(401).json({
      error: 'Invalid email or password'
    });
  }

  // Generate a token after successful login.
  res.json({
    token: jwt.sign(
      { id: u.id, role: u.role },
      process.env.JWT_SECRET,
      { expiresIn: '12h' }
    ),
    user: {
      name: u.name,
      email: u.email,
      role: u.role
    }
  });
}));


// ==================================================
// GET AND SEARCH BUSES
// ==================================================

app.get('/api/buses', wrap(async (req, res) => {
  let q = {};

  // Apply the available search filters.
  for (const k of [
    'Bus_ID',
    'Departure',
    'Destination',
    'Travel_Date'
  ]) {
    if (req.query[k]) {
      q[k] = k === 'Travel_Date'
        ? req.query[k]
        : {
            $regex: String(req.query[k]).replace(
              /[.*+?^${}()|[\]\\]/g,
              '\\$&'
            ),
            $options: 'i'
          };
    }
  }

  // Filter by departure time when provided.
  if (req.query.time) {
    q.Departure_Time = { $gte: req.query.time };
  }

  // Return only buses with available seats when requested.
  if (req.query.available === 'true') {
    q.Available_Seats = { $gt: 0 };
  }

  // Retrieve and sort matching buses.
  res.json(
    await Bus.find(q)
      .sort({ Travel_Date: 1, Departure_Time: 1 })
      .limit(200)
  );
}));


// ==================================================
// GET ONE BUS
// ==================================================

app.get('/api/buses/:id', wrap(async (req, res) => {
  const b = await Bus.findOne({
    Bus_ID: req.params.id
  });

  if (!b) {
    return res.status(404).json({
      error: 'Bus not found'
    });
  }

  res.json(b);
}));


// ==================================================
// ADD A BUS - ADMIN ONLY
// ==================================================

app.post('/api/buses', auth, admin, wrap(async (req, res) => {
  // Accept only permitted bus fields.
  const data = pick(req.body, busKeys);

  // Validate the submitted bus information.
  if (!validBus(data)) {
    return res.status(400).json({
      error: 'Invalid bus details'
    });
  }

  // Create the bus record.
  res.status(201).json(await Bus.create(data));
}));


// ==================================================
// EDIT A BUS - ADMIN ONLY
// ==================================================

app.put('/api/buses/:id', auth, admin, wrap(async (req, res) => {
  // Find the existing bus.
  const old = await Bus.findOne({
    Bus_ID: req.params.id
  });

  if (!old) {
    return res.status(404).json({
      error: 'Bus not found'
    });
  }

  // Combine existing data with submitted changes.
  const merged = {
    ...old.toObject(),
    ...pick(req.body, busKeys)
  };

  // Validate the updated bus information.
  if (!validBus(merged)) {
    return res.status(400).json({
      error: 'Invalid bus details'
    });
  }

  // Apply the changes to the existing record.
  Object.assign(old, pick(req.body, busKeys));

  // Save the updated bus in MongoDB.
  await old.save();

  res.json(old);
}));


// ==================================================
// DELETE A BUS - ADMIN ONLY
// ==================================================

app.delete('/api/buses/:id', auth, admin, wrap(async (req, res) => {
  // Prevent deletion when a confirmed booking exists.
  if (await Booking.exists({
    Bus_ID: req.params.id,
    Booking_Status: 'Confirmed'
  })) {
    return res.status(409).json({
      error: 'Cannot delete bus with active bookings'
    });
  }

  // Find and delete the bus.
  const b = await Bus.findOneAndDelete({
    Bus_ID: req.params.id
  });

  if (!b) {
    return res.status(404).json({
      error: 'Bus not found'
    });
  }

  // Remove saved trips associated with this bus.
  await SavedTrip.deleteMany({
    bus: b._id
  });

  res.json({
    message: 'Bus deleted'
  });
}));


// ==================================================
// GET SAVED TRIPS
// ==================================================

app.get('/api/saved-trips', auth, wrap(async (req, res) => {
  res.json(
    await SavedTrip.find({
      owner: req.user.id
    })
      .populate('bus')
      .sort({ createdAt: -1 })
  );
}));


// ==================================================
// SAVE A TRIP
// ==================================================

app.post('/api/saved-trips', auth, wrap(async (req, res) => {
  // Find the selected bus.
  const bus = await Bus.findById(req.body.busId);

  if (!bus) {
    return res.status(404).json({
      error: 'Bus not found'
    });
  }

  // Prevent saving the same trip twice.
  if (await SavedTrip.exists({
    owner: req.user.id,
    bus: bus.id
  })) {
    return res.status(409).json({
      error: 'Trip already saved'
    });
  }

  // Save the trip for the logged-in user.
  res.status(201).json(
    await SavedTrip.create({
      owner: req.user.id,
      bus: bus.id,
      note: String(req.body.note || '').slice(0, 250)
    })
  );
}));


// ==================================================
// EDIT A SAVED TRIP NOTE
// ==================================================

app.put('/api/saved-trips/:id', auth, wrap(async (req, res) => {
  const t = await SavedTrip.findOneAndUpdate(
    {
      _id: req.params.id,
      owner: req.user.id
    },
    {
      note: String(req.body.note || '').slice(0, 250)
    },
    {
      new: true,
      runValidators: true
    }
  );

  if (!t) {
    return res.status(404).json({
      error: 'Trip not found'
    });
  }

  res.json(t);
}));


// ==================================================
// DELETE A SAVED TRIP
// ==================================================

app.delete('/api/saved-trips/:id', auth, wrap(async (req, res) => {
  const t = await SavedTrip.findOneAndDelete({
    _id: req.params.id,
    owner: req.user.id
  });

  if (!t) {
    return res.status(404).json({
      error: 'Trip not found'
    });
  }

  res.json({
    message: 'Removed'
  });
}));

// ==================================================
// CREATE A SEAT RESERVATION
// ==================================================

app.post('/api/seat-reservations', auth, wrap(async (req, res) => {

  const { busId, travelDate, seatNumber } = req.body;

  // Find the selected bus.
  const bus = await Bus.findOne({
    Bus_ID: busId,
    Travel_Date: travelDate
  });

  if (!bus) {
    return res.status(404).json({
      error: 'Bus not found'
    });
  }

  // Validate the selected seat.
  const seat = String(seatNumber || '').toUpperCase();

  if (
    !/^S\d+$/.test(seat) ||
    Number(seat.slice(1)) > bus.Total_Seats
  ) {
    return res.status(400).json({
      error: 'Invalid seat number'
    });
  }

  // Check whether the seat is already booked or blocked.
  if (
  await Block.exists({ bus: bus.id, seat }) ||
  await Booking.exists({
    Bus_ID: bus.Bus_ID,
    Travel_Date: bus.Travel_Date,
    Seat_Number: seat,
    Booking_Status: 'Confirmed'
  }) ||
  await SeatReservation.exists({
    busId: bus.Bus_ID,
    travelDate: bus.Travel_Date,
    seatNumber: seat
  })
) {
  return res.status(409).json({
    error: 'This seat is not available'
  });
} 

  // Save the reservation in MongoDB.
  const reservation = await SeatReservation.create({
    owner: req.user.id,
    busId: bus.Bus_ID,
    travelDate: bus.Travel_Date,
    seatNumber: seat
  });

  res.status(201).json({
    message: 'Seat reserved successfully',
    reservation
  });

}));

// ==================================================
// GET BUS SEAT INFORMATION
// ==================================================

app.get('/api/seats/:id', wrap(async (req, res) => {
  const bus = await Bus.findOne({
    Bus_ID: req.params.id
  });

  if (!bus) {
    return res.status(404).json({
      error: 'Bus not found'
    });
  }

  // Get confirmed bookings for this bus and travel date.
  const bookings = await Booking.find({
    Bus_ID: bus.Bus_ID,
    Travel_Date: bus.Travel_Date,
    Booking_Status: 'Confirmed'
  }).select('Seat_Number');

  // Get blocked seats.
  const blocked = await Block.find({
    bus: bus.id
  });

  // Get reserved seats for this bus and travel date.
  const reservations = await SeatReservation.find({
    busId: bus.Bus_ID,
    travelDate: bus.Travel_Date
  }).select('seatNumber');

  res.json({
    total: bus.Total_Seats,
    booked: [
      ...bookings.map((x) => x.Seat_Number),
      ...reservations.map((x) => x.seatNumber)
    ],
    blocked: blocked.map((x) => x.seat)
  });
}));


// ==================================================
// BLOCK OR UNBLOCK A SEAT - ADMIN ONLY
// ==================================================

app.post('/api/seats/block', auth, admin, wrap(async (req, res) => {
  const bus = await Bus.findOne({
    Bus_ID: req.body.busId
  });

  if (!bus) {
    return res.status(404).json({
      error: 'Bus not found'
    });
  }

  const seat = String(req.body.seat || '').toUpperCase();

  // Validate the seat number.
  if (
    !/^S\d+$/.test(seat) ||
    Number(seat.slice(1)) > bus.Total_Seats
  ) {
    return res.status(400).json({
      error: 'Invalid seat'
    });
  }

  // A confirmed booking cannot be blocked.
  if (await Booking.exists({
    Bus_ID: bus.Bus_ID,
    Seat_Number: seat,
    Booking_Status: 'Confirmed'
  })) {
    return res.status(409).json({
      error: 'Seat booked'
    });
  }

  // Add a block or remove an existing block.
  if (req.body.block) {
    await Block.updateOne(
      { bus: bus.id, seat },
      { $setOnInsert: { bus: bus.id, seat } },
      { upsert: true }
    );
  } else {
    await Block.deleteOne({
      bus: bus.id,
      seat
    });
  }

  res.json({
    message: 'Updated'
  });
}));


// ==================================================
// GET AND SEARCH BOOKINGS
// ==================================================

app.get('/api/bookings', auth, wrap(async (req, res) => {
  // Admin can view all bookings; users see their own.
  const q = req.user.role === 'admin'
    ? {}
    : { owner: req.user.id };

  if (req.query.id) {
    q.Booking_ID = req.query.id;
  }

  if (req.query.phone && req.user.role === 'admin') {
    q.Phone = req.query.phone;
  }

  if (req.query.email && req.user.role === 'admin') {
    q.Email = req.query.email;
  }

  if (req.query.name && req.user.role === 'admin') {
    q.Passenger_Name = {
      $regex: req.query.name,
      $options: 'i'
    };
  }

  if (req.query.busId && req.user.role === 'admin') {
    q.Bus_ID = req.query.busId;
  }

  if (req.query.date && req.user.role === 'admin') {
    q.Travel_Date = req.query.date;
  }

  res.json(
    await Booking.find(q)
      .sort({ createdAt: -1 })
      .limit(200)
  );
}));


// ==================================================
// CREATE A BOOKING
// ==================================================

app.post('/api/bookings', auth, wrap(async (req, res) => {
  const {
    busId,
    seat,
    name,
    phone,
    email,
    boarding
  } = req.body;

  const bus = await Bus.findOne({
    Bus_ID: busId
  });

  if (!bus) {
    return res.status(404).json({
      error: 'Bus not found'
    });
  }

  const s = String(seat || '').toUpperCase();

  // Validate passenger details and the selected seat.
  if (
    !/^S\d+$/.test(s) ||
    Number(s.slice(1)) > bus.Total_Seats ||
    !name?.trim() ||
    !phone?.trim() ||
    !/^\S+@\S+\.\S+$/.test(email || '')
  ) {
    return res.status(400).json({
      error: 'Valid seat and passenger details required'
    });
  }

  // Check whether the seat is blocked or already booked.
  if (
    await Block.exists({ bus: bus.id, seat: s }) ||
    await Booking.exists({
      Bus_ID: busId,
      Seat_Number: s,
      Booking_Status: 'Confirmed'
    })
  ) {
    return res.status(409).json({
      error: 'Seat unavailable'
    });
  }

  // Decrease the available seat count.
  const updated = await Bus.findOneAndUpdate(
    {
      _id: bus.id,
      Available_Seats: { $gt: 0 }
    },
    {
      $inc: { Available_Seats: -1 }
    },
    {
      new: true
    }
  );

  if (!updated) {
    return res.status(409).json({
      error: 'Bus full'
    });
  }

  try {
    // Create the booking record.
    const booking = await Booking.create({
      Booking_ID: 'BK' + Date.now() + Math.floor(Math.random() * 10000),
      owner: req.user.id,
      Passenger_Name: name.trim(),
      Phone: phone.trim(),
      Email: email.trim(),
      Bus_ID: busId,
      Travel_Date: bus.Travel_Date,
      Seat_Number: s,
      Boarding_Point: boarding || bus.Departure,
      Destination: bus.Destination,
      Fare: bus.Fare
    });

    res.status(201).json(booking);
  } catch (e) {
    // Restore the available seat count if booking creation fails.
    await Bus.updateOne(
      { _id: bus.id },
      { $inc: { Available_Seats: 1 } }
    );

    throw e;
  }
}));


// ==================================================
// EDIT BOOKING DETAILS
// ==================================================


app.put('/api/bookings/:id', auth, wrap(async (req, res) => {

  const q = {
    Booking_ID: req.params.id
  };

  if (req.user.role !== 'admin') {
    q.owner = req.user.id;
  }

  const b = await Booking.findOne(q);

  if (!b) {
    return res.status(404).json({
      error: 'Booking not found'
    });
  }

  if (b.Booking_Status === 'Cancelled') {
    return res.status(409).json({
      error: 'Booking cancelled'
    });
  }

  const allowedFields = [
    'Passenger_Name',
    'Phone',
    'Email',
    'Boarding_Point'
  ];

  const changes = pick(req.body, allowedFields);

  const updatedDetails = {
    ...b.toObject(),
    ...changes
  };

  if (
    !updatedDetails.Passenger_Name?.trim() ||
    !updatedDetails.Phone?.trim() ||
    !/^\S+@\S+\.\S+$/.test(updatedDetails.Email || '')
  ) {
    return res.status(400).json({
      error: 'Valid name, phone and email are required'
    });
  }

  const oldDetails = pick(b.toObject(), allowedFields);

  Object.assign(b, changes);

  await b.save();

  const newDetails = pick(b.toObject(), allowedFields);

  // Save edit history in schedules collection.
  await Schedule.create({
    Booking_ID: b.Booking_ID,
    owner: b.owner,
    Bus_ID: b.Bus_ID,
    action: 'Edited',
    oldDetails,
    newDetails
  });

  res.json({
    message: 'Booking updated successfully',
    booking: b
  });

}));


// ==================================================
// CANCEL A BOOKING
// ==================================================


app.delete('/api/bookings/:id', auth, wrap(async (req, res) => {

  const q = {
    Booking_ID: req.params.id
  };

  if (req.user.role !== 'admin') {
    q.owner = req.user.id;
  }

  const b = await Booking.findOne(q);

  if (!b) {
    return res.status(404).json({
      error: 'Booking not found'
    });
  }

  if (b.Booking_Status === 'Cancelled') {
    return res.status(409).json({
      error: 'Already cancelled'
    });
  }

  const oldDetails = {
    Passenger_Name: b.Passenger_Name,
    Phone: b.Phone,
    Email: b.Email,
    Bus_ID: b.Bus_ID,
    Travel_Date: b.Travel_Date,
    Seat_Number: b.Seat_Number,
    Boarding_Point: b.Boarding_Point,
    Destination: b.Destination,
    Fare: b.Fare,
    Booking_Status: b.Booking_Status
  };

  // Change booking status.
  b.Booking_Status = 'Cancelled';

  await b.save();

  const newDetails = {
    ...oldDetails,
    Booking_Status: 'Cancelled'
  };

  // Save cancellation history in schedules collection.
  await Schedule.create({
    Booking_ID: b.Booking_ID,
    owner: b.owner,
    Bus_ID: b.Bus_ID,
    action: 'Cancelled',
    oldDetails,
    newDetails
  });

  // Return seat to available seats.
  await Bus.updateOne(
    {
      Bus_ID: b.Bus_ID
    },
    {
      $inc: {
        Available_Seats: 1
      }
    }
  );

  res.json({
    message: 'Booking cancelled successfully',
    booking: b
  });

}));


// ==================================================
// GET REVIEWS FOR A BUS
// ==================================================

app.get('/api/reviews/:busId', wrap(async (req, res) => {
  const b = await Bus.findOne({
    Bus_ID: req.params.busId
  });

  res.json(
    b
      ? await Review.find({ bus: b.id }).populate('owner', 'name')
      : []
  );
}));


// ==================================================
// CREATE A REVIEW
// ==================================================

app.post('/api/reviews', auth, wrap(async (req, res) => {
  const bus = await Bus.findOne({
    Bus_ID: req.body.busId
  });

  if (
    !bus ||
    !(Number(req.body.rating) >= 1 && Number(req.body.rating) <= 5)
  ) {
    return res.status(400).json({
      error: 'Valid bus and rating required'
    });
  }

  res.status(201).json(
    await Review.create({
      owner: req.user.id,
      bus: bus.id,
      rating: Number(req.body.rating),
      comment: String(req.body.comment || '').slice(0, 500)
    })
  );
}));


// ==================================================
// EDIT A REVIEW
// ==================================================

app.put('/api/reviews/:id', auth, wrap(async (req, res) => {
  const q = {
    _id: req.params.id
  };

  // Users can edit their own reviews; admins can edit any.
  if (req.user.role !== 'admin') {
    q.owner = req.user.id;
  }

  const r = await Review.findOneAndUpdate(
    q,
    pick(req.body, ['rating', 'comment']),
    {
      new: true,
      runValidators: true
    }
  );

  if (!r) {
    return res.status(404).json({
      error: 'Review not found'
    });
  }

  res.json(r);
}));


// ==================================================
// DELETE A REVIEW
// ==================================================

app.delete('/api/reviews/:id', auth, wrap(async (req, res) => {
  const q = {
    _id: req.params.id
  };

  // Users can delete their own reviews; admins can delete any.
  if (req.user.role !== 'admin') {
    q.owner = req.user.id;
  }

  const r = await Review.findOneAndDelete(q);

  if (!r) {
    return res.status(404).json({
      error: 'Review not found'
    });
  }

  res.json({
    message: 'Deleted'
  });
}));


// ==================================================
// GET TRAVEL PLANS
// ==================================================

app.get('/api/plans', auth, wrap(async (req, res) => {
  res.json(
    await Plan.find({
      owner: req.user.id
    }).sort({ createdAt: -1 })
  );
}));


// ==================================================
// CREATE A TRAVEL PLAN
// ==================================================

app.post('/api/plans', auth, wrap(async (req, res) => {
  if (
    !req.body.departure ||
    !req.body.destination ||
    !req.body.date
  ) {
    return res.status(400).json({
      error: 'Route and date required'
    });
  }

  res.status(201).json(
    await Plan.create({
      ...pick(req.body, [
        'departure',
        'destination',
        'date',
        'time',
        'note'
      ]),
      owner: req.user.id
    })
  );
}));


// ==================================================
// EDIT A TRAVEL PLAN
// ==================================================

app.put('/api/plans/:id', auth, wrap(async (req, res) => {
  const p = await Plan.findOneAndUpdate(
    {
      _id: req.params.id,
      owner: req.user.id
    },
    pick(req.body, [
      'departure',
      'destination',
      'date',
      'time',
      'note'
    ]),
    {
      new: true,
      runValidators: true
    }
  );

  if (!p) {
    return res.status(404).json({
      error: 'Plan not found'
    });
  }

  res.json(p);
}));


// ==================================================
// DELETE A TRAVEL PLAN
// ==================================================

app.delete('/api/plans/:id', auth, wrap(async (req, res) => {
  const p = await Plan.findOneAndDelete({
    _id: req.params.id,
    owner: req.user.id
  });

  if (!p) {
    return res.status(404).json({
      error: 'Plan not found'
    });
  }

  res.json({
    message: 'Deleted'
  });
}));


// ==================================================
// CHECK SERVER AND DATABASE STATUS
// ==================================================

app.get('/api/health', (_req, res) => {
  res.json({
    status: 'ok',
    database: mongoose.connection.readyState === 1
      ? 'connected'
      : 'disconnected'
  });
});


// ==================================================
// GLOBAL ERROR HANDLER
// ==================================================

app.use((err, req, res, next) => {
  console.error(err);

  res.status(
    err.code === 11000
      ? 409
      : err.name === 'CastError'
        ? 400
        : 500
  ).json({
    error: err.code === 11000
      ? 'Record already exists'
      : err.name === 'CastError'
        ? 'Invalid identifier'
        : 'Request failed'
  });
});


// ==================================================
// CONNECT TO MONGODB AND START SERVER
// ==================================================

const port = process.env.PORT || 5000;

// JWT_SECRET must be configured in the .env file.
if (!process.env.JWT_SECRET) {
  throw Error('Set JWT_SECRET in .env');
}

// Connect to MongoDB before starting the server.
mongoose.connect(
  process.env.MONGO_URI || 'mongodb://127.0.0.1:27017/metrogo'
)
  .then(() => {
    app.listen(port, () => {
      console.log('MetroGo running at http://localhost:' + port);
    });
  })
  .catch((e) => {
    console.error('MongoDB connection failed:', e.message);
    process.exit(1);
  });