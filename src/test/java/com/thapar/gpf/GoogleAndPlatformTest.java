package com.thapar.gpf;

import com.thapar.gpf.calculation.CutoffService;
import com.thapar.gpf.calculation.Grade;
import com.thapar.gpf.calculation.MarksService;
import com.thapar.gpf.calculation.PollService;
import com.thapar.gpf.database.Cutoffs;
import com.thapar.gpf.database.Database;
import com.thapar.gpf.database.DemoData;
import com.thapar.gpf.database.Mark;
import com.thapar.gpf.database.Settings;
import com.thapar.gpf.database.Subject;
import com.thapar.gpf.database.User;
import com.thapar.gpf.login.Accounts;
import com.thapar.gpf.login.GoogleLogin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** Production mode: Google identities, the single admin, cutoff history, poll cutoffs and settings. */
class GoogleAndPlatformTest {

    private static final Instant NOW = Instant.ofEpochSecond(1_800_000_000L);

    @BeforeEach
    void freshProductionDatabase() {
        Config.demo = false;
        Config.adminEmail = "admin@thapar.edu";
        Database.open("jdbc:h2:mem:platform" + System.nanoTime(), "sa", "");
        DemoData.seedIfEmpty();
    }

    private static String claims(String email, boolean verified, String hd) {
        return "{\"iss\":\"https://accounts.google.com\",\"aud\":\"client-1\",\"nonce\":\"n-1\",\"exp\":"
                + (NOW.getEpochSecond() + 300) + ",\"sub\":\"google-" + email + "\",\"email\":\"" + email
                + "\",\"email_verified\":" + verified + ",\"hd\":\"" + hd + "\",\"name\":\"Ren\\u00e9 \\\"R\\\" Kumar\"}";
    }

    private static User google(String email) {
        var identity = GoogleLogin.verify(claims(email, true, "thapar.edu"), "client-1", "n-1", NOW);
        return Accounts.googleLogin(identity.email(), identity.name(), identity.subject());
    }

    @Test
    void googleTokenClaimsAreChecked() {
        var identity = GoogleLogin.verify(claims("a@thapar.edu", true, "thapar.edu"), "client-1", "n-1", NOW);
        assertEquals("a@thapar.edu", identity.email());
        assertEquals("René \"R\" Kumar", identity.name());

        assertThrows(AppException.class, () -> GoogleLogin.verify(claims("a@thapar.edu", false, "thapar.edu"), "client-1", "n-1", NOW));
        assertThrows(AppException.class, () -> GoogleLogin.verify(claims("a@gmail.com", true, "gmail.com"), "client-1", "n-1", NOW));
        assertThrows(AppException.class, () -> GoogleLogin.verify(claims("a@thapar.edu", true, "gmail.com"), "client-1", "n-1", NOW));
        assertThrows(AppException.class, () -> GoogleLogin.verify(claims("a@thapar.edu", true, "thapar.edu"), "other-app", "n-1", NOW));
        assertThrows(AppException.class, () -> GoogleLogin.verify(claims("a@thapar.edu", true, "thapar.edu"), "client-1", "replayed", NOW));
        assertThrows(AppException.class, () -> GoogleLogin.verify(claims("a@thapar.edu", true, "thapar.edu"), "client-1", "n-1",
                NOW.plusSeconds(600)));
        assertThrows(AppException.class, () -> GoogleLogin.verify(
                claims("a@thapar.edu", true, "thapar.edu").replace("accounts.google.com", "evil.example"), "client-1", "n-1", NOW));
        assertFalse(Accounts.isThaparEmail("a@evil@thapar.edu"));
        assertFalse(Accounts.isThaparEmail("a@thapar.edu.evil.com"));
    }

    @Test
    void verifiedGoogleIdentityGetsYearAndStableAccount() {
        User student = google("student_be_24@thapar.edu");
        assertEquals(2024, student.admissionYear());
        assertEquals(User.Role.STUDENT, student.role());
        assertNotNull(student.lastLogin());
        assertEquals(student.id(), google("student_be_24@thapar.edu").id());
        assertThrows(AppException.class, () -> Accounts.googleLogin("student_be_24@thapar.edu", "x", "another-google-account"));

        assertEquals(2025, Accounts.admissionYear("name.be_25@thapar.edu"));
        assertEquals(2024, Accounts.admissionYear("be24@thapar.edu"));
        assertNull(Accounts.admissionYear("faculty@thapar.edu"));
    }

