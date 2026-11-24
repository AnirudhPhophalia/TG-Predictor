# Project guide: how the Thapar Grade Predictor works

This explains the whole app in plain words: what each part does, which technology it uses, and how the files call each other. Read it top to bottom once; after that, use the headings to jump around.

---

## 1. The app in one paragraph

Students sign in, enter their marks for a subject (out of 100), and get a **predicted grade** plus class statistics (average, median, distribution). Before a subject has 100 submissions, the grade comes from **cutoff boundaries** (A at 85, A- at 75 ...). From 100 submissions on, it comes from the student's **percentile** in the class (relative grading). An admin manages subjects, cutoffs, polls and unlock requests, but **never sees any student's individual marks**, only combined numbers.

---

## 2. Tech stack

| Layer | Technology | Where |
| --- | --- | --- |
| Language | **Java 21** (records, switch expressions, streams) | everything |
| User interface | **JavaFX 21** (controls, layouts, `TableView`, `Pagination`, `BarChart`), styled with **JavaFX CSS** | `screens/`, `login/LoginScreen.java`, `app.css` |
| Database access | **JDBC** (`Connection`, `PreparedStatement`, `ResultSet`), no ORM | `database/` |
| Database | **H2** embedded database stored in a file (`data/*.mv.db`); **PostgreSQL** optional | `database/Database.java` |
| Passwords | **PBKDF2-HMAC-SHA256** from the JDK (`javax.crypto`), salted | `login/Accounts.java` |
| Google sign-in | **OAuth 2.0 + OpenID Connect**, desktop loopback flow with PKCE, using JDK `HttpServer` and `HttpClient` | `login/GoogleLogin.java` |
| Exports | **Apache POI** (Excel `.xlsx`), **Apache PDFBox** (PDF), plain text (CSV) | `calculation/Exports.java` |
| Build | **Maven** (wrapper `mvnw` included) + `javafx-maven-plugin` | `pom.xml` |
| Tests | **JUnit 5**, on an in-memory H2 database | `src/test/` |

What changed from the old version: Spring Boot, Thymeleaf HTML pages, Spring Security and JPA/Hibernate were removed. The web pages became JavaFX screens, and the JPA repositories became plain JDBC classes.

---

## 3. The big picture

```text
                       ┌───────────────────────────────┐
                       │  Main.java → App.java         │  starts JavaFX, opens the window,
                       │  (window, navigation, banner) │  opens the database, seeds demo data
                       └───────────────┬───────────────┘
                                       │ shows screens
        ┌──────────────────────────────┼──────────────────────────────┐
        ▼                              ▼                              ▼
┌───────────────┐            ┌───────────────────┐          ┌───────────────────┐
│ 1. login/     │            │ screens/          │          │ screens/          │
│ LoginScreen   │            │ StudentScreens    │          │ AdminScreens      │
│ Accounts      │            │ (8 pages)         │          │ (9 pages)         │
│ GoogleLogin   │            └─────────┬─────────┘          └─────────┬─────────┘
└───────┬───────┘                      │ button clicks call           │
        │                              ▼                              ▼
        │                   ┌──────────────────────────────────────────────────┐
        │                   │ 3. calculation/  (the backend logic)             │
        │                   │ MarksService · GradeCalculator · CutoffService   │
        │                   │ PollService · Analytics · Exports · Grade        │
        │                   └─────────────────────────┬────────────────────────┘
        │                                             │ reads / writes rows
        ▼                                             ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│ 2. database/  one class per table (a Java record + its SQL)                   │
│ User · Subject · Mark · Poll · EditRequest · Cutoffs · SubjectStats · Settings│
│                         └──────── all go through ────────┘                    │
│                         Database.java (JDBC connection, tables, transactions) │
└───────────────────────────────────────┬───────────────────────────────────────┘
                                        ▼
                         H2 file  data/grade-predictor-fx.mv.db
```

**Rule of thumb:** screens call calculation (or simple database reads), calculation calls database, and only `Database.java` talks to JDBC. Screens never write SQL.

---

## 4. Part 1: Login (`login/`)

