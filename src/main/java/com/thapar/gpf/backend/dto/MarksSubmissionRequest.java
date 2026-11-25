package com.thapar.gpf.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Total marks are mandatory; the breakdown is optional (null when left blank). */
public record MarksSubmissionRequest(
        @NotNull(message = "Total marks are required")
        @DecimalMin(value = "0.0", message = "Total marks must be between 0 and 100")
        @DecimalMax(value = "100.0", message = "Total marks must be between 0 and 100")
        Double totalMarks,

        @DecimalMin(value = "0.0", message = "EST must be between 0 and 40")
        @DecimalMax(value = "40.0", message = "EST must be between 0 and 40")
        Double estMarks,

        @DecimalMin(value = "0.0", message = "MST must be between 0 and 30")
        @DecimalMax(value = "30.0", message = "MST must be between 0 and 30")
        Double mstMarks,

        @DecimalMin(value = "0.0", message = "Quizzes must be between 0 and 10")
        @DecimalMax(value = "10.0", message = "Quizzes must be between 0 and 10")
        Double quizMarks,

        @DecimalMin(value = "0.0", message = "Assignments must be between 0 and 20")
        @DecimalMax(value = "20.0", message = "Assignments must be between 0 and 20")
        Double assignmentMarks,

        @DecimalMin(value = "0.0", message = "Labs must be between 0 and 20")
        @DecimalMax(value = "20.0", message = "Labs must be between 0 and 20")
        Double labMarks,

        @DecimalMin(value = "0.0", message = "Practical must be between 0 and 20")
        @DecimalMax(value = "20.0", message = "Practical must be between 0 and 20")
        Double practicalMarks) {
}
