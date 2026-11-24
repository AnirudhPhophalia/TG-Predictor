package com.thapar.gpf.screens;

import com.thapar.gpf.App;
import com.thapar.gpf.AppException;
import com.thapar.gpf.calculation.GradeCalculator;
import com.thapar.gpf.calculation.MarksService;
import com.thapar.gpf.calculation.PollService;
import com.thapar.gpf.database.EditRequest;
import com.thapar.gpf.database.Mark;
import com.thapar.gpf.database.Poll;
import com.thapar.gpf.database.Subject;
import com.thapar.gpf.database.SubjectStats;
import com.thapar.gpf.database.User;
import com.thapar.gpf.login.Accounts;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.thapar.gpf.screens.Ui.*;

/** Everything a student sees. Every read uses the signed-in student's own id. */
public final class StudentScreens {

    private StudentScreens() {
    }

    private static User me() {
        return Accounts.current();
    }

    // ---------- overview ----------

    public static Node overview(Integer semester) {
        User student = me();
        List<Mark> myMarks = Mark.mine(student.id());
        Map<Long, Subject> subjects = Subject.all().stream().collect(Collectors.toMap(Subject::id, Function.identity()));
        Set<Long> submitted = myMarks.stream().map(Mark::subjectId).collect(Collectors.toSet());
        List<Subject> active = Subject.allActive();
        List<Subject> open = active.stream().filter(s -> !submitted.contains(s.id())).toList();
        double averagePoints = myMarks.stream().filter(m -> m.predictedGrade() != null)
                .mapToInt(m -> m.predictedGrade().points()).average().orElse(0);
        List<Mark> rows = myMarks.stream()
                .filter(m -> semester == null || semester.equals(subjects.get(m.subjectId()).semester())).toList();

        HBox top = row(heading("YOUR ACADEMIC PERSPECTIVE", "Welcome back, " + student.name() + ".",
                "A little more insight. A little less uncertainty."), grow(),
                secondary("Explore subjects  →", () -> App.show(subjects("", null))));

        var metrics = tiles(List.of(
                metric("YOUR SUBMISSIONS", String.valueOf(myMarks.size()), "Your contribution to the bigger picture"),
                metric("AVERAGE GRADE POINT", fmt(averagePoints, 2) + " / 10", "Unweighted estimate across submissions"),
                metric("READY TO SUBMIT", String.valueOf(open.size()), "More subjects, more clarity"),
                metric("ACTIVE SUBJECTS", String.valueOf(active.size()), "Across the academic community")));

        ComboBox<String> filter = semesterPicker(semester, true);
        filter.setOnAction(e -> App.act(() -> App.show(overview(semesterOf(filter)))));
        VBox submissions = card(row(new VBox(eyebrow("YOUR PROGRESS"), h2("Your submissions")), grow(), filter));
        if (rows.isEmpty()) {
            submissions.getChildren().add(card(text("GP", "empty-symbol"), h3("Your next chapter starts here."),
                    text("Submit your first subject score to see your predicted grade and class insights."),
                    button("Find a subject", () -> App.show(subjects("", null)))));
        } else {
            submissions.getChildren().add(paged("overview-" + semester, rows, 6,
                    page -> new VBox(8, page.stream().map(m -> submissionRow(m, subjects.get(m.subjectId()))).toArray(Node[]::new))));
        }

        VBox submit = card(eyebrow("ADD YOUR PERSPECTIVE"), h2("Submit marks"),
                text("Your contribution makes everyone's predictions more useful.", "small", "muted"));
        if (open.isEmpty()) {
            submit.getChildren().add(muted("You have submitted all available subjects."));
        } else {
            submit.getChildren().add(paged("overview-open", open, 8, page -> new VBox(4, page.stream()
                    .map(s -> clickable(row(new VBox(text(s.code(), "bold"), text(s.name(), "small", "muted")), grow(),
                            text("+", "h3")), "subject-link", () -> openSubject(s.id())))
                    .toArray(Node[]::new))));
        }
        VBox privacy = card(eyebrow("YOURS, AND YOURS ALONE"), h2("Honest marks.\nPrivate insights."),
                text("Your individual marks are visible only to you. Administrators see combined statistics."),
                link("How it works  →", () -> App.show(help())));
        privacy.getStyleClass().add("dark-card");

        VBox side = new VBox(16, submit, privacy);
        side.setPrefWidth(340);
        side.setMinWidth(300);
        HBox columns = new HBox(16, submissions, side);
        HBox.setHgrow(submissions, Priority.ALWAYS);

        return page(top, notice("Predictions are estimates from community submissions. This student initiative is not "
                + "an official university portal."), metrics, columns);
    }

