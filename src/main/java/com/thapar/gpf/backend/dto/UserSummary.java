package com.thapar.gpf.backend.dto;

import com.thapar.gpf.database.tables.Role;

import java.time.LocalDateTime;

/** User metadata for the admin user list (no password, no marks). */
public record UserSummary(
        Long id,
        String name,
        String email,
        String rollNumber,
        String branch,
        Integer semester,
        Role role,
        LocalDateTime createdAt) {
}
