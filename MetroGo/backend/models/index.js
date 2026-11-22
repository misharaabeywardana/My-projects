
const mongoose = require('mongoose');
const opts = { timestamps: true };

const User = mongoose.model('User', new mongoose.Schema({
  name: String,
  email: { type: String, unique: true, lowercase: true },
  passwordHash: String,
  role: { type: String, enum: ['customer', 'admin'], default: 'customer' }
}, opts));

const Bus = mongoose.model('Bus', new mongoose.Schema({
  Bus_ID: { type: String, unique: true, required: true },
  Bus_Name: { type: String, required: true },
  Route: String,
  Departure: { type: String, required: true },
  Destination: { type: String, required: true },
  Travel_Date: { type: String, required: true },
  Departure_Time: { type: String, required: true },
  Arrival_Time: String,
  Total_Seats: { type: Number, min: 1, required: true },
  Available_Seats: { type: Number, min: 0, required: true },
  Fare: { type: Number, min: 0, required: true }
}, opts));

const SavedTrip = mongoose.model('SavedTrip', new mongoose.Schema({
  owner: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  bus: { type: mongoose.Schema.Types.ObjectId, ref: 'Bus', required: true },
  note: { type: String, maxlength: 250, default: '' }
}, opts));

const Booking = mongoose.model('Booking', new mongoose.Schema({
  Booking_ID: { type: String, unique: true },
  owner: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  Passenger_Name: String,
  Phone: String,
  Email: String,
  Bus_ID: String,
  Travel_Date: String,
  Seat_Number: String,
  Boarding_Point: String,
  Destination: String,
  Fare: Number,
  Booking_Status: { type: String, enum: ['Confirmed', 'Cancelled'], default: 'Confirmed' }
}, opts));

const Review = mongoose.model('Review', new mongoose.Schema({
  owner: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
  bus: { type: mongoose.Schema.Types.ObjectId, ref: 'Bus' },
  rating: { type: Number, min: 1, max: 5 },
  comment: { type: String, maxlength: 500 }
}, opts));

const Plan = mongoose.model('Plan', new mongoose.Schema({
  owner: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
  departure: String,
  destination: String,
  date: String,
  time: String,
  note: String
}, opts));

const Block = mongoose.model('Block', new mongoose.Schema({
  bus: { type: mongoose.Schema.Types.ObjectId, ref: 'Bus' },
  seat: String
}, opts));

const SeatReservation = mongoose.model('SeatReservation', new mongoose.Schema({
  owner: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  busId: {
    type: String,
    required: true
  },
  travelDate: {
    type: String,
    required: true
  },
  seatNumber: {
    type: String,
    required: true
  }
}, opts));

SeatReservation.schema.index(
  { busId: 1, travelDate: 1, seatNumber: 1 },
  { unique: true }
);

// ==========================================
// BOOKING EDIT / CANCEL HISTORY MODEL
// ==========================================

const Schedule = mongoose.model('Schedule', new mongoose.Schema({
  Booking_ID: {
    type: String,
    required: true
  },

  owner: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },

  Bus_ID: String,

  action: {
    type: String,
    enum: ['Edited', 'Cancelled'],
    required: true
  },

  oldDetails: {
    type: mongoose.Schema.Types.Mixed
  },

  newDetails: {
    type: mongoose.Schema.Types.Mixed
  }

}, opts));

module.exports = {
  User,
  Bus,
  SavedTrip,
  Booking,
  Review,
  Plan,
  Block,
  SeatReservation,
  Schedule
};