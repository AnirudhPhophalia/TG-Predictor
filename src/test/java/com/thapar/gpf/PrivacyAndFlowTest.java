package com.thapar.gpf;

import com.thapar.gpf.calculation.Analytics;
import com.thapar.gpf.calculation.Exports;
import com.thapar.gpf.calculation.MarksService;
import com.thapar.gpf.calculation.MarksService.Submission;
import com.thapar.gpf.calculation.PollService;
import com.thapar.gpf.database.Database;
import com.thapar.gpf.database.DemoData;
import com.thapar.gpf.database.EditRequest;
import com.thapar.gpf.database.Mark;
import com.thapar.gpf.database.Poll;
import com.thapar.gpf.database.Subject;
import com.thapar.gpf.database.SubjectStats;
import com.thapar.gpf.database.User;
import com.thapar.gpf.login.Accounts;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

/** Walks through the student and admin flows and checks that admins never see individual marks. */
class PrivacyAndFlowTest {

    // an unusual number, so it cannot show up anywhere by accident
    private static final double SECRET = 73.37;

    private long ucs301;

    @BeforeEach
    void freshDemoDatabase() {
        Config.demo = true;
        Config.demoCohortSize = 0;
        Config.adminEmail = "admin@thapar.edu";
        Database.open("jdbc:h2:mem:flow" + System.nanoTime(), "sa", "");
        DemoData.seedIfEmpty();
        ucs301 = Subject.byCode("UCS301").id();
    }

    private static long id(String email) {
        return User.byEmail(email).orElseThrow().id();
    }

    private static Submission total(Double marks) {
        return new Submission(marks, null, null, null, null, null, null);
    }

    @Test
    void studentSubmitsOnceAndASecondSubmitIsRefused() {
        Mark mark = MarksService.submit(id("aarav@thapar.edu"), ucs301, new Submission(SECRET, null, 30.0, null, null, null, null));
        assertTrue(mark.locked());
        assertEquals("B", mark.predictedGrade().label());    // 73.37 with default cutoffs (>= 65)
        AppException locked = assertThrows(AppException.class, () -> MarksService.submit(id("aarav@thapar.edu"), ucs301, total(99.0)));
        assertEquals(MarksService.LOCKED_MESSAGE, locked.getMessage());
        assertEquals(SECRET, Mark.mine(id("aarav@thapar.edu"), mark.id()).orElseThrow().totalMarks());
    }

    @Test
    void studentSeesOwnMarksButNotSomeoneElses() {
        Mark mark = MarksService.submit(id("aarav@thapar.edu"), ucs301, total(SECRET));
        assertTrue(Mark.mine(id("aarav@thapar.edu"), mark.id()).isPresent());
        assertTrue(Mark.mine(id("riya@thapar.edu"), mark.id()).isEmpty());
        assertTrue(Mark.mine(id("riya@thapar.edu")).isEmpty());
    }

    @Test
    void unlockFlowWorksAndAdminNeverSeesMarks() {
        long aarav = id("aarav@thapar.edu");
        Mark mark = MarksService.submit(aarav, ucs301, total(SECRET));
        EditRequest.create(aarav, mark.id(), "MST re-evaluated");
        assertEquals(EditRequest.Status.PENDING, EditRequest.mine(aarav).getFirst().status());

        // the admin view has who / which subject / why, and no field that could hold marks
        EditRequest.AdminView view = EditRequest.allForAdmin().getFirst();
        assertEquals("Aarav Sharma", view.studentName());
        assertEquals("MST re-evaluated", view.reason());
        assertTrue(Arrays.stream(EditRequest.AdminView.class.getRecordComponents())
                .noneMatch(c -> c.getType() == double.class || c.getType() == Double.class));
        assertFalse(view.toString().contains(String.valueOf(SECRET)));

        EditRequest.approve(view.id());
        assertFalse(Mark.mine(aarav, mark.id()).orElseThrow().locked());

        // the student re-enters marks and they are locked again
        Mark updated = MarksService.submit(aarav, ucs301, total(80.5));
        assertEquals(mark.id(), updated.id());
        assertTrue(updated.locked());
        assertEquals(80.5, updated.totalMarks());
        assertEquals("A-", updated.predictedGrade().label());
    }

