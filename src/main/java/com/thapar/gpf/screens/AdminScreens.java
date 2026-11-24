package com.thapar.gpf.screens;

import com.thapar.gpf.App;
import com.thapar.gpf.AppException;
import com.thapar.gpf.Config;
import com.thapar.gpf.calculation.Analytics;
import com.thapar.gpf.calculation.CutoffService;
import com.thapar.gpf.calculation.Exports;
import com.thapar.gpf.calculation.GradeCalculator;
import com.thapar.gpf.calculation.PollService;
import com.thapar.gpf.database.Cutoffs;
import com.thapar.gpf.database.EditRequest;
import com.thapar.gpf.database.Poll;
import com.thapar.gpf.database.Settings;
import com.thapar.gpf.database.Subject;
import com.thapar.gpf.database.SubjectStats;
import com.thapar.gpf.database.User;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.thapar.gpf.screens.Ui.*;

/**
 * Everything the administrator sees.
 * PRIVACY: this class never touches individual marks. It only sees user metadata, unlock requests
 * (without marks) and aggregated statistics. A test checks that it never uses the marks table.
 */
public final class AdminScreens {

    private AdminScreens() {
    }

    // ---------- overview ----------

    public static Node overview() {
        Analytics.Report report = Analytics.build();
        HBox top = row(heading("THE COMMUNITY, AT A GLANCE", "Admin overview",
                "Manage the platform. Keep individual marks private."), grow(), exportButtons());

        var metrics = tiles(List.of(
                metric("ACTIVE SUBJECTS", String.valueOf(report.activeSubjects()), null),
                metric("SUBMISSIONS", String.valueOf(report.totalSubmissions()), null),
                metric("STUDENTS", String.valueOf(report.totalStudents()), null),
                metric("PENDING REQUESTS", String.valueOf(report.pendingRequests()), null)));

        List<Node> actions = new ArrayList<>();
        String[][] cards = {
                {"01", "Subjects", "Shape the curriculum, one course at a time."},
                {"02", "Grade cutoffs", "Past-year records and prediction boundaries."},
                {"03", "Community", "Student profiles, with privacy built in."},
                {"04", "Unlock requests", "Help students keep their submissions accurate."},
                {"05", "Subject polls", "Ask the class. Understand the distribution."},
                {"06", "Analytics", "See the bigger picture in aggregate."},
                {"07", "Settings", "Platform availability and student edit requests."}};
        List<Supplier<Node>> targets = List.of(() -> subjects(null), AdminScreens::cutoffs, AdminScreens::users,
                AdminScreens::requests, AdminScreens::polls, AdminScreens::analytics, AdminScreens::settings);
        for (int i = 0; i < cards.length; i++) {
            Supplier<Node> target = targets.get(i);
            VBox content = new VBox(6, row(text(cards[i][0], "action-number"), grow(), text("↗", "h3")),
                    h2(cards[i][1]), text(cards[i][2], "muted"));
            VBox card = clickable(content, "action-card", () -> App.show(target.get()));
            card.setPrefWidth(270);
            actions.add(card);
        }

        String database = Config.dbUrl().startsWith("jdbc:postgresql") ? "PostgreSQL" : "H2 file";
        HBox strip = row(new VBox(eyebrow("DATABASE"), h3("Connected · " + database)), grow(),
                new VBox(eyebrow("LIVE POLLS"), h3(String.valueOf(report.openPolls()))), grow(),
                new VBox(eyebrow("PRIVACY FIRST"), text("Exports contain subject-level statistics.\n"
                        + "Individual student marks are never included.", "small", "muted")));
        return page(top, metrics, tiles(actions), card(strip));
    }

    private static Node exportButtons() {
        return row(secondary("CSV", () -> export("csv")), secondary("Excel", () -> export("xlsx")),
                secondary("PDF", () -> export("pdf")));
    }

