package com.thapar.gpf.backend.dto;

import jakarta.validation.constraints.NotNull;

public record CutoffRequest(
        @NotNull(message = "All cutoffs are required") Double cutoffA,
        @NotNull(message = "All cutoffs are required") Double cutoffAMinus,
        @NotNull(message = "All cutoffs are required") Double cutoffB,
        @NotNull(message = "All cutoffs are required") Double cutoffBMinus,
        @NotNull(message = "All cutoffs are required") Double cutoffC,
        @NotNull(message = "All cutoffs are required") Double cutoffD) {
}
