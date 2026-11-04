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

import java.util.List;

@Service
public class UserService {

    public static final String ALLOWED_DOMAIN = "@thapar.edu";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static boolean isThaparEmail(String email) {
        return email != null
                && email.toLowerCase().endsWith(ALLOWED_DOMAIN)
                && email.length() > ALLOWED_DOMAIN.length()
                && !email.contains(" ");
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
        return userRepository.save(user);
    }

    public List<UserSummary> findAllSummaries() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(u -> new UserSummary(u.getId(), u.getName(), u.getEmail(), u.getRollNumber(),
                        u.getBranch(), u.getSemester(), u.getRole(), u.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void changeRole(Long userId, Role newRole, String actingAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getEmail().equalsIgnoreCase(actingAdminEmail)) {
            throw new BadRequestException("You cannot change your own role.");
        }
        user.setRole(newRole);
        userRepository.save(user);
    }
}
