package com.thapar.gpf;

import com.thapar.gpf.login.GoogleLoginService;
import com.thapar.gpf.backend.dto.CutoffRequest;
import com.thapar.gpf.backend.dto.MarksSubmissionRequest;
import com.thapar.gpf.backend.exception.BadRequestException;
import com.thapar.gpf.database.tables.Grade;
import com.thapar.gpf.database.tables.Role;
import com.thapar.gpf.database.repository.PastYearCutoffRepository;
import com.thapar.gpf.database.repository.UserRepository;
import com.thapar.gpf.backend.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"gpf.demo.enabled=false", "gpf.google.enabled=true",
        "GOOGLE_CLIENT_ID=test-client", "GOOGLE_CLIENT_SECRET=test-secret",
        "spring.datasource.url=jdbc:h2:mem:google-platform-test;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
@Transactional
class GoogleAndPlatformTest {
    @Autowired GoogleLoginService google;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired SubjectService subjects;
    @Autowired MarksService marks;
    @Autowired PastYearCutoffService history;
    @Autowired PastYearCutoffRepository records;
    @Autowired PollService polls;
    @Autowired SettingsService settings;
    @Autowired MockMvc mvc;

    private OidcUser identity(String email, boolean verified, String hd) {
        return new DefaultOidcUser(List.of(), new OidcIdToken("test", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("sub", "google-" + email, "email", email, "name", "Institute Student",
                        "email_verified", verified, "hd", hd)), "email");
    }

    @Test
    void verifiedGoogleIdentityGetsYearAndStableAccount() {
        var result = google.provision(identity("student_be_24@thapar.edu", true, "thapar.edu"));
        assertEquals("student_be_24@thapar.edu", result.getName());
        var student = users.getByEmail(result.getName());
        assertEquals(2024, student.getAdmissionYear());
        assertEquals(Role.STUDENT, student.getRole());
        google.provision(identity(result.getName(), true, "thapar.edu"));
        assertEquals(student.getId(), users.getByEmail(result.getName()).getId());
        assertNotNull(student.getLastLogin());
        assertEquals(2025, UserService.admissionYear("name.be_25@thapar.edu"));
        assertNull(UserService.admissionYear("faculty@thapar.edu"));
    }

    @Test
    void semesterFollowsAdmissionYearAndDate() {
        assertEquals(1, UserService.semesterOn(2024, java.time.LocalDate.of(2024, 8, 1)));
        assertEquals(2, UserService.semesterOn(2024, java.time.LocalDate.of(2025, 1, 15)));
        assertEquals(5, UserService.semesterOn(2024, java.time.LocalDate.of(2026, 11, 24)));
        assertEquals(6, UserService.semesterOn(2024, java.time.LocalDate.of(2027, 6, 30)));
        assertEquals(8, UserService.semesterOn(2024, java.time.LocalDate.of(2028, 3, 1)));
        assertNull(UserService.semesterOn(2024, java.time.LocalDate.of(2024, 3, 1)));
        assertNull(UserService.semesterOn(2024, java.time.LocalDate.of(2028, 9, 1)));
        assertNull(UserService.semesterOn(null, java.time.LocalDate.of(2026, 11, 24)));
    }