    @Test
    void rejectedRequestKeepsMarksLocked() {
        long riya = id("riya@thapar.edu");
        Mark mark = MarksService.submit(riya, ucs301, total(50.0));
        EditRequest.create(riya, mark.id(), "typo");
        assertThrows(AppException.class, () -> EditRequest.create(riya, mark.id(), "typo again"));   // one pending at a time
        assertThrows(AppException.class, () -> EditRequest.create(id("aarav@thapar.edu"), mark.id(), "not mine"));

        long requestId = EditRequest.mine(riya).getFirst().id();
        EditRequest.reject(requestId, "No proof");
        assertEquals(EditRequest.Status.REJECTED, EditRequest.byId(requestId).orElseThrow().status());
        assertTrue(Mark.mine(riya, mark.id()).orElseThrow().locked());
        assertThrows(AppException.class, () -> EditRequest.approve(requestId));   // already reviewed
    }

    @Test
    void invalidMarksAreRejectedAndNothingIsStored() {
        long kabir = id("kabir@thapar.edu");
        for (Double bad : new Double[]{150.0, -1.0, Double.NaN, null}) {
            assertThrows(AppException.class, () -> MarksService.submit(kabir, ucs301, total(bad)));
        }
        assertThrows(AppException.class, () -> MarksService.submit(kabir, ucs301,
                new Submission(60.0, 31.0, null, null, null, null, null)));       // EST is out of 30
        assertTrue(Mark.mineForSubject(kabir, ucs301).isEmpty());
    }

    @Test
    void onlyThaparEmailsCanRegister() {
        assertThrows(AppException.class, () -> Accounts.register("Evil", "evil@gmail.com", "secret1", null, null, null));
        assertThrows(AppException.class, () -> Accounts.register("Evil", "@thapar.edu", "secret1", null, null, null));
        assertThrows(AppException.class, () -> Accounts.register("Short", "short@thapar.edu", "12345", null, null, null));
        assertThrows(AppException.class, () -> Accounts.register("Admin", "admin@thapar.edu", "secret1", null, null, null));
        assertTrue(User.byEmail("evil@gmail.com").isEmpty());

        Accounts.register("New Student", "New.Student@Thapar.edu", "secret1", null, null, null);
        assertEquals(User.Role.STUDENT, User.byEmail("new.student@thapar.edu").orElseThrow().role());
    }

    @Test
    void passwordLoginWorksAndRoleFollowsAdminEmail() {
        assertTrue(Accounts.login("admin@thapar.edu", "admin123").isAdmin());
        assertFalse(Accounts.login("aarav@thapar.edu", "student123").isAdmin());
        assertThrows(AppException.class, () -> Accounts.login("aarav@thapar.edu", "wrong"));
        assertThrows(AppException.class, () -> Accounts.login("someone@gmail.com", "x"));
        assertFalse(User.byEmail("aarav@thapar.edu").orElseThrow().passwordHash().contains("student123"));

        Config.demo = false;      // outside demo mode only Google sign-in exists
        assertThrows(AppException.class, () -> Accounts.login("admin@thapar.edu", "admin123"));
        assertThrows(AppException.class, () -> Accounts.register("X", "x@thapar.edu", "secret1", null, null, null));
    }

