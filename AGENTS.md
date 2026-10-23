# Thapar Grade Predictor Platform (thapar-gpf)
## Project Architecture Report & Java Migration Specification (for Claude)

---

# SECTION 1: COMPREHENSIVE PLATFORM REPORT

### 1. Executive Summary
The **Thapar Grade Predictor Platform (TGPA)** is a specialized, privacy-first academic intelligence system engineered specifically for students of **Thapar Institute of Engineering and Technology (TIET)**. The platform solves the fundamental information asymmetry and psychological anxiety caused by the university's **relative grading system** by crowdsourcing student marks, calculating real-time cohort percentiles, and dynamically forecasting letter grades with statistical rigor.

---

### 2. What the Platform Is
The platform is a multi-role web application serving two user groups:
1. **Students**: Can securely enter their subject scores (EST, MST, Quizzes, Assignments, Labs), receive instant relative percentile calculations, view predicted letter grades, inspect Gaussian score distributions, and gauge their academic standing relative to class peers.
2. **Administrators**: Can configure curriculum subjects, set historical grade boundaries, monitor system-wide aggregate statistics, launch subject polls, and review student unlock requests.

**Crucial Privacy Architecture**: Unlike conventional university portals, this platform enforces a **Zero-Knowledge Privacy Standard**:
- Individual student marks are **confidential**.
- **Administrators and faculty cannot view individual marks entered by students.**
- Only the student who uploaded the marks can view or edit them.
- The system backend acts solely as an automated calculation engine that aggregates data anonymously to compute percentiles, averages, medians, standard deviations, and grade cutoffs.

---

### 3. Why It Exists & What Problem It Solves

#### A. The Relative Grading Dilemma
In an absolute grading system (e.g. >90% = A, >80% = B), students can easily evaluate their academic status independently. However, Thapar Institute utilizes a **relative grading system** based on cohort distribution curves:
$$\text{Grade} = f(\text{Cohort Percentile}, \mu, \sigma, \text{Minimum Pass Threshold})$$
In relative grading:
- A score of 72/100 might be an **A** if the exam was difficult and the class average is 45.
- The exact same score of 72/100 might be a **B-** or **C** if the exam was easy and the class average is 80.
- Because the institute does not publish live marks distributions during the semester, students endure months of uncertainty, anxiety, and helplessness regarding their academic trajectory.

#### B. The Failure of Informal Workarounds
Prior to this platform, students turned to ad-hoc methods:
1. **Google Sheets shared on WhatsApp/Telegram**:
   - Prone to malicious vandalism and formula tampering.
   - Lacks email authentication; trolls enter fake numbers (e.g. 100/100 or -50).
   - High privacy risk: Students are forced to expose their names, roll numbers, and marks publicly to their peers.
2. **Reddit / Discord Polls**:
   - Highly coarse, unverified, and lacking mathematical modeling (no percentile curves, no standard deviation calculations).

#### C. The Solution: Thapar Grade Predictor Platform
This platform provides:
1. **Institutional Authenticity**: Only students with official `@thapar.edu` emails can log in and submit marks.
2. **Empirical Precision**: Automatically sorts all cohort submissions, builds percentile ranks, and calculates standard statistical indicators.
3. **Ironclad Privacy**: Protects students from judgment or administrative scrutiny by isolating individual marks from all human administrators.
4. **Data Integrity**: Enforces a strict one-time submission rule with a managed unlock-request mechanism to prevent tampering.

---

### 4. Why Students Are Benefited
1. **Elimination of Academic Anxiety**: Students gain immediate clarity on where they stand on the grading curve long before official semester results are declared.
2. **Strategic Study Prioritization**: By knowing their current percentile in each subject, students can strategically allocate their preparation hours toward subjects where their relative performance is in danger (e.g. boosting MST/EST scores).
3. **Safe Participation**: Because individual marks are completely shielded from admins and peers, students can enter honest scores without fear of stigma or reprisal.
4. **Actionable Benchmarks**: Students view comprehensive class metrics:
   - Cohort Mean (Average)
   - Median & Mode
   - Standard Deviation (spread of scores)
   - Highest & Lowest recorded marks
   - Prediction Confidence Score based on submission volume

---

### 5. Detailed Functional Specifications & Business Rules

