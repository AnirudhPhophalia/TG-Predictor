# Thapar Grade Predictor (JavaFX)

A desktop app for Thapar students: submit your marks, see your predicted grade and where you stand in the class, while your individual marks stay private. Built with **Java 21 + JavaFX + JDBC (H2 database)**.

New to the code? Read **[docs/PROJECT-GUIDE.md](docs/PROJECT-GUIDE.md)**. It explains the login, database and calculation parts and how the files talk to each other.

---

## Run it (the easy way)

You need a **JDK 21 or newer** (check with `java -version`). Nothing else: Maven and JavaFX download automatically on the first run (this takes a minute or two the first time).

### macOS / Linux

```sh
./run.sh demo
```

### Windows (Command Prompt)

```bat
run.cmd demo
```

### Windows (PowerShell)

```powershell
.\mvnw.cmd javafx:run '-Dapp.args=--demo'
```

A window opens. Expand **Local demo sign-in** and use one of these accounts:

| Account | Email | Password |
| --- | --- | --- |
| Student | aarav@thapar.edu | student123 |
| Student with detected admission year | anirudh_be_24@thapar.edu | student123 |
| Administrator | admin@thapar.edu | admin123 |

Demo mode uses its own database file (`data/demo-grade-predictor-fx.mv.db`) with 104 curriculum subjects and 120 synthetic students in UCS301, so relative grading (100+ submissions) works straight away. To start the demo fresh, close the app and delete the `data/` folder.

### Run from VS Code

1. Open this folder in VS Code and install the recommended **Extension Pack for Java** when asked.
2. Wait for the Java status in the bottom bar to say the project is ready (it reads `pom.xml`).
3. Open the **Run and Debug** panel, choose **Run demo (sample data)** and press the green play button (or F5).

You can also open `src/main/java/com/thapar/gpf/Main.java` and click **Run** above `main`. Without arguments, that starts the real (non-demo) mode.

### Run the tests

```sh
./mvnw test          # Windows: mvnw.cmd test
```

28 tests check grade boundaries, the 100-submission switch, statistics, mark privacy, locking and unlock review, poll privacy, Google token checks, admission years, the single admin, year records, poll cutoffs, settings and the exports.

---

## Real mode (Google sign-in)

Without `demo`, the app uses `data/grade-predictor-fx.mv.db` and only Google sign-in. Password sign-in and registration exist only in demo mode.

1. In Google Cloud Console create an **OAuth client ID** of type **Desktop app**. Configure the consent screen for the institute, or add test users while it is in testing.
2. Copy `.env.example` to `.env` and fill in `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` and `ADMIN_EMAIL` (must be `@thapar.edu`).
3. Run `./run.sh` (it loads `.env`). On Windows, set the same variables in your shell first, then `run.cmd`.

Clicking **Continue with Google** opens your browser. After you pick your account, Google sends the result back to the app on `http://127.0.0.1:<random port>`. The app accepts only a fresh token for this app, with `email_verified=true`, an exact `@thapar.edu` address and the hosted-domain claim `hd=thapar.edu`. Only `ADMIN_EMAIL` gets admin access.

## Database

- Default: an **H2 database file** in `data/`. Created automatically on first start; data survives restarts.
- PostgreSQL: set `DB_URL` (for example `jdbc:postgresql://localhost:5432/grade_predictor`), `DB_USER` and `DB_PASSWORD` in `.env`. The same tables are created there.
- Tables: `users`, `subjects`, `marks`, `subject_statistics`, `historical_cutoffs`, `past_year_cutoffs`, `polls`, `poll_responses`, `edit_requests`, `platform_settings`. See `database/Database.java`.

Past-year records start empty. The default boundaries (A 85, A- 75, B 65, B- 55, C 45, D 35) are estimates, not verified Thapar results.

## Project structure

```text
src/main/java/com/thapar/gpf/
  Main.java, App.java   start-up, window, navigation, message banner
  Config.java           settings from environment variables (.env)
  AppException.java     a broken rule, shown to the user as a red banner
  login/                1. who you are: accounts, passwords, Google sign-in, login screen
  database/             2. how data is stored: connection, tables, one class per table, demo data
  calculation/          3. the backend: percentiles, grades, statistics, polls, analytics, exports
  screens/              the student and admin pages, shared UI helpers (incl. pagination)
src/main/resources/
  app.css               colours and fonts (cream, brown, gold)
  data/subjects.tsv     the curriculum loaded on first start
src/test/               JUnit tests on an in-memory database
docs/PROJECT-GUIDE.md   how everything fits together
.vscode/                Run configurations for VS Code
```

## Pagination

Long lists are shown a page at a time with JavaFX's built-in `Pagination` control (`Ui.paged`), which also remembers the page you were on:

| Screen | Per page |
| --- | --- |
| Student: your submissions / subjects to submit | 6 / 8 |
| Student: explore subjects | 12 |
| Student: polls / unlock requests | 5 / 10 |
| Admin: subjects, users, analytics by subject | 15 |
| Admin: cutoff forms / past-year records | 5 / 10 |
| Admin: unlock requests / polls | 8 / 10 |

## Privacy

Students see only their own marks. The admin screens never load individual marks (a test enforces this); analytics and exports are aggregates, hidden below five responses. Unlock review shows a name, subject and reason, never the marks. Anyone with direct access to the database file can still read it; this is access control in the app, not encryption.

Predictions are community estimates, not official results.

The previous Spring Boot web version is in the Git history.
