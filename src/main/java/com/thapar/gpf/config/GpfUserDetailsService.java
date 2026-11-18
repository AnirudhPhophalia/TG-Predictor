package com.thapar.gpf.config;

import com.thapar.gpf.model.User;
import com.thapar.gpf.repository.UserRepository;
import com.thapar.gpf.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GpfUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserService users;

    public GpfUserDetailsService(UserRepository userRepository, UserService users) {
        this.userRepository = userRepository;
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email == null ? "" : email.trim().toLowerCase();

        // domain restriction: only @thapar.edu accounts can ever log in
        if (!UserService.isThaparEmail(normalized)) {
            throw new UsernameNotFoundException("Only @thapar.edu accounts are allowed");
        }

        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + normalized));

        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(users.isConfiguredAdmin(user.getEmail()) ? "ADMIN" : "STUDENT")
                .build();
    }
}
