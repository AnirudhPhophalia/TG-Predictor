# Thapar Grade Predictor

A Java 21 / Spring Boot application for Thapar students, with Thymeleaf pages, Spring Security Google OIDC login, Spring Data JPA and a persistent database. There is no React, Next.js, Node.js or frontend build pipeline.

The interface follows the cream, brown and gold palette in `References/`, with responsive layouts, animated headings, scrolling text, distribution graphics and reduced-motion support.

## Run a local demo

Install a **full JDK 21**. Maven is downloaded automatically by the included Maven Wrapper on the first run.

```sh
./run.sh demo
```

Windows: `run.cmd demo`. Open http://localhost:8080 and expand **Local demo sign-in**.

| Account | Email | Password |
| --- | --- | --- |
| Student | aarav@thapar.edu | student123 |
| Student with detected admission year | anirudh_be_24@thapar.edu | student123 |
| Administrator | admin@thapar.edu | admin123 |

Demo data lives separately in `data/demo-grade-predictor.mv.db`. It includes 120 clearly named synthetic students for UCS301. Demo login and registration are disabled outside the demo profile. Never use the demo profile for a live deployment. Start the demo on a fresh demo database to populate its sample data.

## Enable real institute Google login

1. Create a Google Cloud OAuth client with application type **Web application**. Configure the Google consent screen for the institute, or add permitted test users while it is in testing.
2. Add `http://localhost:8080/login/oauth2/code/google` as an authorised redirect URI. Add the equivalent HTTPS URI for your deployment.
3. Copy `.env.example` to `.env`. Set `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` and the single `ADMIN_EMAIL` (default `admin@thapar.edu`). All accounts, including the admin, must belong to `@thapar.edu`.
4. For local H2, remove `DB_URL`, `DB_USER` and `DB_PASSWORD` from `.env`. Run `./run.sh`.

The shell launcher loads `.env`; it is a local shell configuration file. For Windows or a direct `java -jar` launch, provide the same values as environment variables. `run.cmd` uses variables already set in its shell; it does not load `.env`.

Google OIDC verifies the signed ID token, issuer, audience, nonce and OAuth state. The application then requires `email_verified=true`, the exact `@thapar.edu` domain and the Google hosted-domain claim `hd=thapar.edu`. A lookalike domain or a user-supplied email field cannot grant access. Google credentials are not included in this repository, and live Google sign-in needs your own client configuration.

Admission years are derived from local email portions such as `name_be_24` → 2024, `name.be_25` → 2025 and `be24` → 2024. Unrecognised formats remain blank rather than guessing. Only the configured email receives ADMIN access; promotion of other accounts is rejected. Changing ADMIN_EMAIL takes effect on new sign-ins; restart the application to invalidate existing in-memory sessions.

## Database

The default database is a **persistent H2 file** at `data/grade-predictor.mv.db`. Student marks, users, polls, unlock requests, settings, active cutoffs and past-year cutoff records survive restarts. No live users or marks are seeded outside demo mode; only sample curriculum subjects and estimated fallback boundaries are created.

For PostgreSQL, set `DB_URL` (a JDBC URL), `DB_USER` and `DB_PASSWORD`, then run:

```sh
./run.sh postgres
```

Or use Docker after filling `.env`:

```sh
docker compose up --build
```

The Compose database uses a persistent volume. `SECURE_COOKIES=false` supports local HTTP; use `true` behind HTTPS in production. Configure trusted reverse-proxy forwarding in your hosting environment if TLS terminates before the application. Back up the database before upgrades. Hibernate creates/updates tables for development; use reviewed schema migrations for managed production deployments.

### Prisma source database

Prisma is an ORM, not a database. The original `prisma/schema.prisma` targets MongoDB and generates a JavaScript client. Its schema is retained at `docs/legacy-mongodb-schema.prisma` for migration reference. Because Prisma has no official Java client, this implementation uses JPA for the requested Java-only runtime. It does **not** connect to or silently migrate the legacy MongoDB database. See [database migration](docs/database-migration.md) for the mapping and limitations. Actual legacy records and database credentials were not supplied.

## Features

- Verified institute Google login, admission-year detection, single-email admin access and student profile updates.
- Search and semester filtering for subjects and personal submissions.
- One submission per student/subject, optional component marks, immediate locking and an admin-reviewed unlock flow.
- Relative grading at 100 submissions, strict-lower percentile ranking, historical fallback and unconditional failure below 35.
- Mean, median, mode, standard deviation, pass/fail rates, contribution indicator, grade counts and score histograms.
- Admin subject creation, editing and activation; persistent **year-labelled cutoff records**, optional activation and immediate prediction refresh.
- Subject polls with deadlines, one answer per student, personal predictions, independent manual poll cutoffs and anonymous admin distributions.
- Admin community metadata, unlock review, aggregate analytics and genuine CSV, Excel (XLSX) and PDF exports.
- Persistent maintenance and unlock-request settings. Maintenance pauses marks and poll submissions without hiding existing records.
- Responsive UI, CSS animations, reduced-motion preferences, keyboard focus states and accessible form labels.

Past-year records start empty. The default grade boundaries are estimates, not verified Thapar results. Enter verified records through **Admin → Grade cutoffs** and select **Use these boundaries for predictions** when appropriate. Saving another year does not overwrite other years. Saving the same subject/year updates that record.

## Privacy

Students can view their own marks. Admin routes cannot load individual student submissions; analytics and exports expose aggregates, with suppression below five responses. Unlock review includes a name, subject and reason, not the marks. Database operators still have database-level access; this is application-level access control, not encryption or a cryptographic zero-knowledge system. With small cohorts, student-facing class statistics may also allow inferences.

Predictions are community estimates and are not official results. The contribution indicator reaches 100% at 100 submissions; it is not a probability of prediction accuracy. Grade-point averages on the overview are unweighted estimates, not official CGPA.

## Build and test

```sh
./build.sh
./mvnw test
java -jar target/thapar-gpf-1.0.0.jar
```

`build.sh` runs the tests and packages an executable JAR. The tests cover grade boundaries, the 100-response transition, statistics, role separation, mark ownership, locking and unlock review, poll privacy, Google identity validation, admission years, single-admin enforcement, year records, manual poll cutoffs and maintenance settings. GitHub Actions runs Maven verification for pushes and pull requests.

## Project structure

```text
src/main/java/com/thapar/gpf/
  config/        Google OIDC, session security and demo initialisation
  controller/    authentication, student pages and admin pages
  dto/           validated form records and aggregate views
  exception/     controlled errors
  model/         persistent entities and enums
  repository/    Spring Data JPA repositories
  service/       prediction, privacy, marks, polls, history and settings
src/main/resources/
  templates/     server-rendered Thymeleaf pages
  static/        CSS and SVG assets
src/test/        unit and integration tests
References/      original UI screenshots
docs/           legacy schema and migration notes
```

The previous React/Prisma implementation remains available in Git history. Runtime data, secrets and build output are ignored by Git.