| File | Job |
| --- | --- |
| `LoginScreen.java` | The first page: Google button, demo email/password form, demo registration page. |
| `Accounts.java` | The rules about people: `isThaparEmail`, `admissionYear` (be_24 → 2024), `isConfiguredAdmin`, `login`, `register`, `googleLogin`, `updateProfile`, password hashing, and the **session** (`current()` = who is signed in). |
| `GoogleLogin.java` | Google sign-in for a desktop app: opens the browser, receives the answer on `http://127.0.0.1:<port>`, swaps the code for an ID token, and checks it with `verify(...)`. |

**How a demo sign-in flows**

```text
LoginScreen  "Sign in to demo" clicked
  → Accounts.login(email, password)
      → User.byEmail(email)                         (database)
      → Accounts.verify(password, storedHash)       PBKDF2, compares hashes
      → role = ADMIN only if email == ADMIN_EMAIL
  → App.signedIn(user) → AdminScreens.overview() or StudentScreens.overview()
```

**Key ideas to explain**
- Only `@thapar.edu` emails are allowed (a regex, so `a@thapar.edu.evil.com` fails).
- Passwords are never stored. The table holds `pbkdf2$310000$salt$hash`.
- Exactly one admin: the role always comes from `ADMIN_EMAIL`, even if someone edits the database row.
- Google tokens are accepted only if: issuer is Google, audience is this app, nonce matches, not expired, `email_verified=true`, `hd=thapar.edu`.
- Password login and registration exist only in demo mode (`--demo`). Real mode is Google only.

---

## 5. Part 2: Database (`database/`)

| File | Table(s) | What it stores |
| --- | --- | --- |
| `Database.java` | creates all tables | The single JDBC connection, `CREATE TABLE IF NOT EXISTS ...`, helpers `list / one / insert / update / batch`, and `transaction(...)`. |
| `User.java` | `users` | Email, name, password hash, roll no, branch, semester, admission year, role. |
| `Subject.java` | `subjects` | Curriculum (code, name, semester, branch, credits, active). Validates and creates subjects. |
| `Mark.java` | `marks` | One student's marks in one subject + predicted grade, percentile, locked flag. |
| `SubjectStats.java` | `subject_statistics` | Cached aggregates per subject: count, average, median, mode, std dev, pass %, grade counts. **No student identity.** |
| `Cutoffs.java` | `historical_cutoffs`, `past_year_cutoffs` | The six boundaries in use for predictions, and **previous-year records** (one per subject + academic year). |
| `Poll.java` | `polls`, `poll_responses` | Polls and the answers to them. |
| `EditRequest.java` | `edit_requests` | Unlock requests and the approve / reject flow. |
| `Settings.java` | `platform_settings` | Maintenance mode, unlock requests on/off, support email. |
| `DemoData.java` | fills tables | First start: loads `subjects.tsv`; in demo mode adds sample accounts and 120 synthetic UCS301 students. |

**Pattern used everywhere:** each class is a Java `record` (one row) plus static methods holding its SQL. Example from `Mark.java`:

```java
public static Optional<Mark> mine(long studentId, long markId) {
    return Database.one("SELECT * FROM marks WHERE id = ? AND user_id = ?", Mark::map, markId, studentId);
}
```

`?` placeholders + `PreparedStatement` mean user input is never pasted into SQL (no SQL injection).

**How new data comes in and is stored**
- Marks: `Mark.store(...)` inserts a new row, or overwrites an **unlocked** one, and always sets `locked = TRUE`. A `UNIQUE (user_id, subject_id)` constraint makes double submissions impossible.
- Poll answers: `Poll.addAnswer(...)`. The `user_id` column exists only so `UNIQUE (poll_id, user_id)` allows one answer per student.
- Previous-year cutoffs: `Cutoffs.saveYear(...)` updates the row for that subject + year, or inserts it. Other years are never touched.