    @Test
    void exactlyTheConfiguredEmailCanBeAdmin() {
        assertTrue(google("admin@thapar.edu").isAdmin());
        User other = google("other_be_25@thapar.edu");
        assertFalse(other.isAdmin());
        Database.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", other.id());   // tampered row
        assertFalse(google("other_be_25@thapar.edu").isAdmin());
    }

    @Test
    void historicalYearsAreStoredAndActiveCutoffRefreshesGrades() {
        User student = google("history_be_24@thapar.edu");
        long subject = Subject.allActive().getFirst().id();
        Mark mark = MarksService.submit(student.id(), subject, new MarksService.Submission(80.0, null, null, null, null, null, null));
        assertEquals(Grade.A_MINUS, mark.predictedGrade());

        CutoffService.saveYearRecord(subject, 2023, new Cutoffs(90, 85, 75, 65, 45, 35), false);
        assertEquals(Grade.A_MINUS, Mark.mine(student.id(), mark.id()).orElseThrow().predictedGrade());
        CutoffService.saveYearRecord(subject, 2024, new Cutoffs(90, 85, 75, 65, 45, 35), true);
        assertEquals(2, Cutoffs.history().size());
        assertEquals(Grade.B, Mark.mine(student.id(), mark.id()).orElseThrow().predictedGrade());
        CutoffService.saveYearRecord(subject, 2024, new Cutoffs(85, 75, 65, 55, 45, 35), true);
        assertEquals(2, Cutoffs.history().size());      // same subject and year: updated, not duplicated
        assertEquals(Grade.A_MINUS, Mark.mine(student.id(), mark.id()).orElseThrow().predictedGrade());
        assertThrows(AppException.class, () -> CutoffService.saveYearRecord(subject, 1999, Cutoffs.DEFAULTS, false));
    }

    @Test
    void invalidCutoffsCannotBeStored() {
        assertThrows(AppException.class, () -> CutoffService.validate(new Cutoffs(Double.NaN, 75, 65, 55, 45, 35)));
        assertThrows(AppException.class, () -> CutoffService.validate(new Cutoffs(85, 85, 65, 55, 45, 35)));
        assertThrows(AppException.class, () -> CutoffService.validate(new Cutoffs(85, 75, 65, 55, 45, 20)));
        assertThrows(AppException.class, () -> CutoffService.validate(new Cutoffs(101, 75, 65, 55, 45, 35)));
    }

    @Test
    void manualPollCutoffsDriveOnlyOwnersPredictionAndMaintenanceBlocksSubmissions() {
        User student = google("poll_be_25@thapar.edu");
        long subject = Subject.allActive().getFirst().id();
        long poll = PollService.create(subject, "MST total?", 24);
        assertThrows(AppException.class, () -> PollService.respond(student.id(), poll, Double.NaN));
        PollService.respond(student.id(), poll, 80.0);
        assertEquals(Grade.A_MINUS, PollService.myPrediction(poll, student.id()).orElseThrow().grade());
        PollService.setManualCutoffs(poll, new Cutoffs(90, 85, 75, 65, 45, 35));
        assertEquals(Grade.B, PollService.myPrediction(poll, student.id()).orElseThrow().grade());
        assertTrue(PollService.summarize(poll).hidden());

        Settings.save(true, true, "support@thapar.edu");
        assertThrows(AppException.class, () -> MarksService.submit(student.id(), subject,
                new MarksService.Submission(70.0, null, null, null, null, null, null)));
        Settings.save(false, false, "support@thapar.edu");
        assertThrows(AppException.class, Settings::requireUnlockRequestsOpen);
        assertThrows(AppException.class, () -> Settings.save(false, true, "support@gmail.com"));
    }
}
