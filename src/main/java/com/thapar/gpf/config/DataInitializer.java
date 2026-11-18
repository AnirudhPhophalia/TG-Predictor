package com.thapar.gpf.config;

import com.thapar.gpf.dto.MarksSubmissionRequest;
import com.thapar.gpf.dto.SubjectRequest;
import com.thapar.gpf.model.Role;
import com.thapar.gpf.model.Subject;
import com.thapar.gpf.model.User;
import com.thapar.gpf.repository.UserRepository;
import com.thapar.gpf.service.MarksService;
import com.thapar.gpf.service.SubjectService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/** Seeds sample data on first start (H2 in-memory starts empty every time). */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SubjectService subjectService;
    private final MarksService marksService;
    private final PasswordEncoder passwordEncoder;
    private final int demoCohortSize;
    @Value("${gpf.demo.enabled:false}") private boolean demoEnabled;
    @Value("${gpf.admin.email:admin@thapar.edu}") private String adminEmail;

    public DataInitializer(UserRepository userRepository, SubjectService subjectService,
                           MarksService marksService, PasswordEncoder passwordEncoder,
                           @Value("${gpf.seed.demo-cohort:120}") int demoCohortSize) {
        this.userRepository = userRepository;
        this.subjectService = subjectService;
        this.marksService = marksService;
        this.passwordEncoder = passwordEncoder;
        this.demoCohortSize = demoCohortSize;
    }

    @Override
    public void run(String... args) {
        if (!subjectService.findAll().isEmpty()) return;
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                new org.springframework.core.io.ClassPathResource("data/subjects.tsv").getInputStream(),
                java.nio.charset.StandardCharsets.UTF_8))) {
            reader.lines().skip(1).forEach(line -> {
                String[] value = line.split("\\t");
                subjectService.create(new SubjectRequest(value[0], value[1], Integer.parseInt(value[2]),
                        value[3], Double.parseDouble(value[4])));
            });
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not load curriculum subjects", e);
        }
        Subject ds = subjectService.findAll().stream().filter(s -> s.getCode().equals("UCS301")).findFirst().orElseThrow();
        if (!demoEnabled) return;
        saveUser("Administrator", adminEmail, "admin123", Role.ADMIN, null, null, null);
        saveUser("Aarav Sharma", "aarav@thapar.edu", "student123", Role.STUDENT, "102203001", "COE", 5);
        saveUser("Riya Verma", "riya@thapar.edu", "student123", Role.STUDENT, "102203002", "COE", 5);
        saveUser("Kabir Singh", "kabir@thapar.edu", "student123", Role.STUDENT, "102203003", "ECE", 5);
        saveUser("Anirudh Phophalia", "anirudh_be_24@thapar.edu", "student123", Role.STUDENT, "102403001", "COE", 3);
        seedDemoCohort(ds);
    }

    // A fake cohort for UCS301 so the relative (>= 100 submissions) mode can be tried right away.
    // Set gpf.seed.demo-cohort=0 to skip it.
    private void seedDemoCohort(Subject subject) {
        if (demoCohortSize <= 0) {
            return;
        }
        String hash = passwordEncoder.encode("student123");   // encode once, reuse for all demo users
        Random random = new Random(42);
        for (int i = 1; i <= demoCohortSize; i++) {
            User user = new User();
            user.setName("Demo Student " + i);
            user.setEmail(String.format("demo%03d@thapar.edu", i));
            user.setPassword(hash);
            user.setRole(Role.STUDENT);
            user.setBranch("COE");
            user.setSemester(3);
            userRepository.save(user);

            double marks = Math.round((58 + random.nextGaussian() * 15) * 2) / 2.0;
            marks = Math.max(0, Math.min(100, marks));
            marksService.submit(user, subject.getId(),
                    new MarksSubmissionRequest(marks, null, null, null, null, null, null));
        }
    }

    private void saveUser(String name, String email, String password, Role role,
                          String roll, String branch, Integer semester) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setRollNumber(roll);
        user.setBranch(branch);
        user.setSemester(semester);
        user.setAdmissionYear(com.thapar.gpf.service.UserService.admissionYear(email));
        userRepository.save(user);
    }
}
