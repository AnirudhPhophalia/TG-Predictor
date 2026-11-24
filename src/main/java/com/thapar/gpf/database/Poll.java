package com.thapar.gpf.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Tables "polls" and "poll_responses". A poll asks a subject's class for their marks.
 * Answers are stored with the student's id only to allow one answer per student;
 * nothing outside the student's own prediction ever reads an answer together with its owner.
 */
public record Poll(long id, long subjectId, String question, LocalDateTime endTime, boolean active,
                   LocalDateTime createdAt, Cutoffs manualCutoffs) {

    public boolean isOpen() {
        return active && endTime != null && endTime.isAfter(LocalDateTime.now());
    }

    public Subject subject() {
        return Subject.get(subjectId);
    }

    private static Poll map(ResultSet rs) throws SQLException {
        return new Poll(rs.getLong("id"), rs.getLong("subject_id"), rs.getString("question"),
                Database.time(rs, "end_time"), rs.getBoolean("active"), Database.time(rs, "created_at"), Cutoffs.map(rs));
    }

    public static List<Poll> all() {
        return Database.list("SELECT * FROM polls ORDER BY created_at DESC, id DESC", Poll::map);
    }

    public static Optional<Poll> byId(long id) {
        return Database.one("SELECT * FROM polls WHERE id = ?", Poll::map, id);
    }

    public static long countOpen() {
        return Database.count("SELECT COUNT(*) FROM polls WHERE active AND end_time > ?", LocalDateTime.now());
    }

    public static long insert(long subjectId, String question, LocalDateTime endTime) {
        return Database.insert("INSERT INTO polls (subject_id, question, end_time, active, created_at) VALUES (?, ?, ?, TRUE, ?)",
                subjectId, question, endTime, LocalDateTime.now());
    }

    public static void close(long id) {
        Database.update("UPDATE polls SET active = FALSE WHERE id = ?", id);
    }

    public static void setManualCutoffs(long id, Cutoffs c) {
        Database.update("UPDATE polls SET cutoff_a = ?, cutoff_a_minus = ?, cutoff_b = ?, cutoff_b_minus = ?, "
                + "cutoff_c = ?, cutoff_d = ? WHERE id = ?", c.a(), c.aMinus(), c.b(), c.bMinus(), c.c(), c.d(), id);
    }

    // ---------- answers ----------

    public static boolean hasResponded(long pollId, long studentId) {
        return Database.count("SELECT COUNT(*) FROM poll_responses WHERE poll_id = ? AND user_id = ?", pollId, studentId) > 0;
    }

    /** The logged-in student's own answer. */
    public static Optional<Double> myAnswer(long pollId, long studentId) {
        return Database.one("SELECT marks FROM poll_responses WHERE poll_id = ? AND user_id = ?",
                rs -> rs.getDouble(1), pollId, studentId);
    }

    /** All answers of a poll with no student information attached. */
    public static List<Double> answers(long pollId) {
        return Database.list("SELECT marks FROM poll_responses WHERE poll_id = ?", rs -> rs.getDouble(1), pollId);
    }

    public static void addAnswer(long pollId, long studentId, double marks) {
        Database.insert("INSERT INTO poll_responses (poll_id, user_id, marks, submitted_at) VALUES (?, ?, ?, ?)",
                pollId, studentId, marks, LocalDateTime.now());
    }
}
