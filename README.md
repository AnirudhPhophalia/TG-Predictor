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

## Deploy online

Step-by-step guide: **[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)**. It covers the app on Vercel, the database on Neon (created from inside Vercel) and Render as a fallback.

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
- Search and semester filtering for subjects and personal submissions. Overview and Explore subjects open on the student's **current semester**, worked out from the admission year in the email (`be_24` means 2024) and today's date. Odd semesters run July to December, even ones January to June, so a 2024 student is in semester 5 in November 2026. Without a year in the email, the semester saved in the profile is used. **All semesters** shows everything.
- One submission per student/subject, optional component marks, immediate locking and an admin-reviewed unlock flow.
- Relative grading at 100 submissions, strict-lower percentile ranking, historical fallback and unconditional failure below 35.
- Mean, median, mode, standard deviation, pass/fail rates, a Low/Medium/High participation indicator and score histograms. Students never see how many responses a subject has.
- Admin subject creation, editing and activation; persistent **year-labelled cutoff records**, optional activation and immediate prediction refresh.
- Subject polls with deadlines, one answer per student, personal predictions, independent manual poll cutoffs and anonymous admin distributions.
- Admin community metadata, unlock review, aggregate analytics and genuine CSV, Excel (XLSX) and PDF exports.
- Persistent maintenance and unlock-request settings. Maintenance pauses marks and poll submissions without hiding existing records.
- Pagination on every long list, with Previous / page number / Next links. Filters and the current page are kept after actions such as approving a request.
- Responsive UI, CSS animations, reduced-motion preferences, keyboard focus states and accessible form labels.

Past-year records start empty. The default grade boundaries are estimates, not verified Thapar results. Enter verified records through **Admin → Grade cutoffs** and select **Use these boundaries for predictions** when appropriate. Saving another year does not overwrite other years. Saving the same subject/year updates that record.

## Pagination

| Page | Per page | How it is paged |
| --- | --- | --- |
| Admin → Subjects | 15 | database (`Pageable`, LIMIT/OFFSET) |
| Admin → Grade cutoffs: subject forms / past-year records | 5 / 10 | database, two separate pagers |
| Admin → Users | 15 | database |
| Admin → Unlock requests | 10 | database |
| Admin → Polls | 10 | database |
| Admin → Analytics: per-subject table | 15 | in memory (computed statistics) |
| Student → Overview: submissions / subjects to submit | 6 / 8 | in memory (filtered by semester) |
| Student → Explore subjects | 12 | in memory (search and semester filter) |
| Student → Unlock requests | 10 | database |
| Student → Polls | 5 | database |

`ui/Pagination.java` has the two helpers. `Pagination.fetch` asks the repository for one page through Spring Data's `Pageable`. `Pagination.of` slices a list that Java has already filtered or computed. Both return a Spring Data `Page`, which the shared `pager` fragment in `templates/layout.html` renders. A page number past the end shows the last page.

## Privacy

Students can view their own marks, but not how many classmates have submitted. Admin routes cannot load individual student submissions; analytics and exports expose aggregates, with suppression below five responses. Unlock review includes a name, subject and reason, not the marks. Database operators still have database-level access; this is application-level access control, not encryption or a cryptographic zero-knowledge system. With small cohorts, student-facing class statistics may also allow inferences.

Predictions are community estimates and are not official results. The contribution indicator reaches 100% at 100 submissions; it is not a probability of prediction accuracy. Grade-point averages on the overview are unweighted estimates, not official CGPA.

## Build and test

```sh
./build.sh
./mvnw test
java -jar target/thapar-gpf-1.0.0.jar
```

`build.sh` runs the tests and packages an executable JAR. The tests cover grade boundaries, the 100-response transition, statistics, role separation, mark ownership, locking and unlock review, poll privacy, Google identity validation, admission years, single-admin enforcement, year records, manual poll cutoffs and maintenance settings. GitHub Actions runs Maven verification for pushes and pull requests.

## Project structure

The Java code is split into four parts. Each part only talks to the one below it:

```text
Browser ──> ui ──> backend ──> database
              ^
            login (checks who you are before any page opens)
```

```text
src/main/java/com/thapar/gpf/
  GradePredictorApplication.java   starts everything (main method)

  login/       WHO ARE YOU?
    SecurityConfig.java         which pages need sign-in, which need ADMIN, logout, cookies
    GoogleLoginConfig.java      Google client ID/secret (only when Google login is on)
    GoogleLoginService.java     accepts only verified @thapar.edu Google accounts, creates the user
    PasswordLoginService.java   email + password sign-in for the demo accounts
    LoginController.java        the /login and /register pages

  database/    WHAT IS STORED?
    NeonDataSourceConfig.java   connects to PostgreSQL/Neon from DB_URL, POSTGRES_URL or PG* variables
    StartupDataLoader.java      on startup: loads the 104 COE subjects, default cutoffs, demo users
    tables/                     one class per database table (User, Subject, Mark, Poll, ...)
    repository/                 the queries for each table (Spring Data writes the SQL)

  backend/     WHAT HAPPENS?
    service/                    the rules: grade prediction, marks locking, statistics, polls, unlocks
    dto/                        small read-only records passed from the backend to the pages
    exception/                  errors and the error page they turn into

  ui/          WHAT DO YOU SEE?
    StudentController.java      every /student/... page
    AdminController.java        every /admin/... page
    Pagination.java             splits long lists into pages
    FormErrors.java             turns form mistakes into readable messages

src/main/resources/
  templates/   the HTML pages (Thymeleaf); layout.html has the shared navbar, footer and pager
  static/      CSS and images
  data/subjects.tsv   the COE curriculum, semesters 1 to 8 plus electives (104 subjects)
  application*.properties   settings: default (H2 file), demo, postgres
src/test/      unit and integration tests
docs/          deployment guide, legacy schema and migration notes
References/    original UI screenshots
```

### Classes renamed in the reorganisation

Only names and folders changed. Database table names, page addresses and behaviour are the same as before.

| Old name | New name | Why |
| --- | --- | --- |
| `GpfApplication` | `GradePredictorApplication` | says what the app is |
| `InstituteOidcUserService` | `login/GoogleLoginService` | it handles Google sign-in |
| `GpfUserDetailsService` | `login/PasswordLoginService` | it handles password sign-in |
| `AuthController` | `login/LoginController` | it serves the login and register pages |
| `DataInitializer` | `database/StartupDataLoader` | it loads data at startup |
| `HistoricalCutoff` (+ repository) | `FallbackCutoff` | the boundaries used before 100 submissions, not old records |
| `CutoffHistoryService` | `PastYearCutoffService` | it manages `PastYearCutoff` records |
| `EditRequest`, `EditStatus` (+ service, repository, admin view) | `UnlockRequest`, `UnlockStatus` | the pages call them unlock requests |
| `CutoffsChanged` | `CutoffsChangedEvent` | it is an event |
| `controller/Pages`, `controller/Messages` | `ui/Pagination`, `ui/FormErrors` | says what they do |

The previous React/Prisma implementation remains available in Git history. Runtime data, secrets and build output are ignored by Git.