    private static Node submissionRow(Mark mark, Subject subject) {
        SubjectStats stats = SubjectStats.of(subject);
        VBox info = new VBox(2, eyebrow(subject.code()), h3(subject.name()),
                text("Semester " + orDash(subject.semester()) + " · Your marks: " + clean(mark.totalMarks()) + "/100 · "
                        + (mark.locked() ? "Locked" : "Ready to update"), "small", "muted"),
                text((stats.relativeGradingActive() ? "Relative grading" : "Historical fallback") + " · "
                        + stats.totalResponses() + " responses", "small", "muted"));
        VBox grade = new VBox(text(mark.predictedGrade() == null ? "-" : mark.predictedGrade().label(), "grade"),
                text("Predicted grade", "small", "muted"));
        grade.setAlignment(javafx.geometry.Pos.CENTER);
        return clickable(row(info, grow(), grade), "submission-row", () -> App.show(markDetails(mark.id())));
    }

    /** Locked marks go to their detail page (with the unlock option); anything else goes to the form. */
    private static void openSubject(long subjectId) {
        Optional<Mark> existing = Mark.mineForSubject(me().id(), subjectId);
        if (existing.isPresent() && existing.get().locked()) {
            App.show(markDetails(existing.get().id()));
            App.fail("Your marks are locked. Send an unlock request to change them.");
        } else {
            App.show(submit(subjectId));
        }
    }

    // ---------- subjects ----------

    public static Node subjects(String search, Integer semester) {
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<Subject> found = Subject.allActive().stream()
                .filter(s -> semester == null || semester.equals(s.semester()))
                .filter(s -> (s.code() + " " + s.name() + " " + s.branch()).toLowerCase(Locale.ROOT).contains(query))
                .toList();

        TextField searchField = field(search, "Subject name, code or branch");
        ComboBox<String> semesterBox = semesterPicker(semester, true);
        Button find = button("Find subjects", () -> App.show(subjects(searchField.getText(), semesterOf(semesterBox))));
        searchField.setOnAction(e -> find.fire());
        VBox searchBox = labeled("Search subjects", searchField);
        HBox.setHgrow(searchBox, Priority.ALWAYS);
        HBox filters = row(searchBox, labeled("Semester", semesterBox), find);
        filters.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);

        Node results = found.isEmpty()
                ? card(h2("No matching subjects"), text("Try another search or semester."))
                : paged("subjects-" + query + "-" + semester, found, 12, page -> tiles(page.stream().map(s -> {
                    VBox card = card(eyebrow(s.code()), h3(s.name()),
                            text(orDash(s.branch()) + " · Semester " + orDash(s.semester()) + " · " + orDash(s.credits())
                                    + " credits", "small", "muted"),
                            secondary("View / submit marks  →", () -> openSubject(s.id())));
                    card.setPrefWidth(290);
                    card.setMinHeight(190);
                    return card;
                }).toList()));