#### 5.1. Authentication & Authorization
- **Domain Restriction**: Enforces that only emails matching `*@thapar.edu` are permitted.
- **Roles**:
  - `STUDENT`: Can browse subjects, submit marks, view personal predictions, request unlocks, and participate in polls.
  - `ADMIN`: Can manage subjects, set historical cutoffs, launch polls, review unlock requests, and inspect aggregate analytics.
  - **Admin Barrier**: Admins **never** see individual student marks.

#### 5.2. Marks Submission Engine
- **Mandatory Score**: Total Marks ($0.0 - 100.0$), used for percentile and grade calculations.
- **Optional Internal Breakdown**:
  - End Semester Exam (EST): $0 - 30$
  - Mid Semester Exam (MST): $0 - 40$
  - Quizzes: $0 - 10$
  - Assignments: $0 - 20$
  - Lab Experiments: $0 - 20$
  - Practical Exam: $0 - 20$
- **One-Time Submission & Locking**:
  - Once submitted, `isLocked = true`. The student cannot modify marks arbitrarily.
  - To edit marks, the student submits an **Unlock Request** detailing the reason (e.g., re-evaluation marks or input typo).
  - Admin approves or rejects the unlock request. Admin **never** sees or edits marks.
  - Upon approval, the submission is unlocked (`isLocked = false`), enabling the student to re-enter their updated marks securely.

