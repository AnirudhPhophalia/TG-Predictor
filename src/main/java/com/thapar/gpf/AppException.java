package com.thapar.gpf;

/** A rule was broken (invalid input, locked marks, not found...). The message is shown to the user as is. */
public class AppException extends RuntimeException {
    public AppException(String message) {
        super(message);
    }
}
