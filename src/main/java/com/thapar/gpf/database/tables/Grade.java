package com.thapar.gpf.database.tables;

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

    public String getLabel() {
        return label;
    }

    public boolean isPass() {
        return this != E;
    }

    // Relative grading bands (percentile = share of the cohort scoring below the student).
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
