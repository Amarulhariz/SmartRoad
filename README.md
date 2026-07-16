# SmartRoad

Crowdsourced road hazard reporting and monitoring system — an Android app paired with a web admin panel, built for ICT602 Mobile Technology.

SmartRoad lets users report road hazards (potholes, flooding, fallen trees, accidents, damaged signs, broken traffic lights) in real time, complete with GPS location, photo evidence, and automatic timestamps. Reports appear live on a map for other users, and administrators manage the full hazard lifecycle through a dedicated web dashboard.

## Features

### Mobile app
- Email/password login, with users greeted by their real name
- Live GPS location shown on a Google Map
- Report a hazard: type, description, photo, auto-captured GPS + date/time
- Hazard map with color-coded status markers (New / Under Investigation / Resolved) and custom icons per hazard type
- Filter markers by hazard type
- Hazard details view with photo, reporter, and full report info
- Profile screen with editable name, avatar, and a "My Reports" history
- About page with app info and developer credits

### Web admin panel
- Login-protected dashboard with live report/user counts
- Manage reports: search and filter by type, status, and date
- Report details: view photo, update status, delete report
- Photo thumbnails in the reports table

## Tech stack

- **Mobile:** Android (Java), Google Maps SDK, FusedLocationProviderClient, Glide
- **Web:** HTML, JavaScript (Firebase Web SDK, ES modules), CSS
- **Backend:** Firebase Authentication, Cloud Firestore, Firebase Storage

## Project structure

```
SmartRoad-Project/
  mobile/     Android Studio project
  Web/        Admin panel (static HTML/JS/CSS)
```

## Setup

### Mobile
1. Open `mobile/` in Android Studio.
2. Add your own `google-services.json` to `mobile/app/` if setting up a fresh Firebase project.
3. Add a Google Maps API key in `local.properties` / `strings.xml` as configured in the project.
4. Run on an emulator or device (API level per `build.gradle.kts`).

### Web admin
1. Update `Web/js/firebase-config.js` with your Firebase project config.
2. Serve the folder locally, e.g.:
   ```
   cd Web
   python -m http.server 8000
   ```
3. Open `http://localhost:8000`.
4. Create an admin user via Firebase Console → Authentication, then mark them as admin by setting `isAdmin: true` on their `users/{uid}` document in Firestore.

## Security

Firestore and Storage rules restrict report creation to authenticated users and limit status updates/deletion to admin accounts. See `Web/firestore.rules` and `Web/storage.rules`.

## Team

- MOHAMMAD AMARULHARIZ BIN MOHD FAIRUZ (2025301079)
- KHAIR IZZ KHAN BIN MAHADHIR (2025115579)
- ISMA ZAHIN BIN AMIRUDDIN (2025180017)
- AMEEN AKEEF BIN MOHD ARIFF (2025120389)

## Demo video

[Link to be added]

## License

Built for academic purposes as part of ICT602 Mobile Technology Group Project.
