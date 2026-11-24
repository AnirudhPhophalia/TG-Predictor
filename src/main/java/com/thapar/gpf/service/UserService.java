package com.thapar.gpf.service;

import com.thapar.gpf.dto.RegisterRequest;
import com.thapar.gpf.dto.UserSummary;
import com.thapar.gpf.exception.BadRequestException;
import com.thapar.gpf.exception.ResourceNotFoundException;
import com.thapar.gpf.model.Role;
import com.thapar.gpf.model.User;
import com.thapar.gpf.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;

@Service
public class UserService {

    public static final String ALLOWED_DOMAIN = "@thapar.edu";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       @Value("${gpf.admin.email:admin@thapar.edu}") String adminEmail) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (!isThaparEmail(this.adminEmail)) throw new IllegalArgumentException("Admin must use @thapar.edu.");
    }

    public static boolean isThaparEmail(String email) {
        return email != null && email.toLowerCase(Locale.ROOT)
                .matches("[a-z0-9][a-z0-9._+\\-]*@thapar\\.edu");
    }

    public static Integer admissionYear(String email) {
        if (!isThaparEmail(email)) return null;
        String local = email.toLowerCase(Locale.ROOT).split("@")[0];
        var match = Pattern.compile("(?:^|[._-])be[_-]?(\\d{2})(?!\\d)").matcher(local);
        return match.find() ? 2000 + Integer.parseInt(match.group(1)) : null;
    }

    public boolean isConfiguredAdmin(String email) { return adminEmail.equalsIgnoreCase(email); }

    @Transactional
    public User googleLogin(String email, String name, String subject) {
        if (!isThaparEmail(email)) throw new BadRequestException("An institute email is required.");
        String normalized = email.toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalized).orElseGet(User::new);
        if (user.getGoogleSubject() != null && !user.getGoogleSubject().equals(subject)) {
            throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("identity_mismatch");
        }
        user.setEmail(normalized);
        user.setName(name == null || name.isBlank() ? normalized.split("@")[0] : name);
        if (user.getPassword() == null) user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setGoogleSubject(subject);
        user.setAdmissionYear(admissionYear(normalized));
        user.setRole(isConfiguredAdmin(normalized) ? Role.ADMIN : Role.STUDENT);
        user.setLastLogin(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public void updateProfile(User user, String rollNumber, String branch, Integer semester) {
        if (semester == null || semester < 1 || semester > 8 || branch == null || branch.isBlank()
                || branch.length() > 50 || rollNumber == null || !rollNumber.matches("[A-Za-z0-9-]{1,30}")) {
            throw new BadRequestException("Enter a roll number, branch and semester between 1 and 8.");
        }
        user.setRollNumber(rollNumber.trim());
        user.setBranch(branch.trim());
        user.setSemester(semester);
        userRepository.save(user);
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public User register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (!isThaparEmail(email)) {
            throw new BadRequestException("Only official @thapar.edu email addresses can register.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists.");
        }

        User user = new User();
        user.setEmail(email);
        user.setName(request.name().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRollNumber(request.rollNumber());
        user.setBranch(request.branch());
        user.setSemester(request.semester());
        user.setRole(Role.STUDENT);   // self-registration always creates a student
        if (isConfiguredAdmin(email)) throw new BadRequestException("Use Google login for the administrator account.");
        user.setAdmissionYear(admissionYear(email));
        return userRepository.save(user);
    }

    public org.springframework.data.domain.Page<UserSummary> findSummaries(org.springframework.data.domain.Pageable page) {
        return userRepository.findAll(page)
                .map(u -> new UserSummary(u.getId(), u.getName(), u.getEmail(), u.getRollNumber(),
                        u.getBranch(), u.getSemester(), u.getRole(), u.getCreatedAt()));
    }

    @Transactional
    public void changeRole(Long userId, Role newRole, String actingAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Role allowed = isConfiguredAdmin(user.getEmail()) ? Role.ADMIN : Role.STUDENT;
        if (newRole != allowed) {
            throw new BadRequestException("Only the configured administrator email can have the ADMIN role.");
        }
        user.setRole(newRole);
        userRepository.save(user);
    }
}
