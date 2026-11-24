package com.thapar.gpf;

import javafx.application.Application;

/**
 * Plain launcher. It does not extend Application, which lets the app start from
 * VS Code's Run button and plain "java" with JavaFX on the classpath.
 *
 * Arguments: --demo uses the separate demo database with sample accounts and data.
 */
public class Main {
    public static void main(String[] args) {
        // Apache POI logs through Log4j; this keeps the console free of "no logging implementation" noise.
        System.setProperty("log4j2.loggerContextFactory", "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
        Application.launch(App.class, args);
    }
}
