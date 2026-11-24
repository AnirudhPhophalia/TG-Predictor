package com.thapar.gpf.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Everything on the admin dashboard / analytics pages. Aggregates and timestamps only. */
public record AnalyticsReport(
        long totalUsers,
        long totalStudents,
        long totalAdmins,
        long totalSubjects,
        long activeSubjects,
        long totalSubmissions,
        long pendingRequests,
        long openPolls,
        double overallPassPercentage,
        int[] gradeTotals,                    // A, A-, B, B-, C, D, E
        List<SubjectStatDTO> subjectStats,
        List<SubjectStatDTO> topSubjects,
        List<LocalDateTime> recentSubmissionTimes) {
}
