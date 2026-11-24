package com.thapar.gpf.database;

import com.thapar.gpf.Config;
import com.thapar.gpf.calculation.MarksService;
import com.thapar.gpf.login.Accounts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Fills an empty database on first start: the curriculum from data/subjects.tsv (always) and,
 * in demo mode only, sample accounts plus 120 synthetic UCS301 students so relative grading
 * (100+ submissions) can be tried straight away.
 */
public final class DemoData {

    private DemoData() {
    }

    public static void seedIfEmpty() {
        if (Subject.count() > 0) return;
        Database.inTransaction(() -> {
            loadSubjects();
            if (!Config.demo) return;
            account("Administrator", Config.adminEmail, "admin123", User.Role.ADMIN, null, null, null);
            account("Aarav Sharma", "aarav@thapar.edu", "student123", User.Role.STUDENT, "102203001", "COE", 5);
            account("Riya Verma", "riya@thapar.edu", "student123", User.Role.STUDENT, "102203002", "COE", 5);
            account("Kabir Singh", "kabir@thapar.edu", "student123", User.Role.STUDENT, "102203003", "ECE", 5);
            account("Anirudh Phophalia", "anirudh_be_24@thapar.edu", "student123", User.Role.STUDENT, "102403001", "COE", 3);
            seedCohort(Subject.byCode("UCS301"));
        });
    }

    private static void loadSubjects() {
        var file = DemoData.class.getResourceAsStream("/data/subjects.tsv");
        if (file == null) throw new IllegalStateException("data/subjects.tsv is missing");
        try (var reader = new BufferedReader(new InputStreamReader(file, StandardCharsets.UTF_8))) {
            reader.lines().skip(1).forEach(line -> {
                String[] v = line.split("\t");      // code, name, semester, branch, credits
                Subject.create(v[0], v[1], Integer.parseInt(v[2]), v[3], Double.parseDouble(v[4]));
            });
        } catch (IOException e) {
            throw new IllegalStateException("Could not load curriculum subjects", e);
        }
    }

    private static void seedCohort(Subject subject) {
        String hash = Accounts.hash("student123");        // hash once, reuse for every synthetic student
        Random random = new Random(42);
        for (int i = 1; i <= Config.demoCohortSize; i++) {
            long id = User.insert(String.format("demo%03d@thapar.edu", i), "Demo Student " + i, hash,
                    null, "COE", 3, null, User.Role.STUDENT);
            double marks = Math.round((58 + random.nextGaussian() * 15) * 2) / 2.0;
            marks = Math.max(0, Math.min(100, marks));
            MarksService.submit(id, subject.id(), new MarksService.Submission(marks, null, null, null, null, null, null));
        }
    }

    private static void account(String name, String email, String password, User.Role role,
                                String rollNumber, String branch, Integer semester) {
        User.insert(email, name, Accounts.hash(password), rollNumber, branch, semester, Accounts.admissionYear(email), role);
    }
}
