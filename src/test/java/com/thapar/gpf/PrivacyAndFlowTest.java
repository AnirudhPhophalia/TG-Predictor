package com.thapar.gpf;

import com.thapar.gpf.ui.AdminController;
import com.thapar.gpf.database.tables.UnlockRequest;
import com.thapar.gpf.database.tables.UnlockStatus;
import com.thapar.gpf.database.tables.Mark;
import com.thapar.gpf.database.repository.UnlockRequestRepository;
import com.thapar.gpf.database.repository.MarkRepository;
import com.thapar.gpf.database.repository.SubjectRepository;
import com.thapar.gpf.database.repository.UserRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Walks through the whole student/admin flow and checks that admins never see marks. */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PrivacyAndFlowTest {

    // an unusual number, so it cannot show up on a page by accident
    private static final String SECRET = "73.37";

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired SubjectRepository subjectRepository;
    @Autowired MarkRepository markRepository;
    @Autowired UnlockRequestRepository unlockRequestRepository;

    private Long subjectId() {
        return subjectRepository.findAllByOrderByCodeAsc().stream()
                .filter(s -> s.getCode().equals("UCS301")).findFirst().orElseThrow().getId();
    }

    private Long userId(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    @Test
    @Order(1)
    void studentSubmitsOnceAndSecondSubmitIsRejectedWith400() throws Exception {
        mvc.perform(post("/student/submit/" + subjectId()).with(user("aarav@thapar.edu").roles("STUDENT")).with(csrf())
                        .param("totalMarks", SECRET).param("mstMarks", "30"))
                .andExpect(status().is3xxRedirection());

        Mark mark = markRepository.findByUserIdAndSubjectId(userId("aarav@thapar.edu"), subjectId()).orElseThrow();
        assertTrue(mark.isLocked());
        assertEquals("B", mark.getPredictedGrade().getLabel());   // 73.37 with default cutoffs (>= 65)

        mvc.perform(post("/student/submit/" + subjectId()).with(user("aarav@thapar.edu").roles("STUDENT")).with(csrf())
                        .param("totalMarks", "99"))
                .andExpect(status().isBadRequest());
        assertEquals(73.37, markRepository.findById(mark.getId()).orElseThrow().getTotalMarks());
    }

    @Test
    @Order(2)
    void studentSeesOwnMarksButNotSomeoneElses() throws Exception {
        Mark mark = markRepository.findByUserIdAndSubjectId(userId("aarav@thapar.edu"), subjectId()).orElseThrow();

        mvc.perform(get("/student/marks/" + mark.getId()).with(user("aarav@thapar.edu").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(SECRET)));

        mvc.perform(get("/student/marks/" + mark.getId()).with(user("riya@thapar.edu").roles("STUDENT")))
                .andExpect(status().isNotFound());

        // UCS301 is a semester 3 subject; aarav is in semester 5, so ask for semester 3 explicitly
        mvc.perform(get("/student").param("semester", "3").with(user("aarav@thapar.edu").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(SECRET)));
    }

    @Test
    @Order(3)
    void unlockFlowWorksAndAdminNeverSeesMarks() throws Exception {
        Mark mark = markRepository.findByUserIdAndSubjectId(userId("aarav@thapar.edu"), subjectId()).orElseThrow();

        mvc.perform(post("/student/marks/" + mark.getId() + "/unlock-request")
                        .with(user("aarav@thapar.edu").roles("STUDENT")).with(csrf())
                        .param("reason", "MST re-evaluated"))
                .andExpect(status().is3xxRedirection());

        UnlockRequest request = unlockRequestRepository.findByUserIdOrderByCreatedAtDesc(userId("aarav@thapar.edu")).get(0);
        assertEquals(UnlockStatus.PENDING, request.getStatus());

        // every admin page, with a pending request and a submitted mark in the system
        for (String path : List.of("/admin", "/admin/users", "/admin/edit-requests", "/admin/analytics",
                "/admin/subjects", "/admin/cutoffs", "/admin/polls", "/admin/export", "/admin/settings")) {
            MvcResult result = mvc.perform(get(path).with(user("admin@thapar.edu").roles("ADMIN")))
                    .andExpect(status().isOk()).andReturn();
            String body = result.getResponse().getContentAsString();
            assertFalse(body.contains(SECRET), path + " leaked a student's marks");
        }

        // admin can see who/why, though
        mvc.perform(get("/admin/edit-requests").with(user("admin@thapar.edu").roles("ADMIN")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MST re-evaluated")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Aarav Sharma")));

        mvc.perform(post("/admin/edit-requests/" + request.getId() + "/approve")
                        .with(user("admin@thapar.edu").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertFalse(markRepository.findById(mark.getId()).orElseThrow().isLocked());

        // student re-enters marks, they are locked again
        mvc.perform(post("/student/submit/" + subjectId()).with(user("aarav@thapar.edu").roles("STUDENT")).with(csrf())
                        .param("totalMarks", "80.5"))
                .andExpect(status().is3xxRedirection());
        Mark updated = markRepository.findById(mark.getId()).orElseThrow();
        assertTrue(updated.isLocked());
        assertEquals(80.5, updated.getTotalMarks());
        assertEquals("A-", updated.getPredictedGrade().getLabel());
    }

    @Test
    @Order(4)
    void rejectedRequestKeepsMarksLocked() throws Exception {
        mvc.perform(post("/student/submit/" + subjectId()).with(user("riya@thapar.edu").roles("STUDENT")).with(csrf())
                .param("totalMarks", "50")).andExpect(status().is3xxRedirection());
        Mark mark = markRepository.findByUserIdAndSubjectId(userId("riya@thapar.edu"), subjectId()).orElseThrow();

        mvc.perform(post("/student/marks/" + mark.getId() + "/unlock-request")
                .with(user("riya@thapar.edu").roles("STUDENT")).with(csrf())
                .param("reason", "typo")).andExpect(status().is3xxRedirection());
        // a second pending request is refused
        mvc.perform(post("/student/marks/" + mark.getId() + "/unlock-request")
                .with(user("riya@thapar.edu").roles("STUDENT")).with(csrf())
                .param("reason", "typo again")).andExpect(status().isBadRequest());

        UnlockRequest request = unlockRequestRepository.findByUserIdOrderByCreatedAtDesc(userId("riya@thapar.edu")).get(0);
        mvc.perform(post("/admin/edit-requests/" + request.getId() + "/reject")
                .with(user("admin@thapar.edu").roles("ADMIN")).with(csrf())
                .param("reason", "No proof")).andExpect(status().is3xxRedirection());

        assertEquals(UnlockStatus.REJECTED, unlockRequestRepository.findById(request.getId()).orElseThrow().getStatus());
        assertTrue(markRepository.findById(mark.getId()).orElseThrow().isLocked());
    }

    @Test
    @Order(5)
    void invalidMarksAreRejectedAndNothingIsStored() throws Exception {
        for (String bad : List.of("150", "-1", "abc", "")) {
            mvc.perform(post("/student/submit/" + subjectId()).with(user("kabir@thapar.edu").roles("STUDENT")).with(csrf())
                            .param("totalMarks", bad))
                    .andExpect(status().isOk())
                    .andExpect(view().name("student/submit"));
        }
        mvc.perform(post("/student/submit/" + subjectId()).with(user("kabir@thapar.edu").roles("STUDENT")).with(csrf())
                        .param("totalMarks", "60").param("estMarks", "31"))
                .andExpect(status().isOk());
        assertTrue(markRepository.findByUserIdAndSubjectId(userId("kabir@thapar.edu"), subjectId()).isEmpty());
    }

    @Test
    @Order(6)
    void rolesAreSeparated() throws Exception {
        mvc.perform(get("/admin").with(user("aarav@thapar.edu").roles("STUDENT"))).andExpect(status().isForbidden());
        mvc.perform(get("/student").with(user("admin@thapar.edu").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/student")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/export")).andExpect(status().is3xxRedirection());
    }

    @Test
    @Order(7)
    void onlyThaparEmailsCanRegister() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("name", "Evil").param("email", "evil@gmail.com")
                        .param("password", "secret1"))
                .andExpect(status().isOk()).andExpect(view().name("register"));
        mvc.perform(post("/register").with(csrf()).param("name", "Evil").param("email", "@thapar.edu")
                        .param("password", "secret1"))
                .andExpect(status().isOk()).andExpect(view().name("register"));
        assertFalse(userRepository.existsByEmail("evil@gmail.com"));

        mvc.perform(post("/register").with(csrf()).param("name", "New Student").param("email", "New.Student@Thapar.edu")
                        .param("password", "secret1"))
                .andExpect(status().is3xxRedirection());
        assertTrue(userRepository.existsByEmail("new.student@thapar.edu"));
        assertEquals("STUDENT", userRepository.findByEmail("new.student@thapar.edu").orElseThrow().getRole().name());
    }

    @Test
    @Order(8)
    void formLoginWorksForSeededAdminAndRedirectsByRole() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("email", "admin@thapar.edu").param("password", "admin123"))
                .andExpect(redirectedUrl("/admin"));
        mvc.perform(post("/login").with(csrf()).param("email", "aarav@thapar.edu").param("password", "student123"))
                .andExpect(redirectedUrl("/student"));
        mvc.perform(post("/login").with(csrf()).param("email", "aarav@thapar.edu").param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(post("/login").with(csrf()).param("email", "someone@gmail.com").param("password", "x"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @Order(9)
    void pollAggregatesDoNotExposeIndividualAnswers() throws Exception {
        var admin = user("admin@thapar.edu").roles("ADMIN");
        mvc.perform(post("/admin/polls").with(admin).with(csrf())
                .param("subjectId", String.valueOf(subjectId())).param("question", "MST marks?").param("durationHours", "24"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/student/polls").with(user("kabir@thapar.edu").roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("MST marks?")));

        Long pollId = 1L;

        // fewer than 5 answers: admin gets only the count
        answer("kabir@thapar.edu", pollId, "44.44");
        String early = mvc.perform(get("/admin/polls/" + pollId + "/responses").with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(early.contains("44.44"));
        assertTrue(early.contains("at least 5"));

        // duplicate answer is refused with a flash message, not stored
        answer("kabir@thapar.edu", pollId, "10");

        answer("riya@thapar.edu", pollId, "52.52");
        for (String extra : List.of("p1@thapar.edu", "p2@thapar.edu", "p3@thapar.edu")) {
            var u = new com.thapar.gpf.database.tables.User();
            u.setName("Poll Person " + extra);
            u.setEmail(extra);
            u.setPassword("x");
            userRepository.save(u);
        }
        answer("p1@thapar.edu", pollId, "60");
        answer("p2@thapar.edu", pollId, "70");
        answer("p3@thapar.edu", pollId, "80");

        // 44.44, 52.52, 60, 70, 80 => average 61.4
        String body = mvc.perform(get("/admin/polls/" + pollId + "/responses").with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("61.4"), "average should be shown once 5 students answered");
        assertFalse(body.contains("Kabir"));
        assertFalse(body.contains("Riya"));
        assertFalse(body.contains("Poll Person"));
        assertFalse(body.contains("52.52"), "individual answers must not appear");
    }

    private void answer(String email, Long pollId, String marks) throws Exception {
        mvc.perform(post("/student/polls/" + pollId + "/respond").with(user(email).roles("STUDENT")).with(csrf())
                .param("marks", marks)).andExpect(status().is3xxRedirection());
    }

    @Test
    @Order(11)
    void adminStatisticsAreHiddenWhileASubjectHasFewerThanFiveResponses() throws Exception {
        // UCS301 has only 2 submissions in this test run (aarav, riya)
        String csv = mvc.perform(get("/admin/export").with(user("admin@thapar.edu").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String ucs301 = csv.lines().filter(l -> l.startsWith("UCS301")).findFirst().orElseThrow();
        assertTrue(ucs301.contains(",2,,,"), ucs301);   // response count only, rest blank
        assertFalse(ucs301.contains("80.5"));
        assertFalse(ucs301.contains("50"));

        // while the student still sees class statistics (including their own contribution)
        Mark mark = markRepository.findByUserIdAndSubjectId(userId("aarav@thapar.edu"), subjectId()).orElseThrow();
        mvc.perform(get("/student/marks/" + mark.getId()).with(user("aarav@thapar.edu").roles("STUDENT")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("65.25")));   // (80.5 + 50) / 2
    }

    /** Structural guard: the admin controller must not be able to reach individual marks at all. */
    @Test
    @Order(10)
    void adminControllerHasNoDependencyOnMarks() {
        List<Class<?>> used = new ArrayList<>();
        for (Constructor<?> c : AdminController.class.getDeclaredConstructors()) used.addAll(List.of(c.getParameterTypes()));
        for (Field f : AdminController.class.getDeclaredFields()) used.add(f.getType());
        for (Method m : AdminController.class.getDeclaredMethods()) {
            used.add(m.getReturnType());
            used.addAll(List.of(m.getParameterTypes()));
        }
        for (Class<?> type : used) {
            assertNotEquals("MarksService", type.getSimpleName());
            assertNotEquals("MarkRepository", type.getSimpleName());
            assertNotEquals("Mark", type.getSimpleName());
        }
    }

    @Test
    @Order(12)
    void studentCanUseProfileSearchAndSemesterFilters() throws Exception {
        var student = user("aarav@thapar.edu").roles("STUDENT");
        mvc.perform(get("/student/profile").with(student)).andExpect(status().isOk());
        mvc.perform(get("/student/help").with(student)).andExpect(status().isOk());
        mvc.perform(get("/student/subjects").with(student).param("search", "Data Structures").param("semester", "0"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("UCS301")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("UEC501"))));
        mvc.perform(post("/student/profile").with(student).with(csrf())
                        .param("rollNumber", "102403123").param("branch", "COE").param("semester", "3"))
                .andExpect(redirectedUrl("/student/profile"));
        assertEquals(3, userRepository.findByEmail("aarav@thapar.edu").orElseThrow().getSemester());
        mvc.perform(get("/student").with(student).param("semester", "7"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(SECRET))));
    }

    @Test
    @Order(13)
    void excelAndPdfExportsAreValidAndSuppressPrivateSmallCohorts() throws Exception {
        var admin = user("admin@thapar.edu").roles("ADMIN");
        byte[] excel = mvc.perform(get("/admin/export").param("format", "excel").with(admin))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn().getResponse().getContentAsByteArray();
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(excel))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals(subjectRepository.count(), sheet.getLastRowNum());
            var rows = java.util.stream.StreamSupport.stream(sheet.spliterator(), false);
            var subject = rows.filter(r -> r.getCell(0).getStringCellValue().equals("UCS301")).findFirst().orElseThrow();
            assertEquals(2.0, subject.getCell(2).getNumericCellValue());
            assertNull(subject.getCell(3), "Small-cohort averages must be omitted from spreadsheets");
        }
        byte[] pdf = mvc.perform(get("/admin/export").param("format", "pdf").with(admin))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        try (var document = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            assertTrue(document.getNumberOfPages() >= 3);
            String text = new org.apache.pdfbox.text.PDFTextStripper().getText(document);
            assertTrue(text.contains("UCS301"));
            assertFalse(text.contains("80.50"));
            assertFalse(text.contains("Aarav"));
        }
        mvc.perform(get("/admin/export").param("format", "unknown").with(admin)).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/export").param("format", "pdf").with(user("aarav@thapar.edu").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(14)
    void longListsArePagedAndKeepTheirPageAndFilters() throws Exception {
        var admin = user("admin@thapar.edu").roles("ADMIN");
        var codes = subjectRepository.findAllByOrderByCodeAsc().stream().map(s -> s.getCode()).toList();
        int pages = (codes.size() + 14) / 15;

        String first = mvc.perform(get("/admin/subjects").with(admin)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(first.contains(">" + codes.get(0) + "<"));
        assertFalse(first.contains(">" + codes.get(15) + "<"), "page 1 shows only 15 subjects");
        assertTrue(first.contains("Page 1 of " + pages));

        String second = mvc.perform(get("/admin/subjects").param("page", "1").with(admin))
                .andReturn().getResponse().getContentAsString();
        assertTrue(second.contains(">" + codes.get(15) + "<"));
        assertFalse(second.contains(">" + codes.get(0) + "<"));

        // a page past the end shows the last page
        mvc.perform(get("/admin/subjects").param("page", "999").with(admin))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Page " + pages + " of " + pages)));

        // an action on page 3 returns to page 3
        Long id = subjectRepository.findAllByOrderByCodeAsc().get(30).getId();
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/admin/subjects/" + id + "/toggle").param("page", "2").with(admin).with(csrf()))
                    .andExpect(redirectedUrl("/admin/subjects?page=2"));
        }

        // both lists on the cutoffs page have their own pager
        mvc.perform(get("/admin/cutoffs").param("page", "3").with(admin)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("historyPage=0&amp;page=4")));

        // the student's search and semester filter survive moving to the next page
        mvc.perform(get("/student/subjects").param("search", "U").param("semester", "0")
                        .with(user("aarav@thapar.edu").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("search=U&amp;semester=0&amp;page=1")));

        // without a semester in the address, the current semester (3, set in test 12) comes first, then 2, 1, 4...
        mvc.perform(get("/student/subjects").with(user("aarav@thapar.edu").roles("STUDENT")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("UCS301")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("UML501"))));
        for (String path : List.of("/admin/users", "/admin/edit-requests", "/admin/polls", "/admin/analytics?page=3")) {
            mvc.perform(get(path).with(admin)).andExpect(status().isOk());
        }
    }
}