    private static void export(String extension) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save subject statistics");
        chooser.setInitialFileName("subject-statistics." + extension);
        File file = chooser.showSaveDialog(App.stage());
        if (file == null) return;
        try {
            Files.write(file.toPath(), Exports.export(extension));
        } catch (IOException e) {
            throw new AppException("Could not save the file: " + e.getMessage());
        }
        App.ok("Saved " + file.getName() + ".");
    }

    // ---------- subjects ----------

    public static Node subjects(Long editingId) {
        Subject editing = editingId == null ? null : Subject.get(editingId);
        TextField code = field(editing == null ? null : editing.code(), "UCS301");
        TextField name = field(editing == null ? null : editing.name(), "Data Structures");
        TextField semester = field(editing == null ? null : editing.semester(), "1-8");
        TextField branch = field(editing == null ? null : editing.branch(), "COE");
        TextField credits = field(editing == null ? null : editing.credits(), "1-30");

        Button save = button(editing == null ? "Add subject" : "Save changes", () -> {
            if (editing == null) {
                Subject.create(code.getText(), name.getText(), whole(semester, "Semester"), branch.getText(),
                        number(credits, "Credits"));
            } else {
                Subject.update(editing.id(), code.getText(), name.getText(), whole(semester, "Semester"),
                        branch.getText(), number(credits, "Credits"));
            }
            App.show(subjects(null));
            App.ok(editing == null ? "Subject added." : "Subject updated.");
        });
        HBox buttons = row(save);
        if (editing != null) buttons.getChildren().add(secondary("Cancel", () -> App.show(subjects(null))));
        VBox form = card(h2(editing == null ? "Add subject" : "Edit subject"),
                row(labeled("Code", code), labeled("Name", name), labeled("Semester (1-8)", semester),
                        labeled("Branch", branch), labeled("Credits", credits)), buttons);

        Node list = paged("admin-subjects", Subject.all(), 15, page -> table(page,
                new Col<>("Code", Subject::code), new Col<>("Name", Subject::name),
                new Col<>("Sem", s -> orDash(s.semester())), new Col<>("Branch", s -> orDash(s.branch())),
                new Col<>("Credits", s -> orDash(s.credits())), new Col<>("Status", s -> s.active() ? "Active" : "Inactive"),
                new Col<>("", s -> row(link("Edit", () -> App.show(subjects(s.id()))),
                        secondary(s.active() ? "Deactivate" : "Activate", () -> {
                            Subject.toggleActive(s.id());
                            App.show(subjects(editingId));
                            App.ok("Subject status changed.");
                        })))));
        return page(h1("Subjects"), form, list);
    }

    // ---------- grade cutoffs and previous-year records ----------

    public static Node cutoffs() {
        Node forms = paged("admin-cutoffs", Subject.all(), 5,
                page -> new VBox(14, page.stream().map(AdminScreens::cutoffCard).toArray(Node[]::new)));

        List<Cutoffs.YearRecord> history = Cutoffs.history();
        VBox records = card(h2("Past-year records"));
        records.getChildren().add(history.isEmpty()
                ? muted("No verified past-year records have been entered yet.")
                : paged("admin-cutoff-history", history, 10, page -> table(page,
                new Col<>("Subject", Cutoffs.YearRecord::subjectCode), new Col<>("Year", Cutoffs.YearRecord::academicYear),
                new Col<>("A", r -> clean(r.cutoffs().a())), new Col<>("A-", r -> clean(r.cutoffs().aMinus())),
                new Col<>("B", r -> clean(r.cutoffs().b())), new Col<>("B-", r -> clean(r.cutoffs().bMinus())),
                new Col<>("C", r -> clean(r.cutoffs().c())), new Col<>("D", r -> clean(r.cutoffs().d())))));

        return page(heading("A FOUNDATION FOR BETTER ESTIMATES", "Grade cutoffs",
                        "Store past-year records by subject and year. Choose which boundaries drive historical predictions."),
                notice("Default boundaries are estimates. Add verified records below; no past-year university records "
                        + "have been invented. Marks below 35 always receive E."),
                forms, records);
    }

    private static Node cutoffCard(Subject subject) {
        int currentYear = Year.now().getValue();
        TextField year = field(currentYear - 1, "Year");
        TextField[] fields = cutoffFields(Cutoffs.active(subject.id()));
        CheckBox useForPredictions = new CheckBox("Use these boundaries for predictions and refresh existing grades");
        useForPredictions.setSelected(true);

        HBox inputs = row(labeled("Academic year", year));
        inputs.getChildren().addAll(cutoffInputs(fields));
        return card(row(h2(subject.code() + " · " + subject.name()), grow(), badge("Current fallback values")),
                inputs, useForPredictions,
                button("Save year record", () -> {
                    Integer academicYear = whole(year, "Academic year");
                    if (academicYear == null) throw new AppException("Academic year is required");
                    CutoffService.saveYearRecord(subject.id(), academicYear, readCutoffs(fields), useForPredictions.isSelected());
                    App.show(cutoffs());
                    App.ok("Cutoffs saved.");
                }));
    }

    private static final String[] GRADES = {"A", "A-", "B", "B-", "C", "D"};

    private static TextField[] cutoffFields(Cutoffs initial) {
        TextField[] fields = new TextField[6];
        double[] values = initial.values();
        for (int i = 0; i < 6; i++) {
            fields[i] = field(values[i], "35-100");
            fields[i].setPrefColumnCount(5);
        }
        return fields;
    }

    private static List<Node> cutoffInputs(TextField[] fields) {
        List<Node> inputs = new ArrayList<>();
        for (int i = 0; i < 6; i++) inputs.add(labeled(GRADES[i], fields[i]));
        return inputs;
    }

    private static Cutoffs readCutoffs(TextField[] fields) {
        double[] v = new double[6];
        for (int i = 0; i < 6; i++) {
            Double value = number(fields[i], "Cutoff " + GRADES[i]);
            if (value == null) throw new AppException("All cutoffs are required");
            v[i] = value;
        }
        return new Cutoffs(v[0], v[1], v[2], v[3], v[4], v[5]);
    }

    // ---------- users ----------

    public static Node users() {
        Node list = paged("admin-users", User.all(), 15, page -> table(page,
                new Col<>("Name", User::name), new Col<>("Email", User::email),
                new Col<>("Roll no.", u -> orDash(u.rollNumber())), new Col<>("Branch", u -> orDash(u.branch())),
                new Col<>("Sem", u -> orDash(u.semester())), new Col<>("Role", User::role),
                new Col<>("Joined", u -> day(u.createdAt())),
                new Col<>("", u -> badge(u.email().equalsIgnoreCase(Config.adminEmail)
                        ? "Configured administrator" : "Student access"))));
        return page(heading("THE THAPAR COMMUNITY", "Users", "Only the email configured in ADMIN_EMAIL can access administration."),
                list, muted("Marks are private to each student and are not available here."));
    }

    // ---------- unlock requests ----------

    public static Node requests() {
        List<EditRequest.AdminView> all = EditRequest.allForAdmin();
        Node list = all.isEmpty() ? muted("No requests yet.")
                : paged("admin-requests", all, 8, page -> new VBox(12, page.stream().map(AdminScreens::requestCard)
                .toArray(Node[]::new)));
        return page(h1("Unlock requests"),
                muted("You see who asked, for which subject and why. Student marks are never shown to admins."), list);
    }

    private static Node requestCard(EditRequest.AdminView r) {
        VBox card = card(row(new VBox(h3(r.studentName() + (r.rollNumber() == null ? "" : "  ·  " + r.rollNumber())),
                        text(r.subjectCode() + " - " + r.subjectName(), "muted")), grow(),
                        new VBox(4, badge(r.status().name()), text(when(r.createdAt()), "small", "muted"))),
                text("Reason: " + r.reason()));
        if (r.rejectionReason() != null) card.getChildren().add(text("Admin note: " + r.rejectionReason(), "small", "muted"));
        if (r.status() == EditRequest.Status.PENDING) {
            TextField reason = field(null, "Rejection reason");
            reason.setPrefColumnCount(26);
            card.getChildren().add(row(
                    button("Approve Unlock", () -> {
                        EditRequest.approve(r.id());
                        App.show(requests());
                        App.ok("Unlock approved. The student can now update their marks.");
                    }),
                    reason,
                    danger("Reject", () -> {
                        EditRequest.reject(r.id(), reason.getText());
                        App.show(requests());
                        App.ok("Request rejected.");
                    })));
        }
        return card;
    }

    // ---------- polls ----------

    public static Node polls() {
        List<Subject> active = Subject.allActive();
        ComboBox<String> subject = new ComboBox<>();
        active.forEach(s -> subject.getItems().add(s.code() + " - " + s.name()));
        subject.getSelectionModel().selectFirst();
        TextField question = field(null, "What did you score in the MST?");
        question.setPrefColumnCount(30);
        TextField hours = field(48, "1-720");
        hours.setPrefColumnCount(5);

        VBox create = card(h2("Create a poll"),
                row(labeled("Subject", subject), labeled("Question", question), labeled("Duration (hours)", hours)),
                button("Create poll", () -> {
                    int index = subject.getSelectionModel().getSelectedIndex();
                    if (index < 0) throw new AppException("Choose a subject.");
                    Integer duration = whole(hours, "Duration");
                    PollService.create(active.get(index).id(), question.getText(), duration == null ? 0 : duration);
                    App.show(polls());
                    App.ok("Poll created.");
                }),
                row(muted("To set the manual grade cutoffs for a subject, use the"),
                        link("Cutoffs", () -> App.show(cutoffs())), muted("page.")));

        Node list = paged("admin-polls", PollService.summarizeAll(), 10, page -> table(page,
                new Col<>("Subject", PollService.Summary::subjectCode), new Col<>("Question", PollService.Summary::question),
                new Col<>("Status", p -> p.open() ? "Open" : "Closed"), new Col<>("Ends", p -> when(p.endTime())),
                new Col<>("Responses", PollService.Summary::responses),
                new Col<>("Average", p -> p.hidden() ? "-" : fmt(p.average(), 1)),
                new Col<>("", p -> {
                    HBox actions = row(link("Distribution", () -> App.show(pollResults(p.pollId()))));
                    if (p.open()) actions.getChildren().add(secondary("Close", () -> {
                        PollService.close(p.pollId());
                        App.show(polls());
                        App.ok("Poll closed.");
                    }));
                    return actions;
                })));
        return page(h1("Polls"), create, list);
    }

    /** Aggregate numbers only: count, average, median, highest, lowest and the distribution. */
    public static Node pollResults(long pollId) {
        PollService.Summary summary = PollService.summarize(pollId);
        Poll poll = PollService.get(pollId);

        VBox results = new VBox(14);
        if (summary.hidden()) {
            results.getChildren().add(card(text(summary.responses() + " response(s) so far. Results appear once at least "
                    + GradeCalculator.MIN_RESPONSES_FOR_ADMIN + " students have answered, so that nobody's answer can be "
                    + "worked out from the numbers.")));
        } else {
            results.getChildren().addAll(tiles(List.of(
                            metric("RESPONSES", String.valueOf(summary.responses()), null),
                            metric("AVERAGE", fmt(summary.average(), 1), null),
                            metric("MEDIAN", fmt(summary.median(), 1), null),
                            metric("HIGHEST", clean(summary.highest()), null),
                            metric("LOWEST", clean(summary.lowest()), null))),
                    card(h2("Distribution"), distribution(summary.buckets())));
        }

        TextField[] fields = cutoffFields(poll.manualCutoffs() != null ? poll.manualCutoffs() : Cutoffs.DEFAULTS);
        HBox inputs = row();
        inputs.getChildren().addAll(cutoffInputs(fields));
        VBox manual = card(h2("Manual poll cutoffs"),
                text("These boundaries apply to this poll only. Marks below 35 always receive E. Manual boundaries take "
                        + "precedence over percentile grading for this poll.", "small", "muted"),
                inputs,
                button("Save poll cutoffs", () -> {
                    PollService.setManualCutoffs(pollId, readCutoffs(fields));
                    App.show(pollResults(pollId));
                    App.ok("Poll cutoffs updated.");
                }));

        return page(h1("Poll results"), text(summary.subjectCode() + " - " + summary.question(), "bold"),
                muted("Only combined numbers are shown. Individual answers are anonymous."), results,
                link("←  Back to polls", () -> App.show(polls())), manual);
    }

    // ---------- analytics ----------

    public static Node analytics() {
        Analytics.Report report = Analytics.build();
        var metrics = tiles(List.of(
                metric("USERS", String.valueOf(report.totalUsers()), null),
                metric("SUBMISSIONS", String.valueOf(report.totalSubmissions()), null),
                metric("OVERALL PASS RATE", fmt(report.overallPassPercentage(), 1) + "%", null)));

        GridPane grades = new GridPane();
        grades.getStyleClass().add("facts");
        String[] labels = {"A", "A-", "B", "B-", "C", "D", "E"};
        for (int i = 0; i < labels.length; i++) {
            grades.add(text(labels[i], "fact-label"), i, 0);
            grades.add(text(String.valueOf(report.gradeTotals()[i]), "fact-value"), i, 1);
        }

        Node bySubject = paged("admin-analytics", report.subjectStats(), 15, page -> table(page,
                new Col<>("Subject", s -> s.code() + " - " + s.name()),
                new Col<>("Responses", SubjectStats::totalResponses),
                new Col<>("Average", s -> s.hidden() ? "-" : fmt(s.average(), 1)),
                new Col<>("Median", s -> s.hidden() ? "-" : fmt(s.median(), 1)),
                new Col<>("Std dev", s -> s.hidden() ? "-" : fmt(s.standardDeviation(), 1)),
                new Col<>("Pass %", s -> s.hidden() ? "-" : fmt(s.passPercentage(), 1)),
                new Col<>("Fail %", s -> s.hidden() ? "-" : fmt(s.failurePercentage(), 1))));

        VBox top = card(h2("Top performing subjects (by class average)"));
        if (report.topSubjects().isEmpty()) top.getChildren().add(muted("No submissions yet."));
        for (int i = 0; i < report.topSubjects().size(); i++) {
            SubjectStats s = report.topSubjects().get(i);
            top.getChildren().add(text((i + 1) + ".  " + s.code() + " - " + s.name() + ": " + fmt(s.average(), 1)));
        }
        VBox recent = card(h2("Recent submission activity"));
        if (report.recentSubmissionTimes().isEmpty()) recent.getChildren().add(muted("No activity yet."));
        report.recentSubmissionTimes().forEach(t -> recent.getChildren().add(text(when(t) + " - marks submitted")));

        return page(row(h1("Analytics"), grow(), exportButtons()), metrics,
                card(h2("Grade distribution (all subjects)"), grades),
                card(h2("Pass / fail by subject"),
                        muted("To protect students, numbers are hidden for subjects with fewer than 5 responses."), bySubject),
                top, recent);
    }

    // ---------- settings ----------

    public static Node settings() {
        Settings current = Settings.get();
        CheckBox maintenance = new CheckBox("Pause submissions for maintenance");
        maintenance.setSelected(current.maintenanceMode());
        CheckBox unlocks = new CheckBox("Allow student unlock requests");
        unlocks.setSelected(current.allowStudentEdits());
        TextField email = field(current.systemEmail(), "support@thapar.edu");

        VBox availability = card(h2("Availability and participation"), maintenance,
                text("Students can still view existing predictions. New marks and poll responses are paused.", "small", "muted"),
                unlocks, labeled("Support email", email),
                button("Save settings", () -> {
                    Settings.save(maintenance.isSelected(), unlocks.isSelected(), email.getText());
                    App.show(settings());
                    App.ok("Settings saved.");
                }));
        VBox rules = card(h2("Prediction and access rules"), facts(
                        "Relative grading threshold", GradeCalculator.MIN_SUBMISSIONS_FOR_RELATIVE + " submissions",
                        "Minimum passing score", "35 / 100",
                        "Admin statistics threshold", GradeCalculator.MIN_RESPONSES_FOR_ADMIN + " responses",
                        "Single administrator", Config.adminEmail),
                text("The administrator email is configured through ADMIN_EMAIL. Other accounts cannot be promoted.", "small", "muted"));
        availability.setMaxWidth(760);
        rules.setMaxWidth(760);
        return page(heading("KEEP THINGS RUNNING SMOOTHLY", "Platform settings", null), availability, rules);
    }
}
