package com.thapar.gpf.database;

import com.thapar.gpf.AppException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Table "edit_requests": a student's request to unlock a submitted mark so it can be corrected. */
public record EditRequest(long id, long userId, long markId, String reason, Status status, String rejectionReason,
                          LocalDateTime createdAt, LocalDateTime reviewedAt) {

    public enum Status { PENDING, APPROVED, REJECTED }

    /** What an admin sees of a request: who, which subject, why. Never the marks. */
    public record AdminView(long id, String studentName, String rollNumber, String subjectCode, String subjectName,
                            String reason, Status status, String rejectionReason, LocalDateTime createdAt) {
    }

    private static EditRequest map(ResultSet rs) throws SQLException {
        return new EditRequest(rs.getLong("id"), rs.getLong("user_id"), rs.getLong("mark_id"), rs.getString("reason"),
                Status.valueOf(rs.getString("status")), rs.getString("rejection_reason"),
                Database.time(rs, "created_at"), Database.time(rs, "reviewed_at"));
    }

    public static List<EditRequest> mine(long studentId) {
        return Database.list("SELECT * FROM edit_requests WHERE user_id = ? ORDER BY created_at DESC, id DESC",
                EditRequest::map, studentId);
    }

    public static Optional<EditRequest> byId(long id) {
        return Database.one("SELECT * FROM edit_requests WHERE id = ?", EditRequest::map, id);
    }

    public static boolean hasPending(long markId) {
        return Database.count("SELECT COUNT(*) FROM edit_requests WHERE mark_id = ? AND status = 'PENDING'", markId) > 0;
    }

    public static long countPending() {
        return Database.count("SELECT COUNT(*) FROM edit_requests WHERE status = 'PENDING'");
    }

    /** The join reads names and subjects only; no column of the marks row except its subject is selected. */
    public static List<AdminView> allForAdmin() {
        return Database.list("SELECT r.id, u.name, u.roll_number, s.code, s.name AS subject_name, r.reason, r.status, "
                        + "r.rejection_reason, r.created_at FROM edit_requests r JOIN users u ON u.id = r.user_id "
                        + "JOIN marks m ON m.id = r.mark_id JOIN subjects s ON s.id = m.subject_id "
                        + "ORDER BY r.created_at DESC, r.id DESC",
                rs -> new AdminView(rs.getLong("id"), rs.getString("name"), rs.getString("roll_number"),
                        rs.getString("code"), rs.getString("subject_name"), rs.getString("reason"),
                        Status.valueOf(rs.getString("status")), rs.getString("rejection_reason"),
                        Database.time(rs, "created_at")));
    }

    // ---------- the unlock flow: student asks, admin approves or rejects, student updates and the mark relocks ----------

    public static long create(long studentId, long markId, String reason) {
        Settings.requireUnlockRequestsOpen();
        Mark mark = Mark.mine(studentId, markId).orElseThrow(() -> new AppException("Marks not found"));
        if (reason == null || reason.isBlank()) {
            throw new AppException("Please tell the admin why you need to edit your marks.");
        }
        if (reason.trim().length() > 500) throw new AppException("Reason must be 500 characters or fewer.");
        if (!mark.locked()) throw new AppException("Your marks are already unlocked. You can update them now.");
        if (hasPending(markId)) throw new AppException("You already have a pending unlock request for this subject.");
        return Database.insert("INSERT INTO edit_requests (user_id, mark_id, reason, status, created_at) VALUES (?, ?, ?, ?, ?)",
                studentId, markId, reason.trim(), Status.PENDING, LocalDateTime.now());
    }

    /** Unlocks the mark. The admin never sees or supplies the marks themselves. */
    public static void approve(long requestId) {
        EditRequest request = pending(requestId);
        Database.inTransaction(() -> {
            review(requestId, Status.APPROVED, null);
            Mark.setLocked(request.markId(), false);
        });
    }

    public static void reject(long requestId, String rejectionReason) {
        pending(requestId);
        review(requestId, Status.REJECTED,
                rejectionReason == null || rejectionReason.isBlank() ? "No reason given" : rejectionReason.trim());
    }

    private static EditRequest pending(long requestId) {
        EditRequest request = byId(requestId).orElseThrow(() -> new AppException("Request not found"));
        if (request.status() != Status.PENDING) throw new AppException("This request has already been reviewed.");
        return request;
    }

    private static void review(long id, Status status, String rejectionReason) {
        Database.update("UPDATE edit_requests SET status = ?, rejection_reason = ?, reviewed_at = ? WHERE id = ?",
                status, rejectionReason, LocalDateTime.now(), id);
    }
}
