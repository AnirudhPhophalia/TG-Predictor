package com.thapar.gpf.database;

import com.thapar.gpf.AppException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** Table "subjects": the curriculum. Every new subject starts with the default fallback cutoffs. */
public record Subject(long id, String code, String name, Integer semester, String branch, Double credits,
                      boolean active, LocalDateTime createdAt) {

    private static Subject map(ResultSet rs) throws SQLException {
        return new Subject(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                Database.nullableInt(rs, "semester"), rs.getString("branch"), Database.nullableDouble(rs, "credits"),
                rs.getBoolean("active"), Database.time(rs, "created_at"));
    }

    public static List<Subject> all() {
        return Database.list("SELECT * FROM subjects ORDER BY code", Subject::map);
    }

    public static List<Subject> allActive() {
        return Database.list("SELECT * FROM subjects WHERE active ORDER BY code", Subject::map);
    }

    public static Subject get(long id) {
        return Database.one("SELECT * FROM subjects WHERE id = ?", Subject::map, id)
                .orElseThrow(() -> new AppException("Subject not found"));
    }

    public static Subject byCode(String code) {
        return Database.one("SELECT * FROM subjects WHERE code = ?", Subject::map, code)
                .orElseThrow(() -> new AppException("Subject " + code + " not found"));
    }

    public static long count() {
        return Database.count("SELECT COUNT(*) FROM subjects");
    }

    public static long countActive() {
        return Database.count("SELECT COUNT(*) FROM subjects WHERE active");
    }

    public static Subject create(String code, String name, Integer semester, String branch, Double credits) {
        String cleanCode = validate(code, name, semester, credits);
        if (Database.count("SELECT COUNT(*) FROM subjects WHERE code = ?", cleanCode) > 0) {
            throw new AppException("A subject with code " + cleanCode + " already exists.");
        }
        return Database.transaction(() -> {
            long id = Database.insert("INSERT INTO subjects (code, name, semester, branch, credits, active, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, TRUE, ?)", cleanCode, name.trim(), semester, trim(branch), credits, LocalDateTime.now());
            Cutoffs.setActive(id, Cutoffs.DEFAULTS);
            return get(id);
        });
    }

    public static void update(long id, String code, String name, Integer semester, String branch, Double credits) {
        String cleanCode = validate(code, name, semester, credits);
        if (Database.count("SELECT COUNT(*) FROM subjects WHERE code = ? AND id <> ?", cleanCode, id) > 0) {
            throw new AppException("A subject with code " + cleanCode + " already exists.");
        }
        Database.update("UPDATE subjects SET code = ?, name = ?, semester = ?, branch = ?, credits = ? WHERE id = ?",
                cleanCode, name.trim(), semester, trim(branch), credits, id);
    }

    public static void toggleActive(long id) {
        Database.update("UPDATE subjects SET active = NOT active WHERE id = ?", id);
    }

    private static String validate(String code, String name, Integer semester, Double credits) {
        if (code == null || code.isBlank()) throw new AppException("Subject code is required");
        if (name == null || name.isBlank()) throw new AppException("Subject name is required");
        if (semester == null || semester < 1 || semester > 8) throw new AppException("Semester must be 1-8");
        if (credits == null || credits < 1 || credits > 30) throw new AppException("Credits must be 1-30");
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
