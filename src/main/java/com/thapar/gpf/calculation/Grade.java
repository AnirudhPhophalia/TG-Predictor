package com.thapar.gpf.calculation;

public enum Grade {
    A("A"),
    A_MINUS("A-"),
    B("B"),
    B_MINUS("B-"),
    C("C"),
    D("D"),
    E("E");

    private final String label;

    Grade(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Grade points for the unweighted average on the student overview. */
    public int points() {
        return switch (this) {
            case A -> 10;
            case A_MINUS -> 9;
            case B -> 8;
            case B_MINUS -> 7;
            case C -> 6;
            case D -> 5;
            case E -> 0;
        };
    }

    // Relative grading bands (percentile = share of the cohort scoring strictly below the student).
    // The fail override (marks < 35 => E) is applied by the caller, not here.
    public static Grade fromPercentile(double percentile) {
        if (percentile >= 90) return A;
        if (percentile >= 80) return A_MINUS;
        if (percentile >= 55) return B;
        if (percentile >= 30) return B_MINUS;
        if (percentile >= 20) return C;
        return D;
    }
}
