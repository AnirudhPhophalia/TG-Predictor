package com.thapar.gpf;

import java.util.Locale;

/** Settings read from environment variables (or a .env file loaded by run.sh). */
public final class Config {

    private Config() {
    }

    /** True when started with --demo: separate database, password login, sample accounts. */
    public static boolean demo = false;

    /** The single account that receives ADMIN access. Must be an @thapar.edu address. */
    public static String adminEmail = env("ADMIN_EMAIL", "admin@thapar.edu").trim().toLowerCase(Locale.ROOT);

    public static boolean googleEnabled = "true".equalsIgnoreCase(env("GOOGLE_LOGIN_ENABLED", "false"));
    public static String googleClientId = env("GOOGLE_CLIENT_ID", "");
    public static String googleClientSecret = env("GOOGLE_CLIENT_SECRET", "");

    /** Number of synthetic students seeded into UCS301 in demo mode. */
    public static int demoCohortSize = 120;

    public static String dbUrl() {
        return env("DB_URL", demo ? "jdbc:h2:file:./data/demo-grade-predictor-fx" : "jdbc:h2:file:./data/grade-predictor-fx");
    }

    public static String dbUser() {
        return env("DB_USER", "sa");
    }

    public static String dbPassword() {
        return env("DB_PASSWORD", "");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
