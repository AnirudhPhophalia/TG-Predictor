package com.thapar.gpf.login;

import com.thapar.gpf.App;
import com.thapar.gpf.AppException;
import com.thapar.gpf.Config;
import com.thapar.gpf.screens.Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/** The sign-in page (Google, plus email and password in demo mode) and the demo registration page. */
public final class LoginScreen {

    private LoginScreen() {
    }

    public static Node view() {
        HBox top = new HBox(App.brand(), Ui.grow(), Ui.eyebrow("A LITTLE CLARITY GOES A LONG WAY"));
        top.setAlignment(Pos.CENTER_LEFT);

        HBox columns = new HBox(48, story(), panel());
        HBox.setHgrow(columns.getChildren().get(0), Priority.ALWAYS);

        Node ticker = Ui.text("KNOW YOUR COHORT  ·  FIND YOUR PERSPECTIVE  ·  KEEP YOUR MARKS PRIVATE  ·  "
                + "PLAN YOUR NEXT CHAPTER", "ticker");
        VBox page = Ui.page(top, columns, ticker);
        page.getStyleClass().add("login-page");
        return page;
    }

    private static Node story() {
        SVGPath fill = new SVGPath();
        fill.setContent("M20 135 C100 135 130 130 175 86 S230 20 270 20 S320 45 365 86 S440 135 520 135 L520 150 L20 150Z");
        fill.getStyleClass().add("curve-fill");
        SVGPath line = new SVGPath();
        line.setContent("M20 135 C100 135 130 130 175 86 S230 20 270 20 S320 45 365 86 S440 135 520 135");
        line.getStyleClass().add("curve-line");
        StackPane curve = new StackPane(fill, line);
        curve.setAlignment(Pos.BOTTOM_CENTER);

        VBox curveCard = Ui.card(Ui.row(Ui.eyebrow("THE BIGGER PICTURE"), Ui.grow(), Ui.muted("Every contribution brings clarity")),
                curve, Ui.text("Illustrative distribution · Your insights use actual submissions", "small", "muted"));

        return new VBox(18, Ui.eyebrow("●  FOR THE THAPAR COMMUNITY"),
                Ui.text("Your marks.\nYour perspective.\nClearer possibilities.", "hero"),
                Ui.text("Relative grading can feel uncertain.\nFind your place in the curve, and a little peace of mind.", "lead"),
                curveCard);
    }

    private static Node panel() {
        VBox card = Ui.card(Ui.eyebrow("WELCOME TO YOUR NEXT CHAPTER"), Ui.h2("Understand where\nyou stand."),
                Ui.muted("Your academic insights, all in one place."));

        if (Config.googleEnabled) {
            Button google = Ui.button("G    Continue with Google    →", LoginScreen::googleSignIn);
            google.getStyleClass().add("google-button");
            google.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().add(google);
        } else {
            card.getChildren().add(Ui.notice("Google sign-in is awaiting configuration. Follow the Google login setup "
                    + "in the README to connect your institute account."));
        }
        card.getChildren().addAll(
                Ui.text("Exclusively for @thapar.edu accounts.\nYour admission year is detected from your institute email.", "small", "muted"),
                feature("01", "A clearer prediction", "Historical cutoffs and relative ranking"),
                feature("02", "See the bigger picture", "Live cohort statistics and distributions"),
                feature("03", "Private by design", "Your individual marks stay yours"));
        if (Config.demo) card.getChildren().add(demoSignIn());
        card.setPrefWidth(430);
        card.setMaxWidth(430);

        return new VBox(12, card, Ui.text("An independent student initiative. Predictions are estimates.", "small", "muted"));
    }

    private static Node feature(String number, String title, String caption) {
        HBox row = Ui.row(Ui.text(number, "feature-number"), new VBox(Ui.h3(title), Ui.text(caption, "small", "muted")));
        row.getStyleClass().add("feature");
        return row;
    }

    private static Node demoSignIn() {
        TextField email = Ui.field(null, "you@thapar.edu");
        PasswordField password = new PasswordField();
        Button signIn = Ui.button("Sign in to demo", () -> App.signedIn(Accounts.login(email.getText(), password.getText())));
        signIn.setDefaultButton(true);
        signIn.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(10,
                Ui.text("Student: aarav@thapar.edu / student123\nAdmin: " + Config.adminEmail + " / admin123", "small", "muted"),
                Ui.labeled("Institute email", email), Ui.labeled("Password", password), signIn,
                Ui.link("Create a demo student account", () -> App.show(register())));
        TitledPane pane = new TitledPane("Local demo sign-in", form);
        pane.getStyleClass().add("demo-login");
        return pane;
    }

    private static void googleSignIn() {
        if (Config.googleClientId.isBlank() || Config.googleClientSecret.isBlank()) {
            throw new AppException("Google login requires GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.");
        }
        App.ok("Finish signing in with Google in your browser. This window updates when you are done.");
        GoogleLogin.start(App::openBrowser,
                identity -> App.act(() -> App.signedIn(Accounts.googleLogin(identity.email(), identity.name(), identity.subject()))),
                App::fail);
    }

    public static Node register() {
        TextField name = Ui.field(null, "Full name");
        TextField email = Ui.field(null, "you@thapar.edu");
        PasswordField password = new PasswordField();
        TextField roll = Ui.field(null, "e.g. 102203001");
        TextField branch = Ui.field(null, "COE");
        TextField semester = Ui.field(null, "1-8");

        Button create = Ui.button("Register", () -> {
            Accounts.register(name.getText(), email.getText(), password.getText(), roll.getText(), branch.getText(),
                    Ui.whole(semester, "Semester"));
            App.show(view());
            App.ok("Account created. You can log in now.");
        });
        create.setDefaultButton(true);

        VBox card = Ui.card(Ui.h1("Create account"), Ui.muted("Only @thapar.edu emails are accepted."),
                Ui.labeled("Full name", name), Ui.labeled("Email", email),
                Ui.labeled("Password (min 6 characters)", password), Ui.labeled("Roll number", roll),
                Ui.labeled("Branch", branch), Ui.labeled("Semester", semester), create,
                Ui.row(Ui.muted("Already have an account?"), Ui.link("Login", () -> App.show(view()))));
        card.setMaxWidth(460);
        VBox page = Ui.page(card);
        page.setAlignment(Pos.TOP_CENTER);
        return page;
    }
}
