package com.thapar.gpf.login;

import com.sun.net.httpserver.HttpServer;
import com.thapar.gpf.AppException;
import com.thapar.gpf.Config;
import javafx.application.Platform;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Google sign-in for a desktop app (OAuth 2.0 loopback flow with PKCE):
 * open the browser at Google, receive the code on http://127.0.0.1:{random port},
 * exchange it for an ID token over TLS, then check the token's claims.
 *
 * Needs a Google Cloud OAuth client of type "Desktop app" (see README).
 */
public final class GoogleLogin {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private GoogleLogin() {
    }

    /** A verified institute identity. */
    public record Identity(String email, String name, String subject) {
    }

    /**
     * Runs the browser flow on a background thread. The callbacks run on the JavaFX thread,
     * so they may touch the screen and the database.
     */
    public static void start(Consumer<String> openBrowser, Consumer<Identity> onSuccess, Consumer<String> onError) {
        Thread thread = new Thread(() -> {
            try {
                Identity identity = authorize(openBrowser);
                Platform.runLater(() -> onSuccess.accept(identity));
            } catch (AppException e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept("Google sign-in could not be completed: " + e.getMessage()));
            }
        }, "google-login");
        thread.setDaemon(true);
        thread.start();
    }

    private static Identity authorize(Consumer<String> openBrowser) throws Exception {
        String state = random(), nonce = random(), verifier = random();
        String challenge = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));

        CompletableFuture<Map<String, String>> callback = new CompletableFuture<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            byte[] page = ("<html><body style='font-family:sans-serif;padding:40px'><h2>You can close this tab and "
                    + "return to Thapar Grade Predictor.</h2></body></html>").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, page.length);
            exchange.getResponseBody().write(page);
            exchange.close();
            callback.complete(query(exchange.getRequestURI().getRawQuery()));
        });
        server.start();
        try {
            String redirectUri = "http://127.0.0.1:" + server.getAddress().getPort();
            Map<String, String> params = new LinkedHashMap<>();
            params.put("client_id", Config.googleClientId);
            params.put("redirect_uri", redirectUri);
            params.put("response_type", "code");
            params.put("scope", "openid email profile");
            params.put("state", state);
            params.put("nonce", nonce);
            params.put("code_challenge", challenge);
            params.put("code_challenge_method", "S256");
            params.put("hd", "thapar.edu");
            params.put("prompt", "select_account");
            openBrowser.accept(AUTH_URL + "?" + form(params));

            Map<String, String> result = callback.get(3, TimeUnit.MINUTES);
            if (!state.equals(result.get("state"))) throw new AppException("Google sign-in failed: unexpected response.");
            if (result.get("code") == null) throw new AppException("Google sign-in was cancelled.");

            Map<String, String> exchange = new LinkedHashMap<>();
            exchange.put("code", result.get("code"));
            exchange.put("client_id", Config.googleClientId);
            exchange.put("client_secret", Config.googleClientSecret);
            exchange.put("redirect_uri", redirectUri);
            exchange.put("grant_type", "authorization_code");
            exchange.put("code_verifier", verifier);
            HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(TOKEN_URL))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(form(exchange))).build(),
                    HttpResponse.BodyHandlers.ofString());
            String idToken = json(response.body(), "id_token");
            if (response.statusCode() != 200 || idToken == null) {
                throw new AppException("Google did not return an identity. Check GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.");
            }
            // The token came straight from Google's token endpoint over TLS, so (per OpenID Connect Core 3.1.3.7)
            // the TLS connection vouches for the issuer; the claims below are still checked one by one.
            String claims = new String(Base64.getUrlDecoder().decode(idToken.split("\\.")[1]), StandardCharsets.UTF_8);
            return verify(claims, Config.googleClientId, nonce, Instant.now());
        } finally {
            server.stop(0);
        }
    }

    /**
     * Accepts only a fresh token issued by Google for this app, for a verified @thapar.edu
     * address with the hosted-domain claim hd=thapar.edu. A lookalike domain is rejected.
     */
    public static Identity verify(String claims, String clientId, String nonce, Instant now) {
        String issuer = json(claims, "iss");
        String expires = json(claims, "exp");
        if (!("https://accounts.google.com".equals(issuer) || "accounts.google.com".equals(issuer))
                || !clientId.equals(json(claims, "aud")) || !nonce.equals(json(claims, "nonce"))
                || expires == null || now.getEpochSecond() >= Long.parseLong(expires)) {
            throw new AppException("Google sign-in failed: the identity token is not valid for this app.");
        }
        String email = json(claims, "email");
        if (!"true".equals(json(claims, "email_verified")) || !Accounts.isThaparEmail(email)
                || !"thapar.edu".equals(json(claims, "hd"))) {
            throw new AppException("Use a verified institute @thapar.edu Google account and try again.");
        }
        return new Identity(email, json(claims, "name"), json(claims, "sub"));
    }

    // ponytail: reads one top-level field of flat JSON (all Google returns here); use a JSON library if nested values are needed
    static String json(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(?:\"((?:\\\\.|[^\"\\\\])*)\"|([^,}\\s]+))")
                .matcher(json == null ? "" : json);
        if (!m.find()) return null;
        if (m.group(1) == null) return m.group(2);         // true, false or a number
        StringBuilder out = new StringBuilder();
        String raw = m.group(1);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char next = raw.charAt(++i);
            switch (next) {
                case 'u' -> {
                    out.append((char) Integer.parseInt(raw.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                case 'n' -> out.append('\n');
                case 't' -> out.append('\t');
                default -> out.append(next);         // \" \\ \/
            }
        }
        return out.toString();
    }

    private static String random() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return base64Url(bytes);
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String form(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static Map<String, String> query(String rawQuery) {
        Map<String, String> result = new HashMap<>();
        if (rawQuery == null) return result;
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                result.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return result;
    }
}
