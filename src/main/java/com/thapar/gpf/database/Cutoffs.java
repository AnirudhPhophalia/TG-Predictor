package com.thapar.gpf.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Six grade boundaries (lowest marks for A, A-, B, B-, C, D). Stored in three places:
 * - historical_cutoffs: the boundaries a subject uses for predictions before it has 100 submissions
 * - past_year_cutoffs: previous-year records, one per subject and academic year (never overwritten by other years)
 * - polls: optional manual boundaries for a single poll
 */
public record Cutoffs(double a, double aMinus, double b, double bMinus, double c, double d) {

    /** Estimates, not verified university records. */
    public static final Cutoffs DEFAULTS = new Cutoffs(85, 75, 65, 55, 45, 35);

    private static final String COLUMNS = "cutoff_a = ?, cutoff_a_minus = ?, cutoff_b = ?, cutoff_b_minus = ?, cutoff_c = ?, cutoff_d = ?";

    public double[] values() {
        return new double[]{a, aMinus, b, bMinus, c, d};
    }

    /** Reads the six cutoff columns of the current row; null when they are empty (a poll without manual cutoffs). */
    static Cutoffs map(ResultSet rs) throws SQLException {
        Double first = Database.nullableDouble(rs, "cutoff_a");
        if (first == null) return null;
        return new Cutoffs(first, rs.getDouble("cutoff_a_minus"), rs.getDouble("cutoff_b"),
                rs.getDouble("cutoff_b_minus"), rs.getDouble("cutoff_c"), rs.getDouble("cutoff_d"));
    }

    // ---------- active fallback cutoffs ----------

    public static Cutoffs active(long subjectId) {
        return Database.one("SELECT * FROM historical_cutoffs WHERE subject_id = ?", Cutoffs::map, subjectId)
                .orElseGet(() -> {
                    setActive(subjectId, DEFAULTS);
                    return DEFAULTS;
                });
    }

    public static void setActive(long subjectId, Cutoffs c) {
        int updated = Database.update("UPDATE historical_cutoffs SET " + COLUMNS + " WHERE subject_id = ?",
                c.a, c.aMinus, c.b, c.bMinus, c.c, c.d, subjectId);
        if (updated == 0) {
            Database.update("INSERT INTO historical_cutoffs (subject_id, cutoff_a, cutoff_a_minus, cutoff_b, cutoff_b_minus, "
                            + "cutoff_c, cutoff_d, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    subjectId, c.a, c.aMinus, c.b, c.bMinus, c.c, c.d, LocalDateTime.now());
        }
    }

    // ---------- previous-year records ----------

    public record YearRecord(long id, long subjectId, String subjectCode, int academicYear, Cutoffs cutoffs,
                             LocalDateTime updatedAt) {
    }

    public static List<YearRecord> history() {
        return Database.list("SELECT p.*, s.code AS subject_code FROM past_year_cutoffs p "
                        + "JOIN subjects s ON s.id = p.subject_id ORDER BY p.academic_year DESC, s.code",
                rs -> new YearRecord(rs.getLong("id"), rs.getLong("subject_id"), rs.getString("subject_code"),
                        rs.getInt("academic_year"), map(rs), Database.time(rs, "updated_at")));
    }

    /** Saving the same subject and year again updates that record; other years are untouched. */
    public static void saveYear(long subjectId, int academicYear, Cutoffs c) {
        int updated = Database.update("UPDATE past_year_cutoffs SET " + COLUMNS + ", updated_at = ? "
                        + "WHERE subject_id = ? AND academic_year = ?",
                c.a, c.aMinus, c.b, c.bMinus, c.c, c.d, LocalDateTime.now(), subjectId, academicYear);
        if (updated == 0) {
            Database.insert("INSERT INTO past_year_cutoffs (subject_id, academic_year, cutoff_a, cutoff_a_minus, cutoff_b, "
                            + "cutoff_b_minus, cutoff_c, cutoff_d, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    subjectId, academicYear, c.a, c.aMinus, c.b, c.bMinus, c.c, c.d, LocalDateTime.now());
        }
    }
}