**How anonymity is kept in the data layer**
- Anything that returns whole mark rows takes the **signed-in student's id** (`Mark.mine(studentId, ...)`).
- Everything else returns anonymous values only: `Mark.scores(subjectId)` → just numbers; `Poll.answers(pollId)` → just numbers; `Mark.recentSubmissionTimes()` → just timestamps.
- `EditRequest.allForAdmin()` joins name + subject + reason but selects no marks column.

**Transactions:** `Database.transaction(...)` turns off auto-commit, runs the work, commits, or rolls everything back on an error. Saving marks + recalculating the class is all-or-nothing.

---

## 6. Part 3: Calculation backend (`calculation/`)

| File | Job |
| --- | --- |
| `GradeCalculator.java` | Pure maths, no database: `percentile`, `predict`, `gradeFromCutoffs`, `computeStats`, `median`, `mode`, `histogram`. |
| `Grade.java` | The grades A, A-, B, B-, C, D, E, the percentile bands and grade points. |
| `MarksService.java` | Submit marks → validate → store → **recalculate the whole subject**. |
| `CutoffService.java` | Validate cutoffs, save a year record, make boundaries active and refresh grades. |
| `PollService.java` | Create / close polls, answer once, the student's own poll prediction, admin summaries. |
| `Analytics.java` | Admin dashboard numbers and the CSV. Hides subjects with fewer than 5 responses. |
| `Exports.java` | Excel and PDF versions of the same aggregate statistics. |

**The grading rules (`GradeCalculator.predict`)**

```text
percentile = (students with marks strictly below you) ÷ (all submissions) × 100

if marks < 35                → E                       (always, even in relative mode)
else if submissions ≥ 100    → by percentile: ≥90 A, ≥80 A-, ≥55 B, ≥30 B-, ≥20 C, else D
else                         → by the subject's active cutoffs (default 85/75/65/55/45/35)
```

**Statistics (`computeStats`)**: mean; median (middle value, or average of the two middle values); mode (most frequent, lower score wins a tie); population standard deviation √(Σ(x−mean)²/n); pass % = share with marks ≥ 35; histogram = ten buckets 0-10 … 90-100.

**What happens when a student submits marks**

```text
StudentScreens.submit → "Save and lock"
  → MarksService.submit(studentId, subjectId, submission)
      Settings.requireSubmissionsOpen()          maintenance mode check
      range checks (total 0-100, EST 0-30, MST 0-40 ...)
      Subject.get(...) must be active
      Mark.mineForSubject(...) must not be locked
      Database.transaction:
        Mark.store(...)                          insert or update, locked = TRUE
        recalculate(subjectId):
          Mark.scoresWithIds(subjectId)          everyone's (id, score), no names
          Cutoffs.active(subjectId)              current boundaries
          for each mark: GradeCalculator.predict(...)
          Mark.savePredictions(...)              one batch UPDATE of grade + percentile
          SubjectStats.save(...)                 cached aggregates
  → App.show(StudentScreens.markDetails(id))     result displayed
```

Why recalculate everyone? A percentile depends on the whole class, so one new score can change other students' grades.

**When the admin changes cutoffs:** `CutoffService.saveYearRecord(...)` → `Cutoffs.saveYear(...)`, and if "use for predictions" is ticked → `Cutoffs.setActive(...)` → `MarksService.recalculate(...)`. Every existing grade refreshes immediately.

**Polls:** `PollService.respond` stores one answer per student. `myPrediction` grades the student's own answer: with the poll's manual cutoffs if the admin set them, otherwise with the same rules as marks. `summarize` gives the admin count / average / median / highest / lowest / histogram, and hides everything except the count below 5 answers.

**Final results displayed:** `StudentScreens.markDetails` (grade, percentile, class statistics, distribution chart), `StudentScreens.polls` (poll prediction), `AdminScreens.analytics` and `pollResults` (aggregates only), `Exports` (CSV / Excel / PDF).

---

## 7. The UI (`screens/`, plus `App.java`)

