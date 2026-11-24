package com.thapar.gpf;

import com.thapar.gpf.database.Database;
import com.thapar.gpf.database.DemoData;
import com.thapar.gpf.database.User;
import com.thapar.gpf.login.Accounts;
import com.thapar.gpf.login.LoginScreen;
import com.thapar.gpf.screens.AdminScreens;
import com.thapar.gpf.screens.StudentScreens;
import com.thapar.gpf.screens.Ui;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The window: a header with navigation, a message banner and a scrollable page.
 * Screens are plain methods that return a JavaFX Node; App.show(...) swaps them in.
 */
public class App extends Application {

    private static Stage stage;
    private static HostServices hostServices;
    private static BorderPane root;
    private static VBox banner;
    private static ScrollPane scroll;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        hostServices = getHostServices();
        Config.demo = getParameters().getRaw().contains("--demo");
        try {
            if (!Accounts.isThaparEmail(Config.adminEmail)) {
                throw new IllegalStateException("ADMIN_EMAIL must be an @thapar.edu address.");
            }
            Database.open(Config.dbUrl(), Config.dbUser(), Config.dbPassword());
            DemoData.seedIfEmpty();
        } catch (RuntimeException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "The app could not start.\n\n" + e.getMessage()
                    + "\n\nIs another copy of the app already open?").showAndWait();
            Platform.exit();
            return;
        }

        root = new BorderPane();
        banner = new VBox();
        scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content");
        VBox center = new VBox(banner, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setCenter(center);
        root.setBottom(footer());
        root.getStyleClass().add("root-pane");

        Scene scene = new Scene(root, 1240, 820);
        scene.getStylesheets().add(App.class.getResource("/app.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Thapar Grade Predictor" + (Config.demo ? " (demo)" : ""));
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        showLogin();
        stage.show();
    }

    @Override
    public void stop() {
        Database.close();
    }

    // ---------- navigation ----------

    public static void showLogin() {
        root.setTop(null);
        show(LoginScreen.view());
    }

    public static void signedIn(User user) {
        root.setTop(header(user));
        show(user.isAdmin() ? AdminScreens.overview() : StudentScreens.overview(null));
    }

    /** Replaces the page and clears the old message. */
    public static void show(Node page) {
        banner.getChildren().clear();
        scroll.setContent(page);
        scroll.setVvalue(0);
    }

    public static void ok(String message) {
        flash(message, "success");
    }

    public static void fail(String message) {
        flash(message, "failure");
    }

    private static void flash(String message, String style) {
        Label label = Ui.text(message, style);
        label.setMaxWidth(Double.MAX_VALUE);
        banner.getChildren().setAll(label);
    }

    /** Runs a button action. A broken rule (AppException) becomes a red banner on the current page. */
    public static void act(Runnable action) {
        try {
            action.run();
        } catch (AppException e) {
            fail(e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            fail("Something went wrong: " + e.getMessage());
        }
    }

    public static void openBrowser(String url) {
        hostServices.showDocument(url);
    }

    public static Stage stage() {
        return stage;
    }

    // ---------- header and footer ----------

    private static HBox header(User user) {
        Map<String, Supplier<Node>> pages = new LinkedHashMap<>();
        if (user.isAdmin()) {
            pages.put("Overview", AdminScreens::overview);
            pages.put("Subjects", () -> AdminScreens.subjects(null));
            pages.put("Cutoffs", AdminScreens::cutoffs);
            pages.put("Users", AdminScreens::users);
            pages.put("Requests", AdminScreens::requests);
            pages.put("Polls", AdminScreens::polls);
            pages.put("Analytics", AdminScreens::analytics);
            pages.put("Settings", AdminScreens::settings);
        } else {
            pages.put("Overview", () -> StudentScreens.overview(null));
            pages.put("Subjects", () -> StudentScreens.subjects("", null));
            pages.put("Polls", StudentScreens::polls);
            pages.put("Requests", StudentScreens::requests);
            pages.put("Profile", StudentScreens::profile);
            pages.put("Help", StudentScreens::help);
        }
        HBox nav = new HBox(4);
        nav.setAlignment(Pos.CENTER_LEFT);
        pages.forEach((name, page) -> {
            Button button = new Button(name);
            button.getStyleClass().add("nav");
            button.setOnAction(e -> act(() -> show(page.get())));
            nav.getChildren().add(button);
        });

        Button signOut = new Button("Sign out");
        signOut.getStyleClass().add("header-button");
        signOut.setOnAction(e -> {
            Accounts.signOut();
            showLogin();
            ok("You have been signed out.");
        });
        HBox header = new HBox(18, brand(), nav, Ui.grow(), Ui.text(user.name(), "header-user"), signOut);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("site-header");
        return header;
    }

    public static HBox brand() {
        Label symbol = Ui.text("GP", "brand-symbol");
        VBox words = new VBox(Ui.text("Grade Predictor", "brand-name"), Ui.text("THAPAR INSTITUTE", "brand-small"));
        HBox brand = new HBox(10, symbol, words);
        brand.setAlignment(Pos.CENTER_LEFT);
        return brand;
    }

    private static HBox footer() {
        HBox footer = new HBox(Ui.text("Thapar Grade Predictor"), Ui.grow(),
                Ui.text("Built for clarity. Designed for privacy."), Ui.grow(),
                Ui.text("Student initiative · Estimates, not official results"));
        footer.getStyleClass().add("site-footer");
        return footer;
    }
}
