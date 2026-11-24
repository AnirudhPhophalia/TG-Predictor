package com.thapar.gpf.database;

import com.thapar.gpf.calculation.GradeCalculator;

import java.time.LocalDateTime;

/**
 * Table "subject_statistics": the cached aggregate numbers of one subject, refreshed after every
 * submission. It holds no student identity. gradeCounts is A, A-, B, B-, C, D, E.
 */
public record SubjectStats(long subjectId, String code, String name, int totalResponses, double average,
                           double median, double mode, double highest, double lowest, double standardDeviation,
                           double passPercentage, double failurePercentage, int[] gradeCounts, boolean hidden) {

    public static SubjectStats of(Subject subject) {
        return Database.one("SELECT * FROM subject_statistics WHERE subject_id = ?",
                rs -> new SubjectStats(subject.id(), subject.code(), subject.name(), rs.getInt("total_responses"),
                        rs.getDouble("average_marks"), rs.getDouble("median_marks"), rs.getDouble("mode_marks"),
                        rs.getDouble("highest_marks"), rs.getDouble("lowest_marks"), rs.getDouble("standard_deviation"),
                        rs.getDouble("pass_percentage"), rs.getDouble("failure_percentage"),
                        new int[]{rs.getInt("grade_a"), rs.getInt("grade_a_minus"), rs.getInt("grade_b"),
                                rs.getInt("grade_b_minus"), rs.getInt("grade_c"), rs.getInt("grade_d"), rs.getInt("grade_e")},
                        false), subject.id())
                .orElse(new SubjectStats(subject.id(), subject.code(), subject.name(), 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        new int[7], false));
    }

    public static void save(long subjectId, GradeCalculator.Stats s, int[] g) {
        Object[] values = {s.count(), s.mean(), s.median(), s.mode(), s.highest(), s.lowest(), s.standardDeviation(),
                s.passPercentage(), s.failurePercentage(), g[0], g[1], g[2], g[3], g[4], g[5], g[6], LocalDateTime.now(), subjectId};
        int updated = Database.update("UPDATE subject_statistics SET total_responses = ?, average_marks = ?, median_marks = ?, "
                + "mode_marks = ?, highest_marks = ?, lowest_marks = ?, standard_deviation = ?, pass_percentage = ?, "
                + "failure_percentage = ?, grade_a = ?, grade_a_minus = ?, grade_b = ?, grade_b_minus = ?, grade_c = ?, "
                + "grade_d = ?, grade_e = ?, last_updated = ? WHERE subject_id = ?", values);
        if (updated == 0) {
            Database.update("INSERT INTO subject_statistics (total_responses, average_marks, median_marks, mode_marks, "
                    + "highest_marks, lowest_marks, standard_deviation, pass_percentage, failure_percentage, grade_a, "
                    + "grade_a_minus, grade_b, grade_b_minus, grade_c, grade_d, grade_e, last_updated, subject_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", values);
        }
    }

    /**
     * The version admins may see: with too few responses the numbers would reveal
     * individual marks, so only the response count is kept.
     */
    public SubjectStats forAdmin() {
        if (totalResponses >= GradeCalculator.MIN_RESPONSES_FOR_ADMIN) return this;
        return new SubjectStats(subjectId, code, name, totalResponses, 0, 0, 0, 0, 0, 0, 0, 0, new int[7], true);
    }

    /** Participation indicator: grows with submissions and is 100% at 100 submissions. */
    public int confidencePercent() {
        return Math.min(100, totalResponses);
    }

    public String confidenceLabel() {
        if (totalResponses >= 100) return "High";
        if (totalResponses >= 30) return "Medium";
        return "Low";
    }

    public boolean relativeGradingActive() {
        return totalResponses >= GradeCalculator.MIN_SUBMISSIONS_FOR_RELATIVE;
    }
}
