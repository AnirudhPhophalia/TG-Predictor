# Thapar Grade Predictor

## Implementation

Use Java 21, JavaFX (controls only, UI built in Java code, styled by src/main/resources/app.css) and plain JDBC with H2. Do not introduce Spring, an ORM, FXML or a web frontend. Keep the four packages: `login` (accounts, Google sign-in, login screen), `database` (one class per table: record + SQL), `calculation` (grading math, marks, cutoffs, polls, analytics, exports) and `screens` (student and admin pages). Use the warm cream, brown and gold references in References/. Keep file names clear, comments limited and user-facing text free of emojis. Long lists use `Ui.paged(...)` (JavaFX Pagination).

## Business rules

- Google login requires a verified @thapar.edu email and hd=thapar.edu. Only ADMIN_EMAIL may receive ADMIN access.
- Detect admission years from email tokens such as be_24 and be_25. Do not guess absent years.
- Only the owner sees individual marks. Admins receive metadata and aggregates; suppress statistics below five responses. AdminScreens must never use the Mark table or MarksService.
- One submission per student/subject, locked on save. Admin-approved unlock requests allow a student to update and relock.
- Marks below 35 always receive E. At 100 submissions use strict-lower percentile ranking with thresholds 90/80/55/30/20/0. Before 100 use configured historical boundaries.
- Preserve historical cutoff records by subject and academic year. Updating active boundaries refreshes existing predictions.
- Polls accept one valid response per student until closing. Manual poll cutoffs apply only to that poll.
- Default persistence is an H2 file in data/; PostgreSQL works through DB_URL. Keep secrets and runtime data out of Git.
- Demo credentials, password sign-in and synthetic cohorts belong only to demo mode (--demo).

## Validation

Run ./mvnw verify with a full JDK 21 or newer. Check new rules with tests in src/test (they use an in-memory H2 database, no UI). See README.md for run scripts and docs/PROJECT-GUIDE.md for how the parts fit together.
