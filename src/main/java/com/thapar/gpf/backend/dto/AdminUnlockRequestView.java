package com.thapar.gpf.backend.dto;

import com.thapar.gpf.database.tables.UnlockStatus;

import java.time.LocalDateTime;

/** What an admin sees of an unlock request: who, which subject, why. Never the marks. */
public record AdminUnlockRequestView(
        Long id,
        String studentName,
        String rollNumber,
        String subjectCode,
        String subjectName,
        String reason,
        UnlockStatus status,
        String rejectionReason,
        LocalDateTime createdAt) {
}
