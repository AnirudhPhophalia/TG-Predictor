package com.thapar.gpf.exception;

public class MarkLockedException extends BadRequestException {
    public MarkLockedException() {
        super("Your marks for this subject are already submitted and locked. Send an unlock request to edit them.");
    }
}