        return page(heading("FIND YOUR NEXT PERSPECTIVE", "Explore subjects",
                        "Find your courses, contribute your marks, understand your standing."),
                card(filters), muted(found.size() + " subjects found"), results);
    }

    // ---------- submitting marks ----------

    public static Node submit(long subjectId) {
        User student = me();
        Subject subject = Subject.get(subjectId);
        Optional<Mark> existing = Mark.mineForSubject(student.id(), subjectId);
        if (existing.isPresent() && existing.get().locked()) throw new AppException(MarksService.LOCKED_MESSAGE);
        Mark old = existing.orElse(null);

        TextField total = field(old == null ? null : old.totalMarks(), "0 - 100");
        TextField est = field(old == null ? null : old.estMarks(), "0 - 30");
        TextField mst = field(old == null ? null : old.mstMarks(), "0 - 40");
        TextField quiz = field(old == null ? null : old.quizMarks(), "0 - 10");
        TextField assignment = field(old == null ? null : old.assignmentMarks(), "0 - 20");
        TextField lab = field(old == null ? null : old.labMarks(), "0 - 20");
        TextField practical = field(old == null ? null : old.practicalMarks(), "0 - 20");

        Button save = button("Save and lock", () -> {
            Mark mark = MarksService.submit(student.id(), subjectId, new MarksService.Submission(
                    number(total, "Total marks"), number(est, "EST"), number(mst, "MST"), number(quiz, "Quizzes"),
                    number(assignment, "Assignments"), number(lab, "Labs"), number(practical, "Practical")));
            App.show(markDetails(mark.id()));
            App.ok("Marks saved and locked. Your grade has been predicted.");
        });
        save.setDefaultButton(true);

        VBox form = card(muted(old != null
                        ? "Your marks were unlocked by the admin. Enter the corrected values; they will be locked again when you save."
                        : "You can submit only once. To change marks later you must send an unlock request."),
                labeled("Total marks (0 - 100) *", total), h3("Optional breakdown"),
                labeled("End semester exam - EST (0 - 30)", est), labeled("Mid semester exam - MST (0 - 40)", mst),
                labeled("Quizzes (0 - 10)", quiz), labeled("Assignments (0 - 20)", assignment),
                labeled("Lab experiments (0 - 20)", lab), labeled("Practical exam (0 - 20)", practical),
                row(save, secondary("Cancel", () -> App.show(overview(null)))));
        form.setMaxWidth(620);
        return page(h1("Submit marks"), text(subject.code() + " - " + subject.name(), "bold"), form);
    }

    // ---------- one submission in detail ----------

    public static Node markDetails(long markId) {
        User student = me();
        Mark mark = Mark.mine(student.id(), markId).orElseThrow(() -> new AppException("Marks not found"));
        Subject subject = mark.subject();
        SubjectStats stats = SubjectStats.of(subject);

        var summary = tiles(List.of(
                card(muted("Predicted grade"), text(mark.predictedGrade() == null ? "-" : mark.predictedGrade().label(), "grade")),
                card(muted("My marks"), text(clean(mark.totalMarks()), "number")),
                card(muted("Percentile"), text(fmt(mark.percentile() == null ? 0 : mark.percentile(), 1) + "%", "number"))));

        VBox how = card(h2("How was this predicted?"), text(stats.relativeGradingActive()
                        ? "Relative grading is active: " + stats.totalResponses() + " students submitted, so your grade comes "
                        + "from your percentile in the class."
                        : "Only " + stats.totalResponses() + " of the " + GradeCalculator.MIN_SUBMISSIONS_FOR_RELATIVE
                        + " submissions needed for relative grading are in, so the admin's historical cutoffs are used instead."),
                muted("Participation indicator: " + stats.confidenceLabel() + " (" + stats.confidencePercent() + "%). "
                        + "Marks below 35 are always an E."));

        VBox classStats = card(h2("Class statistics"), facts(
                        "Responses", String.valueOf(stats.totalResponses()), "Average", fmt(stats.average(), 2),
                        "Median", fmt(stats.median(), 2), "Mode", fmt(stats.mode(), 2),
                        "Highest", clean(stats.highest()), "Lowest", clean(stats.lowest()),
                        "Standard deviation", fmt(stats.standardDeviation(), 2),
                        "Pass / Fail", fmt(stats.passPercentage(), 1) + "% / " + fmt(stats.failurePercentage(), 1) + "%"),
                h3("Score distribution"),
                text("Response counts by score range. The distribution updates as the class contributes.", "small", "muted"),
                distribution(MarksService.scoreBuckets(subject.id())));

        VBox breakdown = card(h2("My breakdown"), table(List.of(mark),
                new Col<>("EST", m -> orDash(m.estMarks())), new Col<>("MST", m -> orDash(m.mstMarks())),
                new Col<>("Quizzes", m -> orDash(m.quizMarks())), new Col<>("Assignments", m -> orDash(m.assignmentMarks())),
                new Col<>("Labs", m -> orDash(m.labMarks())), new Col<>("Practical", m -> orDash(m.practicalMarks()))));

        VBox change = card(h2("Need to change your marks?"));
        if (!mark.locked()) {
            change.getChildren().addAll(text("Your marks are unlocked."),
                    button("Update Marks Now", () -> App.show(submit(subject.id()))));
        } else if (EditRequest.hasPending(mark.id())) {
            change.getChildren().addAll(text("Your unlock request is waiting for admin review."),
                    secondary("View my requests", () -> App.show(requests())));
        } else {
            TextArea reason = new TextArea();
            reason.setPromptText("e.g. MST marks were re-evaluated");
            reason.setPrefRowCount(3);
            reason.setWrapText(true);
            change.getChildren().addAll(
                    muted("Marks are locked after submission. The admin will see only your reason, never your marks."),
                    labeled("Reason", reason),
                    button("Request Unlock", () -> {
                        EditRequest.create(student.id(), mark.id(), reason.getText());
                        App.show(requests());
                        App.ok("Unlock request sent to the admin.");
                    }));
        }

        return page(h1(subject.code() + " - " + subject.name()), summary, how, classStats, breakdown, change,
                link("←  Back to dashboard", () -> App.show(overview(null))));
    }

    // ---------- unlock requests ----------

    public static Node requests() {
        User student = me();
        List<EditRequest> mine = EditRequest.mine(student.id());
        Node content = mine.isEmpty() ? muted("You have not sent any unlock requests.")
                : paged("student-requests", mine, 10, page -> table(page,
                new Col<>("Subject", r -> Mark.mine(student.id(), r.markId()).map(m -> m.subject().code()).orElse("-")),
                new Col<>("Reason", EditRequest::reason),
                new Col<>("Status", r -> badge(r.status().name())),
                new Col<>("Sent", r -> when(r.createdAt())),
                new Col<>("Admin note", r -> r.rejectionReason() == null ? "" : r.rejectionReason()),
                new Col<>("", r -> Mark.mine(student.id(), r.markId())
                        .filter(m -> r.status() == EditRequest.Status.APPROVED && !m.locked())
                        .map(m -> (Object) button("Update Marks Now", () -> App.show(submit(m.subjectId()))))
                        .orElse(null))));
        return page(h1("My unlock requests"), content, link("←  Back to dashboard", () -> App.show(overview(null))));
    }

    // ---------- polls ----------

    public static Node polls() {
        User student = me();
        List<Poll> polls = Poll.all();
        Node content = polls.isEmpty() ? muted("No polls have been created yet.")
                : paged("student-polls", polls, 5, page -> new VBox(14, page.stream().map(poll -> pollCard(poll, student))
                .toArray(Node[]::new)));
        return page(heading("THE CLASS, IN PERSPECTIVE", "Subject polls",
                "Answers are anonymous. Admins only see the combined statistics."), content);
    }

    private static Node pollCard(Poll poll, User student) {
        boolean answered = Poll.hasResponded(poll.id(), student.id());
        VBox card = card(h3(poll.subject().code() + " - " + poll.question()), muted("Closes " + when(poll.endTime())));
        if (answered) card.getChildren().add(text("You have answered this poll. Thank you!", "bold"));
        card.getChildren().add(badge(poll.isOpen() ? "Open for responses" : "Closed"));
        PollService.myPrediction(poll.id(), student.id()).ifPresent(p -> {
            VBox tile = new VBox(4, eyebrow("YOUR PREDICTED GRADE"), text(p.grade().label(), "grade"),
                    text(p.relative() ? "Relative ranking" : "Configured cutoff prediction", "small", "muted"));
            tile.getStyleClass().add("detail-tile");
            card.getChildren().add(tile);
        });
        if (poll.isOpen() && !answered) {
            TextField marks = field(null, "0 - 100");
            card.getChildren().add(row(labeled("Your marks (0 - 100)", marks), button("Submit answer", () -> {
                PollService.respond(student.id(), poll.id(), number(marks, "Marks"));
                App.show(polls());
                App.ok("Thanks! Your anonymous answer was recorded.");
            })));
        }
        return card;
    }

    // ---------- profile ----------

    public static Node profile() {
        User student = me();
        TextField roll = field(student.rollNumber(), "e.g. 102203001");
        TextField branch = field(student.branch(), "e.g. COE");
        ComboBox<String> semester = semesterPicker(student.semester(), false);

        VBox admission = new VBox(4, eyebrow("ADMISSION YEAR"),
                h3(student.admissionYear() != null ? String.valueOf(student.admissionYear()) : "Not detected"),
                text("From your institute email", "small", "muted"));
        VBox since = new VBox(4, eyebrow("MEMBER SINCE"), h3(day(student.createdAt())),
                text(student.googleSubject() != null ? "Verified with Google" : "Local demo account", "small", "muted"));
        admission.getStyleClass().add("detail-tile");
        since.getStyleClass().add("detail-tile");

        VBox card = card(row(text(student.name().substring(0, 1), "avatar"), new VBox(h2(student.name()), muted(student.email()))),
                tiles(List.of(admission, since)),
                labeled("Roll number", roll), row(labeled("Branch", branch), labeled("Current semester", semester)),
                button("Save profile", () -> {
                    Accounts.updateProfile(roll.getText(), branch.getText(), semesterOf(semester));
                    App.show(profile());
                    App.ok("Profile updated.");
                }));
        card.setMaxWidth(720);
        return page(heading("THE PERSON BEHIND THE PERSPECTIVE", "Your profile", null), card,
                link("Back to overview", () -> App.show(overview(null))));
    }

    // ---------- help ----------

    public static Node help() {
        var bands = new javafx.scene.layout.GridPane();
        bands.getStyleClass().add("facts");
        String[][] cells = {{"Percentile at least", "90", "80", "55", "30", "20", "0"},
                {"Predicted grade", "A", "A-", "B", "B-", "C", "D"}};
        for (int r = 0; r < cells.length; r++) {
            for (int c = 0; c < cells[r].length; c++) bands.add(text(cells[r][c], c == 0 ? "fact-label" : "fact-value"), c, r);
        }
        VBox prediction = card(h2("From a score to a perspective"),
                text("Submit a total out of 100 for a subject. You can also enter optional exam, quiz, assignment and lab "
                        + "marks. These components are for reference; your total drives the prediction."),
                h3("Before 100 submissions"),
                text("Predictions use the subject's configured historical cutoffs. Default boundaries are A: 85, A-: 75, "
                        + "B: 65, B-: 55, C: 45 and D: 35. Defaults are estimates, not verified university records."),
                h3("At 100 submissions and beyond"),
                text("Your percentile is the number of students strictly below your score divided by all submissions, "
                        + "multiplied by 100. Tied scores receive the same percentile."),
                bands, text("Scores below 35 always receive E, regardless of percentile."));
        VBox privacy = card(h2("Your marks remain private"),
                text("Only your signed-in account can see your individual submissions. Administrators manage subjects, polls "
                        + "and unlock requests, and see aggregate statistics. Admin statistics are hidden until at least "
                        + "five students respond."),
                h3("Made a mistake?"),
                text("Open your submission and request an unlock with a reason. An administrator can approve or reject it "
                        + "without seeing your score. After approval, update your marks; the submission locks again."),
                h3("How reliable is the prediction?"),
                text("The contribution indicator rises with the response count and reaches 100% at 100 submissions. It "
                        + "measures participation, not a statistical probability of correctness. Community data can be "
                        + "incomplete or unrepresentative."),
                h3("Google login and admission year"),
                text("Use your verified Thapar Google account. Addresses containing be_24 or be_25 are recognised as "
                        + "admission years 2024 and 2025. Other institute addresses can sign in; an undetected year stays blank."));
        return page(heading("A LITTLE CONTEXT, A LOT MORE CLARITY", "Understanding your prediction", null), prediction, privacy);
    }
}
