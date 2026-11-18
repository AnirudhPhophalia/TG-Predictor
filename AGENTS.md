# Thapar Grade Predictor

## Implementation

Use Java 21, Spring Boot, Thymeleaf, Spring Security OIDC and Spring Data JPA. Do not introduce React, Node.js or a frontend compilation pipeline. CSS and server-rendered HTML supply the UI. Use the warm cream, brown and gold references in References/. Keep file names clear, comments limited and user-facing text free of emojis.

## Business rules

- Google login requires a verified @thapar.edu email and hd=thapar.edu. Only ADMIN_EMAIL may receive ADMIN access.
- Detect admission years from email tokens such as be_24 and be_25. Do not guess absent years.
- Only the owner sees individual marks. Admins receive metadata and aggregates; suppress statistics below five responses.
- One submission per student/subject, locked on save. Admin-approved unlock requests allow a student to update and relock.
- Marks below 35 always receive E. At 100 submissions use strict-lower percentile ranking with thresholds 90/80/55/30/20/0. Before 100 use configured historical boundaries.
- Preserve historical cutoff records by subject and academic year. Updating active boundaries refreshes existing predictions.
- Polls accept one valid response per student until closing. Manual poll cutoffs apply only to that poll.
- Default persistence is an H2 file; PostgreSQL is the supported deployment database. Keep secrets and runtime data out of Git.
- Demo credentials and synthetic cohorts belong only to the explicit demo profile.

## Validation

Run ./mvnw verify with a full JDK 21. Check new security or business logic with meaningful integration tests. Inspect responsive pages for UI changes. See README.md for run scripts and docs/database-migration.md for legacy Prisma limitations.
