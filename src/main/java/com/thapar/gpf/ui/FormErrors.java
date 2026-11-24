package com.thapar.gpf.ui;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;

/** Turns validation errors into plain strings for the simple error box on forms. */
public final class FormErrors {

    private FormErrors() {
    }

    public static List<String> of(BindingResult result) {
        return result.getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fe && fe.isBindingFailure()) {
                        return fe.getField() + " must be a valid number";
                    }
                    return error.getDefaultMessage();
                })
                .toList();
    }

    public static String first(BindingResult result) {
        return of(result).get(0);
    }
}