| File | Job |
| --- | --- |
| `App.java` | The window: header with navigation buttons, a message banner (green success / red failure), a scrollable page area, a footer. `App.show(page)` swaps the page. `App.act(action)` runs a button action and turns an `AppException` into a red message. |
| `Ui.java` | Building blocks: headings, cards, buttons, form fields, number parsing, `table(...)` (a `TableView`), `paged(...)` (a `Pagination`), `distribution(...)` (a `BarChart`). |
| `StudentScreens.java` | Overview, Subjects (search + semester filter), Submit marks, Mark details, Polls, Unlock requests, Profile, Help. |
| `AdminScreens.java` | Overview, Subjects, Grade cutoffs, Users, Unlock requests, Polls, Poll results, Analytics, Settings. |
| `resources/app.css` | Colours and fonts: cream `#fbf8f3`, brown `#6b482e`, gold `#b48a5b`, Georgia headings. |

**How a screen is built:** every screen is a static method that returns a JavaFX `Node`. No FXML. For example, `StudentScreens.markDetails(markId)` reads the data, builds cards with `Ui.card(...)`, adds a `BarChart`, and returns the page. Buttons get lambdas:

```java
button("Request Unlock", () -> {
    EditRequest.create(student.id(), mark.id(), reason.getText());
    App.show(requests());
    App.ok("Unlock request sent to the admin.");
});
```

If `create` throws `AppException("You already have a pending unlock request...")`, `App.act` shows it in red and the page stays as it was.

**Pagination:** `Ui.paged(key, list, perPage, renderPage)` wraps JavaFX's `Pagination`: it creates one page of rows at a time, and remembers which page you were on (by key), so approving a request on page 3 keeps you on page 3. Used on subjects, users, cutoffs, past-year records, requests, polls, analytics and the student overview.

---

## 8. Example flow: unlock request (all four parts together)

```text
Student (StudentScreens.markDetails)  → EditRequest.create(studentId, markId, reason)
                                          checks: settings allow it, mark is mine and locked, no pending request
Admin   (AdminScreens.requests)        → EditRequest.allForAdmin()  shows name, subject, reason (no marks)
Admin clicks Approve                   → EditRequest.approve(id)    transaction: status APPROVED + Mark.setLocked(false)
Student clicks "Update Marks Now"      → MarksService.submit(...)   overwrites, locks again, recalculates class
```

---

## 9. Database tables at a glance

```text
users ──< marks >── subjects ──< polls ──< poll_responses >── users
  │         │          │  └── historical_cutoffs (1 per subject, in use now)
  │         │          ├──< past_year_cutoffs (1 per subject per year)
  │         │          └── subject_statistics (1 per subject, cached aggregates)
  └──< edit_requests >─┘(via marks)
platform_settings (single row)
```

`──<` means one-to-many. The foreign keys (`REFERENCES`) and unique constraints are in `Database.java`.

---

## 10. Running and testing

```text
./run.sh demo        start with sample data (Windows: run.cmd demo)
./mvnw test          run the 28 JUnit tests
```

In VS Code: Run and Debug → **Run demo (sample data)**.

The tests run without any window: each test opens a fresh in-memory H2 database, seeds it, and calls the same classes the screens call (`MarksService`, `PollService`, `Accounts` ...).

---

## 11. Questions you may be asked

- **Why JDBC and not an ORM?** Fewer moving parts, and every SQL statement is visible in one place per table. `PreparedStatement` keeps it safe from SQL injection.
- **Why H2?** It is embedded (no server to install), stores data in a single file, and speaks standard SQL, so the same tables work on PostgreSQL.
- **How do you stop the admin seeing marks?** The admin screens never call `Mark` or `MarksService` (a test reads `AdminScreens.java` and fails if they do). Admin data comes from aggregate-only methods and is hidden below 5 responses.
- **How is a percentile calculated, and what about ties?** Students strictly below you ÷ everyone × 100. Equal scores get the same percentile.
- **Why recalculate the whole subject on every submission?** Each percentile depends on everyone else's score.
- **What happens if two things fail halfway?** Writes that belong together run inside `Database.transaction`, which rolls back on any error.
- **Where is the event handling?** Button lambdas (`setOnAction`), `ComboBox.setOnAction` for the semester filter, `setOnMouseClicked` / `setOnKeyPressed` for clickable rows, and `Pagination.setPageFactory` for paging.
