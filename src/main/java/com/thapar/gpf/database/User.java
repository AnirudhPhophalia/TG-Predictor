package com.thapar.gpf.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Table "users". The password column holds a salted PBKDF2 hash, never the password itself. */
public record User(long id, String email, String name, String passwordHash, String rollNumber, String branch,
                   Integer semester, Integer admissionYear, String googleSubject, LocalDateTime lastLogin,
                   Role role, LocalDateTime createdAt) {

    public enum Role { STUDENT, ADMIN }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getLong("id"), rs.getString("email"), rs.getString("name"), rs.getString("password"),
                rs.getString("roll_number"), rs.getString("branch"), Database.nullableInt(rs, "semester"),
                Database.nullableInt(rs, "admission_year"), rs.getString("google_subject"),
                Database.time(rs, "last_login"), Role.valueOf(rs.getString("role")), Database.time(rs, "created_at"));
    }

    public static Optional<User> byEmail(String email) {
        return Database.one("SELECT * FROM users WHERE email = ?", User::map, email);
    }

    public static Optional<User> byId(long id) {
        return Database.one("SELECT * FROM users WHERE id = ?", User::map, id);
    }

    /** Newest first. Metadata only: marks live in their own table. */
    public static List<User> all() {
        return Database.list("SELECT * FROM users ORDER BY created_at DESC, id DESC", User::map);
    }

    public static long count() {
        return Database.count("SELECT COUNT(*) FROM users");
    }

    public static long countByRole(Role role) {
        return Database.count("SELECT COUNT(*) FROM users WHERE role = ?", role);
    }

    public static long insert(String email, String name, String passwordHash, String rollNumber, String branch,
                              Integer semester, Integer admissionYear, Role role) {
        return Database.insert("INSERT INTO users (email, name, password, roll_number, branch, semester, admission_year, "
                        + "role, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                email, name, passwordHash, rollNumber, branch, semester, admissionYear, role, LocalDateTime.now());
    }

    public static void updateProfile(long id, String rollNumber, String branch, int semester) {
        Database.update("UPDATE users SET roll_number = ?, branch = ?, semester = ? WHERE id = ?",
                rollNumber, branch, semester, id);
    }

    public static void updateGoogleLogin(long id, String name, String googleSubject, Integer admissionYear, Role role) {
        Database.update("UPDATE users SET name = ?, google_subject = ?, admission_year = ?, role = ?, last_login = ? "
                + "WHERE id = ?", name, googleSubject, admissionYear, role, LocalDateTime.now(), id);
    }
}
