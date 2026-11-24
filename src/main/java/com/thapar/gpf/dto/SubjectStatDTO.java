package com.thapar.gpf.dto;

import com.thapar.gpf.model.Subject;
import com.thapar.gpf.model.SubjectStatistics;

/** Aggregated numbers for one subject. No student identity anywhere in here. */
public record SubjectStatDTO(
        Long subjectId,
        String code,
        String name,
        int totalResponses,
        double average,
        double median,
        double mode,
        double highest,
        double lowest,
        double standardDeviation,
        double passPercentage,
        double failurePercentage,
        int gradeA, int gradeAMinus, int gradeB, int gradeBMinus, int gradeC, int gradeD, int gradeE,
        boolean hidden) {

    public static SubjectStatDTO from(Subject subject, SubjectStatistics s) {
        if (s == null) {
            return new SubjectStatDTO(subject.getId(), subject.getCode(), subject.getName(),
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        }
        return new SubjectStatDTO(subject.getId(), subject.getCode(), subject.getName(),
                s.getTotalResponses(), s.getAverageMarks(), s.getMedianMarks(), s.getModeMarks(),
                s.getHighestMarks(), s.getLowestMarks(), s.getStandardDeviation(),
                s.getPassPercentage(), s.getFailurePercentage(),
                s.getGradeA(), s.getGradeAMinus(), s.getGradeB(), s.getGradeBMinus(),
                s.getGradeC(), s.getGradeD(), s.getGradeE(), false);
    }

    /**
     * The version of these numbers that admins may see: with too few responses the
     * numbers would reveal individual marks, so only the response count is kept.
     */
    public SubjectStatDTO forAdmin(int minResponses) {
        if (totalResponses >= minResponses) {
            return this;
        }
        return new SubjectStatDTO(subjectId, code, name, totalResponses,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, true);
    }

    /** Confidence grows with the number of submissions and is 100% at 100 submissions. */
    public int confidencePercent() {
        return Math.min(100, totalResponses);
    }

    public String confidenceLabel() {
        if (totalResponses >= 100) return "High";
        if (totalResponses >= 30) return "Medium";
        return "Low";
    }

    public boolean relativeGradingActive() {
        return totalResponses >= 100;
    }
}
