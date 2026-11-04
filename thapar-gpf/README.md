# Thapar Grade Predictor Platform (thapar-gpf) - Java edition

Spring Boot 3 + Thymeleaf + Spring Data JPA + Spring Security. Plain server-rendered HTML, no frontend tooling.

## Run

```bash
mvn spring-boot:run          # http://localhost:8080  (Java 21, H2 in-memory DB)
mvn test                     # 21 tests
```

PostgreSQL: `mvn spring-boot:run -Dspring-boot.run.profiles=postgres` (set `DB_URL`, `DB_USER`, `DB_PASSWORD`).

## Demo logins (seeded at startup)

| Role    | Email                | Password     |
|---------|----------------------|--------------|
| Admin   | admin@thapar.edu     | admin123     |
| Student | aarav@thapar.edu     | student123   |
| Student | riya@thapar.edu, kabir@thapar.edu | student123 |

120 fake students (`demo001@thapar.edu` ...) have already submitted UCS301 marks so the relative (100+ submissions)
grading mode can be tried at once. Set `gpf.seed.demo-cohort=0` in `application.properties` to disable them.
New students can register at `/register` (only `@thapar.edu` emails are accepted).

## How the rules are implemented

* **Prediction**: `GradePredictionService` (Java streams). 100+ submissions: percentile = students strictly below / total x 100,
  mapped to A/A-/B/B-/C/D. Fewer than 100: the admin's `HistoricalCutoff` boundaries. Marks below 35 are always E.
* **One-time submission**: a locked mark rejects further POSTs with HTTP 400. The student sends an unlock request, an admin
  approves or rejects it, and after approval the student re-submits (which locks the mark again).
* **Statistics** (mean, median, mode, population std-dev, pass/fail %, decile histogram, grade counts) are recomputed and cached in
  `SubjectStatistics` after every submission.

## Privacy design

* Only `MarksService` (always scoped to the logged-in student's id) and the statistics code read individual marks.
* `AdminController` has no dependency on `Mark`, `MarkRepository` or `MarksService` (a test enforces this), and admins are
  blocked from `/student/**`, so no admin route can load a student's marks.
* Admin pages receive DTO records with aggregates only; the unlock-request view carries student name, subject and reason, never marks.
* **Small-sample guard**: admin statistics, the CSV export and poll results are hidden until at least 5 students have responded
  (`GradePredictionService.MIN_RESPONSES_FOR_ADMIN`), because with 1 response the "average" is that student's marks.
  Students themselves always see the class statistics of their own subject.

## Layout

```
src/main/java/com/thapar/gpf
  model/        entities + Grade, Role enums
  repository/   Spring Data JPA repositories
  dto/          records (MarksSubmissionRequest, SubjectStatDTO, PollSummaryDTO, ...)
  service/      GradePredictionService, MarksService, SubjectService, EditRequestService, PollService, UserService, AnalyticsService
  controller/   AuthController, StudentController, AdminController
  config/       SecurityConfig, GpfUserDetailsService, DataInitializer
  exception/    custom exceptions + @ControllerAdvice
src/main/resources/templates   Thymeleaf pages (layout.html, student/*, admin/*)
src/main/resources/static/css/main.css
```
