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
        if (userRepository.count() > 0) {
            return;
        }

        // admin and sample students (passwords are for local demo only)
        saveUser("Admin", "admin@thapar.edu", "admin123", Role.ADMIN, null, null, null);
        saveUser("Aarav Sharma", "aarav@thapar.edu", "student123", Role.STUDENT, "102203001", "COE", 5);
        saveUser("Riya Verma", "riya@thapar.edu", "student123", Role.STUDENT, "102203002", "COE", 5);
        saveUser("Kabir Singh", "kabir@thapar.edu", "student123", Role.STUDENT, "102203003", "ECE", 5);

        Subject ds = subjectService.create(new SubjectRequest("UCS301", "Data Structures", 3, "COE", 4));
        subjectService.create(new SubjectRequest("UEC501", "Digital Signal Processing", 5, "ECE", 4));
        subjectService.create(new SubjectRequest("UCS503", "Software Engineering", 5, "COE", 3));

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
        userRepository.save(user);
    }
}