    @Test
    void pollAggregatesDoNotExposeIndividualAnswers() {
        long pollId = PollService.create(ucs301, "MST marks?", 24);
        assertEquals("MST marks?", Poll.all().getFirst().question());

        // fewer than 5 answers: admin gets only the count
        PollService.respond(id("kabir@thapar.edu"), pollId, 44.44);
        PollService.Summary early = PollService.summarize(pollId);
        assertTrue(early.hidden());
        assertEquals(1, early.responses());
        assertEquals(0, early.average());

        assertThrows(AppException.class, () -> PollService.respond(id("kabir@thapar.edu"), pollId, 10.0));   // once only
        PollService.respond(id("riya@thapar.edu"), pollId, 52.52);
        PollService.respond(id("aarav@thapar.edu"), pollId, 60.0);
        PollService.respond(id("anirudh_be_24@thapar.edu"), pollId, 70.0);
        Accounts.register("Poll Person", "p1@thapar.edu", "secret1", null, null, null);
        PollService.respond(id("p1@thapar.edu"), pollId, 80.0);

        // 44.44, 52.52, 60, 70, 80 => average 61.392
        PollService.Summary summary = PollService.summarize(pollId);
        assertFalse(summary.hidden());
        assertEquals(61.392, summary.average(), 1e-9);
        assertFalse(summary.toString().contains("52.52"), "individual answers must not appear");
        assertFalse(summary.toString().contains("Riya"));
    }

    @Test
    void adminStatisticsAreHiddenWhileASubjectHasFewerThanFiveResponses() {
        MarksService.submit(id("aarav@thapar.edu"), ucs301, total(80.5));
        MarksService.submit(id("riya@thapar.edu"), ucs301, total(50.0));

        String line = Analytics.csv().lines().filter(l -> l.startsWith("UCS301")).findFirst().orElseThrow();
        assertTrue(line.contains(",2,,,"), line);      // response count only, rest blank
        assertFalse(line.contains("80.5"));
        assertTrue(Analytics.build().subjectStats().stream().filter(s -> s.code().equals("UCS301")).findFirst().orElseThrow().hidden());

        // while the student still sees class statistics (including their own contribution)
        assertEquals(65.25, SubjectStats.of(Subject.get(ucs301)).average(), 1e-9);
    }

    /** Structural guard: the admin screens must not be able to reach individual marks at all. */
    @Test
    void adminScreensNeverUseTheMarksTable() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/thapar/gpf/screens/AdminScreens.java"));
        assertFalse(source.matches("(?s).*\\bMark\\b.*"), "AdminScreens must not use the Mark table");
        assertFalse(source.contains("MarksService"), "AdminScreens must not use MarksService");
    }

    @Test
    void studentCanUpdateTheirProfile() {
        Accounts.login("aarav@thapar.edu", "student123");
        Accounts.updateProfile("102403123", "COE", 3);
        assertEquals(3, User.byEmail("aarav@thapar.edu").orElseThrow().semester());
        assertEquals(3, Accounts.current().semester());
        assertThrows(AppException.class, () -> Accounts.updateProfile("bad roll!", "COE", 3));
        assertThrows(AppException.class, () -> Accounts.updateProfile("102403123", "COE", 9));
    }

    @Test
    void excelAndPdfExportsAreValidAndSuppressSmallCohorts() throws Exception {
        MarksService.submit(id("aarav@thapar.edu"), ucs301, total(80.5));
        MarksService.submit(id("riya@thapar.edu"), ucs301, total(50.0));

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(Exports.export("xlsx")))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals(Subject.count(), sheet.getLastRowNum());
            var row = StreamSupport.stream(sheet.spliterator(), false)
                    .filter(r -> r.getCell(0).getStringCellValue().equals("UCS301")).findFirst().orElseThrow();
            assertEquals(2.0, row.getCell(2).getNumericCellValue());
            assertNull(row.getCell(3), "Small-cohort averages must be omitted from spreadsheets");
        }
        try (var document = Loader.loadPDF(Exports.export("pdf"))) {
            assertTrue(document.getNumberOfPages() >= 3);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("UCS301"));
            assertFalse(text.contains("80.50"));
            assertFalse(text.contains("Aarav"));
        }
        assertThrows(AppException.class, () -> Exports.export("unknown"));
    }
}
