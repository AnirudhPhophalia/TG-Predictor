package com.thapar.gpf.database;

import com.thapar.gpf.AppException;
import com.thapar.gpf.login.Accounts;

import java.util.Locale;

/** Table "platform_settings": a single row (id 1) with the admin's platform switches. */
public record Settings(boolean maintenanceMode, boolean allowStudentEdits, String systemEmail) {

    private static final Settings DEFAULTS = new Settings(false, true, "admin@thapar.edu");

    public static Settings get() {
        return Database.one("SELECT * FROM platform_settings WHERE id = 1",
                rs -> new Settings(rs.getBoolean("maintenance_mode"), rs.getBoolean("allow_student_edits"),
                        rs.getString("system_email"))).orElse(DEFAULTS);
    }

    public static void save(boolean maintenanceMode, boolean allowStudentEdits, String systemEmail) {
        if (!Accounts.isThaparEmail(systemEmail)) throw new AppException("Use an @thapar.edu contact email.");
        String email = systemEmail.trim().toLowerCase(Locale.ROOT);
        int updated = Database.update("UPDATE platform_settings SET maintenance_mode = ?, allow_student_edits = ?, "
                + "system_email = ? WHERE id = 1", maintenanceMode, allowStudentEdits, email);
        if (updated == 0) {
            Database.update("INSERT INTO platform_settings (id, maintenance_mode, allow_student_edits, system_email) "
                    + "VALUES (1, ?, ?, ?)", maintenanceMode, allowStudentEdits, email);
        }
    }

    public static void requireSubmissionsOpen() {
        if (get().maintenanceMode()) {
            throw new AppException("Submissions are paused for maintenance. Please try again later.");
        }
    }

    public static void requireUnlockRequestsOpen() {
        requireSubmissionsOpen();
        if (!get().allowStudentEdits()) throw new AppException("Unlock requests are temporarily paused.");
    }
}