#### 5.3. Grade Prediction Algorithm
- **Activation Threshold**: Relative percentile grading activates **only when 100 or more valid submissions** have been recorded for that subject.
- **Percentile Calculation Formula**:
  $$\text{Percentile} = \left( \frac{\text{Count of cohort students with marks } < \text{ student's marks}}{\text{Total valid submissions in subject}} \right) \times 100$$
- **Relative Grade Mapping Rules**:
  - $\text{Percentile} \ge 90\% \implies \mathbf{A}$ (Top 10%)
  - $80\% \le \text{Percentile} < 90\% \implies \mathbf{A-}$ (Top 10%–20%)
  - $55\% \le \text{Percentile} < 80\% \implies \mathbf{B}$ (Top 20%–45%)
  - $30\% \le \text{Percentile} < 55\% \implies \mathbf{B-}$ (Top 45%–70%)
  - $20\% \le \text{Percentile} < 30\% \implies \mathbf{C}$ (Top 70%–80%)
  - $0\% \le \text{Percentile} < 20\% \implies \mathbf{D}$ (Top 80%–100%)
- **Absolute Failure Override**:
  $$\text{If Total Marks} < 35.0 \implies \mathbf{E} \text{ (Fail, irrespective of percentile)}$$
- **Historical Fallback Mode (< 100 Submissions)**:
  - When fewer than 100 submissions exist, percentile predictions lack statistical confidence.
  - The system automatically falls back to historical cutoff boundaries configured by the administrator for that subject:
    - Default: $\ge 85 \to A, \ge 75 \to A-, \ge 65 \to B, \ge 55 \to B-, \ge 45 \to C, \ge 35 \to D, < 35 \to E$.

#### 5.4. Statistical Analysis Engine
For each subject, the system computes and caches:
- $N$ (Total Responses)
- Mean: $\mu = \frac{1}{N}\sum_{i=1}^N x_i$
- Median: 50th percentile score
- Mode: Most frequent score
- Standard Deviation: $\sigma = \sqrt{\frac{1}{N}\sum_{i=1}^N (x_i - \mu)^2}$
- Pass Percentage ($x \ge 35$) and Failure Percentage ($x < 35$)
- Decile Histogram Buckets: $0-10, 10-20, \dots, 90-100$

---

# SECTION 2: THE MASTER CLAUDE PROMPT
### (Copy and paste the prompt below directly into Claude to build the complete project in Java)

```markdown
You are a Principal Java Architect. Build a complete, fully functional, production-ready web application in Java called:

**"Thapar Grade Predictor Platform" (thapar-gpf)**

Replicate all features, business rules, statistical models, and privacy guarantees from the platform described in the specification below.

### IMPORTANT DESIGN & UI PHILOSOPHY:
1. **Language & Ecosystem**: 100% Java (Java 17 or 21 LTS, Spring Boot 3.x).
2. **UI Simplicity**: The USER has explicitly requested: **"The UI should be way too simple such that it does not use high quality code, just normal Java code"**.
   - Use clean, simple Spring Boot Thymeleaf templates or basic HTML/CSS forms.
   - Do NOT use React, Next.js, Vue, Vite, or complex frontend tooling.
   - Simple HTML tables, basic clean CSS cards, standard HTML5 form controls.
   - Plain, readable, beginner-to-intermediate standard Java code without unnecessary abstractions.
3. **CRITICAL PRIVACY REQUIREMENT**:
   - Currently, university admins are NOT allowed to see student marks.
   - Individual student marks must be stored such that **ONLY THE SYSTEM** (for statistical math) and **THE USER WHO UPLOADS THE MARKS** can access them.
   - **NO ADMIN CAN ACCESS OR VIEW INDIVIDUAL STUDENT MARKS.**
   - All admin analytics, poll results, and exports must be strictly aggregated statistics (mean, median, response count, distributions) with zero student identity linkage.

---

### TECH STACK REQUIREMENTS:
- **Language**: Java 17 or 21
- **Framework**: Spring Boot 3.2+ (spring-boot-starter-web, spring-boot-starter-thymeleaf, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-validation)
- **Database**: H2 (for zero-setup dev/testing) and PostgreSQL/MySQL compatible (Spring Data JPA / Hibernate) OR MongoDB (Spring Data MongoDB)
- **Security**: Spring Security with role-based access control (`STUDENT`, `ADMIN`), session/cookie authentication, and `@thapar.edu` domain restriction.
- **Java Features to Highlight**:
  - **Java Records** for immutable DTOs (e.g. `MarksSubmissionRequest`, `SubjectStatDTO`, `PollSummaryDTO`)
  - **Java Streams API** for calculating averages, medians, standard deviations, and percentile rankings
  - **Java Enums** for `Grade` (A, A_MINUS, B, B_MINUS, C, D, E) with helper methods and `Role` (STUDENT, ADMIN)
  - **Optional API** for safe entity lookups
  - **Custom Exception Handling** with `@ControllerAdvice`
  - **Lombok** or standard getters/setters/constructors

---

### COMPLETE SYSTEM MODULES & SPECIFICATIONS:

#### 1. Models / Entities:
- `User`: id, email (unique, must end with @thapar.edu), name, rollNumber, branch, semester, role (STUDENT, ADMIN), createdAt
- `Subject`: id, code (unique, e.g. "UEC501"), name ("Data Structures"), semester, branch, credits, isActive, createdAt
- `Mark`: id, user (ManyToOne, indexed), subject (ManyToOne, indexed), totalMarks (double, 0-100), estMarks (0-30), mstMarks (0-40), quizMarks (0-10), assignmentMarks (0-20), labMarks (0-20), practicalMarks (0-20), predictedGrade (String/Enum), percentile (Double), isLocked (boolean, default true), submittedAt (LocalDateTime)
  - Unique constraint on `(user, subject)`.
- `EditRequest` (Unlock Request): id, user (ManyToOne), mark (ManyToOne), reason (String), status ("PENDING", "APPROVED", "REJECTED"), rejectionReason (String), createdAt, reviewedAt
- `SubjectStatistics`: id, subject (OneToOne), totalResponses (int), averageMarks (double), medianMarks (double), modeMarks (double), highestMarks (double), lowestMarks (double), standardDeviation (double), passPercentage (double), failurePercentage (double), gradeA (int), gradeAMinus (int), gradeB (int), gradeBMinus (int), gradeC (int), gradeD (int), gradeE (int), lastUpdated (LocalDateTime)
- `HistoricalCutoff`: id, subject (OneToOne), cutoffA (double, default 85), cutoffAMinus (75), cutoffB (65), cutoffBMinus (55), cutoffC (45), cutoffD (35), createdAt
- `Poll`: id, subject (ManyToOne), question (String), endTime (LocalDateTime), isActive (boolean), createdAt
- `PollResponse`: id, poll (ManyToOne), user (ManyToOne), marks (double), submittedAt (LocalDateTime). Unique on `(poll, user)`.

---

#### 2. Business Logic & Grade Prediction Rules:
1. **One-Time Submission**:
   - A student can submit marks for a subject only once.
   - Immediately sets `isLocked = true`.
   - If `isLocked == true`, further POST submissions are rejected with HTTP 400.
2. **Unlock Flow (Privacy-Preserving)**:
   - Student creates an `EditRequest` with a `reason` (e.g. "MST marks re-evaluated").
   - Admin views the list of pending unlock requests (Student Name, Subject, Reason). **Admin does not see the student's marks.**
   - Admin clicks **Approve Unlock**.
   - System updates `mark.setLocked(false)`.
   - Student visits their submission form, updates marks, and upon submit, `isLocked` is reset to `true`.
3. **Prediction Engine**:
   - If `totalValidSubmissions >= 100`: Use relative percentile ranking:
     - $\text{Percentile} = \left( \frac{\text{Count}(m < \text{studentMarks})}{\text{TotalSubmissions}} \right) \times 100$
     - Top 10% ($\ge 90\%$) $\to$ A
     - 10%–20% ($\ge 80\%$) $\to$ A-
     - 20%–45% ($\ge 55\%$) $\to$ B
     - 45%–70% ($\ge 30\%$) $\to$ B-
     - 70%–80% ($\ge 20\%$) $\to$ C
     - 80%–100% ($\ge 0\%$) $\to$ D
   - If `totalMarks < 35.0`: Grade is **E** (Fail) regardless of percentile!
   - If `totalValidSubmissions < 100`: Fall back to `HistoricalCutoff` boundaries.
4. **Strict Confidentiality Enforcement**:
   - **No query allows Admin to fetch or list individual student marks.**
   - Repositories: `markRepository.findByUserId(Long userId)` can ONLY be called by the authenticated student checking their own records.
   - Admin routes:
     - `/admin/users`: Shows user metadata and role promotion. No marks column or view marks button.
     - `/admin/analytics`: Shows overall metrics and recent activity timestamps without student marks.
     - `/admin/polls/{id}/responses`: Returns aggregate response counts, average, highest, lowest, and bucket distribution. Never individual student marks.
     - `/admin/export`: Generates CSV/PDF of **Subject-level Statistics**, never individual student marks.

---

#### 3. Controller & View Architecture (Thymeleaf):
- **Authentication**:
  - `/login`: Simple login form (email & password or simulated OTP/Google login for `@thapar.edu`).
  - `/logout`
- **Student Views**:
  - `/student`: Student dashboard. Lists student's submitted marks, predicted grades, class averages, and active subjects.
  - `/student/submit/{subjectId}`: Form for entering Total Marks (0-100) and optional breakdown (EST, MST, Quizzes, Labs).
  - `/student/marks/{markId}`: Details page showing student's marks, predicted grade, percentile, class average, and "Request Unlock" button.
  - `/student/edit-requests`: List of student's unlock requests with status (PENDING, APPROVED, REJECTED) and "Update Marks Now" button if approved.
  - `/student/polls`: List of active polls to submit anonymous poll marks.
- **Admin Views**:
  - `/admin`: Admin dashboard with subject counts, submission counters, export buttons, and quick links.
  - `/admin/subjects`: Add/edit subjects (Code, Name, Credits, Semester, Branch).
  - `/admin/cutoffs`: Configure fallback cutoffs for each subject.
  - `/admin/users`: List users, promote to admin, demote to student.
  - `/admin/edit-requests`: Approve/reject unlock requests.
  - `/admin/polls`: Create poll, set duration, set manual cutoffs, and view aggregate statistical distribution.
  - `/admin/analytics`: View aggregate pass/fail rates, top performing subjects, and grade distributions.

---

### DELIVERABLES EXPECTED:
Provide the complete Maven project with:
1. `pom.xml` with all dependencies (Spring Web, Security, Data JPA, Thymeleaf, H2/PostgreSQL, Validation, Lombok).
2. `application.properties` configured for local running.
3. Domain Entities (`User`, `Subject`, `Mark`, `EditRequest`, `SubjectStatistics`, `HistoricalCutoff`, `Poll`, `PollResponse`).
4. Repositories (Spring Data JPA).
5. Service Layer (`GradePredictionService` with Java Streams math, `SubjectService`, `MarksService`, `PollService`, `UserService`).
6. Security Configuration (`SecurityConfig.java`) enforcing role-based permissions and student email domain restriction.
7. Controllers (`AuthController`, `StudentController`, `AdminController`).
8. Simple, clean Thymeleaf HTML templates (`layout.html`, `student/dashboard.html`, `student/submit.html`, `student/mark-details.html`, `student/edit-requests.html`, `admin/dashboard.html`, `admin/users.html`, `admin/edit-requests.html`, `admin/polls.html`).
9. Simple CSS stylesheet (`main.css`) for clean, normal layout.
10. Database Initializer (`DataInitializer.java`) seeding sample subjects (e.g. UEC501, UCS301), an admin account (`admin@thapar.edu`), and sample students.
```
