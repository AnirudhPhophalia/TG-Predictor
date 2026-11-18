package com.thapar.gpf.dto;

import com.thapar.gpf.model.EditStatus;

import java.time.LocalDateTime;

/** What an admin sees of an unlock request: who, which subject, why. Never the marks. */
public record AdminEditRequestView(
        Long id,
        String studentName,
        String rollNumber,
        String subjectCode,
        String subjectName,
        String reason,
        EditStatus status,
        String rejectionReason,
        LocalDateTime createdAt) {
}
