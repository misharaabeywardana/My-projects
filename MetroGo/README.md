# MetroGo — Integrated University Group Project

MetroGo combines the six member pages into one website and one shared Express/MongoDB backend.

## Pages

1. **Home** — search buses, view available services and select a bus.
2. **Seat Reservation** — view seat availability and choose a seat.
3. **Passenger Booking** — enter passenger details and confirm a booking.
4. **My Booking** — view, update and cancel your bookings.
5. **Bus Information** — browse bus information and submit/read customer reviews.
6. **Routes Management** — admin bus/route record management (add, edit and delete) plus customer travel plans.

Saved Trips remains available as a shortcut from the Home page. The repeated standalone seat-reservation module and repeated page copies were not included; the integrated versions are used instead.

## Requirements

- Node.js 18+
- MongoDB Community Server running locally, or a MongoDB Atlas database
- Internet connection for Bootstrap/jQuery CDN assets

## Setup (Windows / VS Code)

1. Extract the ZIP.
2. Open the extracted `MetroGo_Professional` folder in VS Code.
3. Open Terminal and run:

   ```bash
   cd backend
   npm install
   ```

4. In the `backend` folder, copy `.env.example` and rename the copy to `.env`.
5. Open `.env` and set a private JWT secret and (optionally) the MongoDB URI:

   ```env
   PORT=5000
   MONGO_URI=mongodb://127.0.0.1:27017/metrogo
   JWT_SECRET=replace_with_a_long_random_secret
   SEED_ADMIN_PASSWORD=ChooseYourOwnPassword123!
   ```

   Use your own strong admin password. Do not share or upload your `.env` file.

6. Make sure MongoDB is running. In the same `backend` terminal, run this once:

   ```bash
   npm run seed
   ```

   This adds sample bus services and creates the admin user if it does not already exist.

7. Start the website:

   ```bash
   npm start
   ```

8. Open **http://localhost:5000** in Chrome.

Do not open `frontend/index.html` directly with Live Server. The website uses the Express API, so start the backend and open the localhost URL.

## Admin account

- Email: `admin@metrogo.lk`
- Password: the value you entered for `SEED_ADMIN_PASSWORD` in `.env`

The seed script does not reset an existing admin password. If the admin already exists, use the password that was set when that account was first created.

## Notes about integration

- All six page views are served by the same frontend and use the same `/api` backend, rather than running six servers on different ports.
- Authentication/session handling is shared across the integrated pages.
- Route Management reuses the shared bus records and the admin-only bus CRUD API, avoiding a second conflicting route database and duplicate login system.
- Demo buses are sample assignment data, not verified real-world schedules.
- This is a university project prototype, not a production transport or payment platform.
