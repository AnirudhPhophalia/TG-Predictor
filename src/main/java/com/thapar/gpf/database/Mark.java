package com.thapar.gpf.database;

import com.thapar.gpf.calculation.Grade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Table "marks": one student's marks in one subject (unique per student and subject).
 *
 * PRIVACY: reads of whole rows always take the logged-in student's id, so a student can only
 * load their own marks. Everything else gets anonymous scores, counts or timestamps only.
 * The admin screens never use this class (a test checks that).
 */
public record Mark(long id, long userId, long subjectId, double totalMarks, Double estMarks, Double mstMarks,
                   Double quizMarks, Double assignmentMarks, Double labMarks, Double practicalMarks,
                   Grade predictedGrade, Double percentile, boolean locked, LocalDateTime submittedAt) {

    public Subject subject() {
        return Subject.get(subjectId);
    }

    private static Mark map(ResultSet rs) throws SQLException {
        String grade = rs.getString("predicted_grade");
        return new Mark(rs.getLong("id"), rs.getLong("user_id"), rs.getLong("subject_id"), rs.getDouble("total_marks"),
                Database.nullableDouble(rs, "est_marks"), Database.nullableDouble(rs, "mst_marks"),
                Database.nullableDouble(rs, "quiz_marks"), Database.nullableDouble(rs, "assignment_marks"),
                Database.nullableDouble(rs, "lab_marks"), Database.nullableDouble(rs, "practical_marks"),
                grade == null ? null : Grade.valueOf(grade), Database.nullableDouble(rs, "percentile"),
                rs.getBoolean("locked"), Database.time(rs, "submitted_at"));
    }

    // ---------- the owner's own marks ----------

    public static List<Mark> mine(long studentId) {
        return Database.list("SELECT * FROM marks WHERE user_id = ? ORDER BY submitted_at DESC", Mark::map, studentId);
    }

    public static Optional<Mark> mine(long studentId, long markId) {
        return Database.one("SELECT * FROM marks WHERE id = ? AND user_id = ?", Mark::map, markId, studentId);
    }

    public static Optional<Mark> mineForSubject(long studentId, long subjectId) {
        return Database.one("SELECT * FROM marks WHERE user_id = ? AND subject_id = ?", Mark::map, studentId, subjectId);
    }

    // ---------- anonymous reads ----------

    /** All scores of a subject with no student information attached. */
    public static List<Double> scores(long subjectId) {
        return Database.list("SELECT total_marks FROM marks WHERE subject_id = ?", rs -> rs.getDouble(1), subjectId);
    }

    public record Score(long markId, double totalMarks) {
    }

    /** Mark ids and scores of a subject, used only by the system to recalculate everyone's grade. */
    public static List<Score> scoresWithIds(long subjectId) {
        return Database.list("SELECT id, total_marks FROM marks WHERE subject_id = ? ORDER BY id",
                rs -> new Score(rs.getLong(1), rs.getDouble(2)), subjectId);
    }

    public static List<LocalDateTime> recentSubmissionTimes(int limit) {
        return Database.list("SELECT submitted_at FROM marks ORDER BY submitted_at DESC FETCH FIRST " + limit + " ROWS ONLY",
                rs -> Database.time(rs, "submitted_at"));
    }

    public static long count() {
        return Database.count("SELECT COUNT(*) FROM marks");
    }

    // ---------- writes ----------

    /** How new marks are taken in: insert a new row, or overwrite an unlocked one. Either way it is locked again. */
    public static long store(Long existingId, long studentId, long subjectId, double total, Double est, Double mst,
                             Double quiz, Double assignment, Double lab, Double practical) {
        LocalDateTime now = LocalDateTime.now();
        if (existingId != null) {
            Database.update("UPDATE marks SET total_marks = ?, est_marks = ?, mst_marks = ?, quiz_marks = ?, "
                            + "assignment_marks = ?, lab_marks = ?, practical_marks = ?, locked = TRUE, submitted_at = ? "
                            + "WHERE id = ? AND user_id = ?",
                    total, est, mst, quiz, assignment, lab, practical, now, existingId, studentId);
            return existingId;
        }
        return Database.insert("INSERT INTO marks (user_id, subject_id, total_marks, est_marks, mst_marks, quiz_marks, "
                        + "assignment_marks, lab_marks, practical_marks, locked, submitted_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?)",
                studentId, subjectId, total, est, mst, quiz, assignment, lab, practical, now);
    }

    public static void setLocked(long markId, boolean locked) {
        Database.update("UPDATE marks SET locked = ? WHERE id = ?", locked, markId);
    }

    /** Each row is {grade, percentile, markId}. */
    public static void savePredictions(List<Object[]> rows) {
        Database.batch("UPDATE marks SET predicted_grade = ?, percentile = ? WHERE id = ?", rows);
    }
}
