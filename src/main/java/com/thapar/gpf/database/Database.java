package com.thapar.gpf.database;

import com.thapar.gpf.AppException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * How the data is stored: one JDBC connection to an H2 file (data/*.mv.db) by default,
 * or PostgreSQL when DB_URL points to one. Tables are created on first start.
 *
 * Every other class in this package is one table: it holds the row as a Java record
 * plus the SQL that reads and writes it.
 */
public final class Database {

    private static Connection connection;

    private Database() {
    }

    /** Turns the current row of a ResultSet into a Java object. */
    @FunctionalInterface
    public interface Row<T> {
        T map(ResultSet rs) throws SQLException;
    }

    // ponytail: one shared connection used from the JavaFX thread; use a connection pool if this ever becomes a server
    public static void open(String url, String user, String password) {
        close();
        try {
            connection = DriverManager.getConnection(url, user, password);
            try (Statement statement = connection.createStatement()) {
                for (String table : SCHEMA) {
                    statement.execute(table);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not open the database " + url + ": " + e.getMessage(), e);
        }
    }

    public static void close() {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException ignored) {
            // closing on exit; nothing useful to do
        }
        connection = null;
    }

    /** Runs the work as one all-or-nothing transaction and returns its result. */
    public static <T> T transaction(Supplier<T> work) {
        try {
            if (!connection.getAutoCommit()) {
                return work.get();          // already inside a transaction
            }
            connection.setAutoCommit(false);
            try {
                T result = work.get();
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    public static void inTransaction(Runnable work) {
        transaction(() -> {
            work.run();
            return null;
        });
    }

    // ---------- small helpers around PreparedStatement ----------

    public static <T> List<T> list(String sql, Row<T> row, Object... params) {
        try (PreparedStatement ps = prepare(sql, false, params); ResultSet rs = ps.executeQuery()) {
            List<T> result = new ArrayList<>();
            while (rs.next()) {
                result.add(row.map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    public static <T> Optional<T> one(String sql, Row<T> row, Object... params) {
        return list(sql, row, params).stream().findFirst();
    }

    public static long count(String sql, Object... params) {
        return one(sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    public static int update(String sql, Object... params) {
        try (PreparedStatement ps = prepare(sql, false, params)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    /** Inserts a row and returns its generated id. */
    public static long insert(String sql, Object... params) {
        try (PreparedStatement ps = prepare(sql, true, params)) {
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    /** Runs the same statement once per parameter row, in one round trip. */
    public static void batch(String sql, List<Object[]> rows) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Object[] params : rows) {
                bind(ps, params);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    // ---------- reading nullable columns ----------

    static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, Double.class);
    }

    static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, Integer.class);
    }

    static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class);
    }

    private static PreparedStatement prepare(String sql, boolean returnKeys, Object... params) throws SQLException {
        if (connection == null) throw new IllegalStateException("Database is not open");
        PreparedStatement ps = returnKeys
                ? connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                : connection.prepareStatement(sql);
        bind(ps, params);
        return ps;
    }

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i] instanceof Enum<?> e ? e.name() : params[i];
            ps.setObject(i + 1, value);
        }
    }

    private static RuntimeException failure(SQLException e) {
        // 23505 = unique constraint, e.g. a double click submitting the same thing twice
        if ("23505".equals(e.getSQLState())) return new AppException("That record already exists.");
        return new IllegalStateException("Database error: " + e.getMessage(), e);
    }

    // ---------- the tables ----------

    private static final String ID = "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, ";
    private static final String SIX_CUTOFFS = "cutoff_a DOUBLE PRECISION, cutoff_a_minus DOUBLE PRECISION, "
            + "cutoff_b DOUBLE PRECISION, cutoff_b_minus DOUBLE PRECISION, cutoff_c DOUBLE PRECISION, cutoff_d DOUBLE PRECISION, ";

    private static final String[] SCHEMA = {
            "CREATE TABLE IF NOT EXISTS users (" + ID
                    + "email VARCHAR(255) NOT NULL UNIQUE, name VARCHAR(255) NOT NULL, password VARCHAR(255) NOT NULL, "
                    + "roll_number VARCHAR(30), branch VARCHAR(50), semester INT, admission_year INT, "
                    + "google_subject VARCHAR(255), last_login TIMESTAMP, role VARCHAR(10) NOT NULL, created_at TIMESTAMP NOT NULL)",
            "CREATE TABLE IF NOT EXISTS subjects (" + ID
                    + "code VARCHAR(30) NOT NULL UNIQUE, name VARCHAR(255) NOT NULL, semester INT, branch VARCHAR(50), "
                    + "credits DOUBLE PRECISION, active BOOLEAN NOT NULL, created_at TIMESTAMP NOT NULL)",
            // the boundaries currently used for predictions while a subject has fewer than 100 submissions
            "CREATE TABLE IF NOT EXISTS historical_cutoffs (subject_id BIGINT PRIMARY KEY REFERENCES subjects(id), "
                    + SIX_CUTOFFS + "created_at TIMESTAMP NOT NULL)",
            // previous-year records, one per subject and academic year
            "CREATE TABLE IF NOT EXISTS past_year_cutoffs (" + ID + "subject_id BIGINT NOT NULL REFERENCES subjects(id), "
                    + "academic_year INT NOT NULL, " + SIX_CUTOFFS + "updated_at TIMESTAMP NOT NULL, "
                    + "UNIQUE (subject_id, academic_year))",
            "CREATE TABLE IF NOT EXISTS marks (" + ID
                    + "user_id BIGINT NOT NULL REFERENCES users(id), subject_id BIGINT NOT NULL REFERENCES subjects(id), "
                    + "total_marks DOUBLE PRECISION NOT NULL, est_marks DOUBLE PRECISION, mst_marks DOUBLE PRECISION, "
                    + "quiz_marks DOUBLE PRECISION, assignment_marks DOUBLE PRECISION, lab_marks DOUBLE PRECISION, "
                    + "practical_marks DOUBLE PRECISION, predicted_grade VARCHAR(10), percentile DOUBLE PRECISION, "
                    + "locked BOOLEAN NOT NULL, submitted_at TIMESTAMP, UNIQUE (user_id, subject_id))",
            // cached aggregates per subject: no student identity in here
            "CREATE TABLE IF NOT EXISTS subject_statistics (subject_id BIGINT PRIMARY KEY REFERENCES subjects(id), "
                    + "total_responses INT NOT NULL, average_marks DOUBLE PRECISION, median_marks DOUBLE PRECISION, "
                    + "mode_marks DOUBLE PRECISION, highest_marks DOUBLE PRECISION, lowest_marks DOUBLE PRECISION, "
                    + "standard_deviation DOUBLE PRECISION, pass_percentage DOUBLE PRECISION, failure_percentage DOUBLE PRECISION, "
                    + "grade_a INT, grade_a_minus INT, grade_b INT, grade_b_minus INT, grade_c INT, grade_d INT, grade_e INT, "
                    + "last_updated TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS polls (" + ID + "subject_id BIGINT NOT NULL REFERENCES subjects(id), "
                    + "question VARCHAR(300) NOT NULL, end_time TIMESTAMP, active BOOLEAN NOT NULL, created_at TIMESTAMP NOT NULL, "
                    + SIX_CUTOFFS.substring(0, SIX_CUTOFFS.length() - 2) + ")",
            // the user link only enforces one answer per student; admins only ever read aggregates
            "CREATE TABLE IF NOT EXISTS poll_responses (" + ID + "poll_id BIGINT NOT NULL REFERENCES polls(id), "
                    + "user_id BIGINT NOT NULL REFERENCES users(id), marks DOUBLE PRECISION NOT NULL, "
                    + "submitted_at TIMESTAMP NOT NULL, UNIQUE (poll_id, user_id))",
            "CREATE TABLE IF NOT EXISTS edit_requests (" + ID + "user_id BIGINT NOT NULL REFERENCES users(id), "
                    + "mark_id BIGINT NOT NULL REFERENCES marks(id), reason VARCHAR(500) NOT NULL, status VARCHAR(10) NOT NULL, "
                    + "rejection_reason VARCHAR(500), created_at TIMESTAMP NOT NULL, reviewed_at TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS platform_settings (id BIGINT PRIMARY KEY, maintenance_mode BOOLEAN NOT NULL, "
                    + "allow_student_edits BOOLEAN NOT NULL, system_email VARCHAR(255) NOT NULL)",
    };
}
