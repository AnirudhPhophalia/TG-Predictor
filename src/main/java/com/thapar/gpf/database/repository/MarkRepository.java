package com.thapar.gpf.database.repository;

import com.thapar.gpf.database.tables.Mark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * PRIVACY: only MarksService (student-facing, scoped to the logged-in student) and
 * AnalyticsService (counts and timestamps only) may use this repository.
 * Controllers under /admin must never depend on it.
 */
public interface MarkRepository extends JpaRepository<Mark, Long> {

    // Only called with the id of the authenticated student.
    List<Mark> findByUserId(Long userId);

    Optional<Mark> findByUserIdAndSubjectId(Long userId, Long subjectId);

    Optional<Mark> findByIdAndUserId(Long id, Long userId);

    // System use: recalculating percentiles / grades for a subject.
    List<Mark> findBySubjectId(Long subjectId);

    // Anonymous scores only - no user information.
    @Query("select m.totalMarks from Mark m where m.subject.id = :subjectId")
    List<Double> findTotalMarksBySubjectId(Long subjectId);

    // Timestamps only.
    @Query("select m.submittedAt from Mark m order by m.submittedAt desc")
    List<LocalDateTime> findRecentSubmissionTimes(Pageable pageable);
}
