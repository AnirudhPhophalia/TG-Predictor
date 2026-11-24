package com.thapar.gpf.login;

import com.thapar.gpf.AppException;
import com.thapar.gpf.Config;
import com.thapar.gpf.database.Database;
import com.thapar.gpf.database.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Everything about who is signed in: institute email rules, admission-year detection,
 * the single configured admin, password hashing, registration and the current session.
 */
public final class Accounts {

    private static final Pattern THAPAR_EMAIL = Pattern.compile("[a-z0-9][a-z0-9._+\\-]*@thapar\\.edu");
    private static final Pattern ADMISSION_YEAR = Pattern.compile("(?:^|[._-])be[_-]?(\\d{2})(?!\\d)");
    private static final int HASH_ITERATIONS = 310_000;

    /** The signed-in user of this window (a desktop app has exactly one session). */
    private static User current;

    private Accounts() {
    }

    // ---------- rules ----------

    public static boolean isThaparEmail(String email) {
        return email != null && THAPAR_EMAIL.matcher(email.trim().toLowerCase(Locale.ROOT)).matches();
    }

    /** be_24, be.25, be24 ... in the part before @ give 2024, 2025, 2024. Unknown formats give null, never a guess. */
    public static Integer admissionYear(String email) {
        if (!isThaparEmail(email)) return null;
        String local = email.trim().toLowerCase(Locale.ROOT).split("@")[0];
        Matcher match = ADMISSION_YEAR.matcher(local);
        return match.find() ? 2000 + Integer.parseInt(match.group(1)) : null;
    }

    public static boolean isConfiguredAdmin(String email) {
        return Config.adminEmail.equalsIgnoreCase(email == null ? "" : email.trim());
    }

    private static User.Role roleFor(String email) {
        return isConfiguredAdmin(email) ? User.Role.ADMIN : User.Role.STUDENT;
    }

    // ---------- session ----------

    public static User current() {
        if (current == null) throw new IllegalStateException("Nobody is signed in");
        return current;
    }

    public static void signOut() {
        current = null;
    }

    private static User signIn(long userId) {
        current = User.byId(userId).orElseThrow();
        return current;
    }

    // ---------- signing in ----------

    /** Email and password sign-in, available in demo mode only (real accounts use Google). */
    public static User login(String email, String password) {
        if (!Config.demo) throw new AppException("Password sign-in is only available in demo mode.");
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!isThaparEmail(normalized)) throw new AppException("Only @thapar.edu accounts are allowed.");
        User user = User.byEmail(normalized)
                .filter(u -> password != null && verify(password, u.passwordHash()))
                .orElseThrow(() -> new AppException("The email or password is incorrect."));
        // the role always follows ADMIN_EMAIL, whatever the stored row says
        Database.update("UPDATE users SET role = ? WHERE id = ?", roleFor(user.email()), user.id());
        return signIn(user.id());
    }

    /** Called after GoogleLogin has verified the identity token. Creates the account on first sign-in. */
    public static User googleLogin(String email, String name, String googleSubject) {
        if (!isThaparEmail(email)) throw new AppException("An institute email is required.");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        var existing = User.byEmail(normalized);
        if (existing.isPresent() && existing.get().googleSubject() != null
                && !existing.get().googleSubject().equals(googleSubject)) {
            throw new AppException("This Google account does not match the one linked to " + normalized + ".");
        }
        String displayName = name == null || name.isBlank() ? normalized.split("@")[0] : name;
        long id = existing.map(User::id).orElseGet(() -> User.insert(normalized, displayName,
                hash(UUID.randomUUID().toString()), null, null, null, null, User.Role.STUDENT));
        User.updateGoogleLogin(id, displayName, googleSubject, admissionYear(normalized), roleFor(normalized));
        return signIn(id);
    }

    /** Demo-mode self-registration. Always creates a student. */
    public static void register(String name, String email, String password, String rollNumber, String branch,
                                Integer semester) {
        if (!Config.demo) throw new AppException("Registration is only available in demo mode.");
        if (name == null || name.isBlank()) throw new AppException("Name is required");
        if (email == null || email.isBlank()) throw new AppException("Email is required");
        if (password == null || password.length() < 6) throw new AppException("Password must be at least 6 characters");
        if (semester != null && (semester < 1 || semester > 8)) throw new AppException("Semester must be 1-8");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!isThaparEmail(normalized)) throw new AppException("Only official @thapar.edu email addresses can register.");
        if (User.byEmail(normalized).isPresent()) throw new AppException("An account with this email already exists.");
        if (isConfiguredAdmin(normalized)) throw new AppException("Use Google login for the administrator account.");
        User.insert(normalized, name.trim(), hash(password), blankToNull(rollNumber), blankToNull(branch), semester,
                admissionYear(normalized), User.Role.STUDENT);
    }

    public static void updateProfile(String rollNumber, String branch, Integer semester) {
        if (semester == null || semester < 1 || semester > 8 || branch == null || branch.isBlank()
                || branch.trim().length() > 50 || rollNumber == null || !rollNumber.trim().matches("[A-Za-z0-9-]{1,30}")) {
            throw new AppException("Enter a roll number, branch and semester between 1 and 8.");
        }
        User.updateProfile(current().id(), rollNumber.trim(), branch.trim(), semester);
        signIn(current().id());
    }

    // ---------- password hashing (PBKDF2 from the JDK) ----------

    /** Returns "pbkdf2$iterations$salt$hash"; the plain password is never stored. */
    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "pbkdf2$" + HASH_ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(pbkdf2(password, salt, HASH_ITERATIONS));
    }

    static boolean verify(String password, String stored) {
        String[] parts = stored == null ? new String[0] : stored.split("\\$");
        if (parts.length != 4 || !parts[0].equals("pbkdf2")) return false;
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        byte[] actual = pbkdf2(password, Base64.getDecoder().decode(parts[2]), Integer.parseInt(parts[1]));
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
