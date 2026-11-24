package com.thapar.gpf.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubjectRequest(
        @NotBlank(message = "Subject code is required") String code,
        @NotBlank(message = "Subject name is required") String name,
        @NotNull(message = "Semester is required") @Min(value = 1, message = "Semester must be 1-8") @Max(value = 8, message = "Semester must be 1-8") Integer semester,
        String branch,
        @NotNull(message = "Credits are required") @Min(value = 1, message = "Credits must be 1-30") @Max(value = 30, message = "Credits must be 1-30") Double credits) {
    public SubjectRequest(String code, String name, Integer semester, String branch, int credits) {
        this(code, name, semester, branch, Double.valueOf(credits));
    }
}