    @Test
    void studentCannotOpenOrSubmitMarksForFutureSemesterSubjects() throws Exception {
        int admissionYear = LocalDate.now().getYear() - 2;
        String email = String.format("future_be_%02d@thapar.edu", admissionYear % 100);
        google.provision(identity(email, true, "thapar.edu"));
        var student = users.getByEmail(email);
        Integer currentSemester = UserService.currentSemester(student);
        var futureSubject = subjects.findActive().stream()
                .filter(subject -> subject.getSemester() != null && subject.getSemester() > currentSemester)
                .findFirst().orElseThrow();

        mvc.perform(get("/student/submit/" + futureSubject.getId()).with(user(email).roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student"));
        mvc.perform(post("/student/submit/" + futureSubject.getId()).with(user(email).roles("STUDENT")).with(csrf())
                        .param("totalMarks", "80"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student"));

        assertTrue(marks.findMyMarkForSubject(student.getId(), futureSubject.getId()).isEmpty());
    }

    @Test
    void googleRejectsUnverifiedExternalAndSpoofedInstituteAccounts() {
        assertThrows(OAuth2AuthenticationException.class, () -> google.provision(identity("a@thapar.edu", false, "thapar.edu")));
        assertThrows(OAuth2AuthenticationException.class, () -> google.provision(identity("a@gmail.com", true, "gmail.com")));
        assertThrows(OAuth2AuthenticationException.class, () -> google.provision(identity("a@thapar.edu", true, "gmail.com")));
        assertFalse(UserService.isThaparEmail("a@evil@thapar.edu"));
        assertFalse(UserService.isThaparEmail("a@thapar.edu.evil.com"));
    }

    @Test
    void exactlyConfiguredEmailCanBeAdmin() {
        google.provision(identity("admin@thapar.edu", true, "thapar.edu"));
        google.provision(identity("other_be_25@thapar.edu", true, "thapar.edu"));
        assertEquals(Role.ADMIN, users.getByEmail("admin@thapar.edu").getRole());
        var other = users.getByEmail("other_be_25@thapar.edu");
        assertThrows(BadRequestException.class, () -> users.changeRole(other.getId(), Role.ADMIN, "admin@thapar.edu"));
        other.setRole(Role.ADMIN);
        userRepository.saveAndFlush(other);
        google.provision(identity(other.getEmail(), true, "thapar.edu"));
        assertEquals(Role.STUDENT, users.getByEmail(other.getEmail()).getRole());
    }

    @Test
    void productionOnlyExposesGoogleLoginAndKeepsCsrfProtection() throws Exception {
        mvc.perform(get("/register")).andExpect(status().isNotFound());
        mvc.perform(post("/login").with(csrf()).param("email", "admin@thapar.edu").param("password", "admin123"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/oauth2/authorization/google")).andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://accounts.google.com/o/oauth2/v2/auth")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("state=")));
        mvc.perform(post("/admin/settings").with(user("admin@thapar.edu").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void historicalYearsAreStoredAndActiveCutoffRefreshesGrades() {
        google.provision(identity("history_be_24@thapar.edu", true, "thapar.edu"));
        var student = users.getByEmail("history_be_24@thapar.edu");
        var subject = subjects.findActive().getFirst();
        var mark = marks.submit(student, subject.getId(), new MarksSubmissionRequest(80.0,null,null,null,null,null,null));
        assertEquals(Grade.A_MINUS, mark.getPredictedGrade());
        history.save(subject.getId(), 2023, new CutoffRequest(90.0,85.0,75.0,65.0,45.0,35.0), false);
        assertEquals(Grade.A_MINUS, marks.getMyMark(mark.getId(), student.getId()).getPredictedGrade());
        history.save(subject.getId(), 2024, new CutoffRequest(90.0,85.0,75.0,65.0,45.0,35.0), true);
        assertEquals(2, records.findAll().size());
        assertEquals(Grade.B, marks.getMyMark(mark.getId(), student.getId()).getPredictedGrade());
        history.save(subject.getId(), 2024, new CutoffRequest(85.0,75.0,65.0,55.0,45.0,35.0), true);
        assertEquals(2, records.findAll().size());
        assertEquals(Grade.A_MINUS, marks.getMyMark(mark.getId(), student.getId()).getPredictedGrade());
    }

    @Test
    void invalidCutoffsCannotBeStored() {
        assertThrows(BadRequestException.class, () -> PastYearCutoffService.validate(new CutoffRequest(Double.NaN,75.0,65.0,55.0,45.0,35.0)));
        assertThrows(BadRequestException.class, () -> PastYearCutoffService.validate(new CutoffRequest(85.0,85.0,65.0,55.0,45.0,35.0)));
        assertThrows(BadRequestException.class, () -> PastYearCutoffService.validate(new CutoffRequest(85.0,75.0,65.0,55.0,45.0,20.0)));
    }

    @Test
    void manualPollCutoffsDriveOnlyOwnersPredictionAndMaintenanceBlocksSubmissions() {
        google.provision(identity("poll_be_25@thapar.edu", true, "thapar.edu"));
        var student = users.getByEmail("poll_be_25@thapar.edu");
        var subject = subjects.findActive().getFirst();
        var poll = polls.create(subject.getId(), "MST total?", 24);
        assertThrows(BadRequestException.class, () -> polls.respond(student, poll.getId(), Double.NaN));
        polls.respond(student, poll.getId(), 80.0);
        polls.updateCutoffs(poll.getId(), new CutoffRequest(90.0,85.0,75.0,65.0,45.0,35.0));
        assertEquals(Grade.B, polls.myPrediction(poll.getId(), student.getId()).orElseThrow().grade());
        assertTrue(polls.summarize(poll.getId()).hidden());
        settings.save(true, true, "support@thapar.edu");
        assertThrows(BadRequestException.class, () -> marks.submit(student, subject.getId(), new MarksSubmissionRequest(70.0,null,null,null,null,null,null)));
        settings.save(false, false, "support@thapar.edu");
        assertThrows(BadRequestException.class, settings::requireUnlockRequestsOpen);
    }
}
