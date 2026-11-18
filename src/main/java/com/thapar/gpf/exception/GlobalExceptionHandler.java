package com.thapar.gpf.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException e) {
        return errorPage(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // Also covers MarkLockedException: a second submission gets HTTP 400.
    @ExceptionHandler(BadRequestException.class)
    public ModelAndView handleBadRequest(BadRequestException e) {
        return errorPage(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // e.g. two quick double-clicks hitting a unique constraint
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDuplicate(DataIntegrityViolationException e) {
        return errorPage(HttpStatus.BAD_REQUEST, "That record already exists.");
    }

    private ModelAndView errorPage(HttpStatus status, String message) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("message", message);
        return mav;
    }
}
