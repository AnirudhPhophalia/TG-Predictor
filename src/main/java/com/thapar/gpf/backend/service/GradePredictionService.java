package com.thapar.gpf.backend.service;

import com.thapar.gpf.database.tables.Grade;
import com.thapar.gpf.database.tables.FallbackCutoff;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Pure math: percentiles, grade prediction and descriptive statistics (no database access). */
@Service
public class GradePredictionService {

    public static final int MIN_SUBMISSIONS_FOR_RELATIVE = 100;
    public static final double PASS_MARKS = 35.0;

    // Admins only see combined numbers once at least this many students contributed.
    // With 1 response the "average" would simply be that one student's marks.
    public static final int MIN_RESPONSES_FOR_ADMIN = 5;

    /** Result of predicting one student's grade. */
    public record Prediction(Grade grade, double percentile, boolean relative) {
    }

    /** Descriptive statistics of a list of scores. */
    public record Stats(int count, double mean, double median, double mode,
                        double highest, double lowest, double standardDeviation,
                        double passPercentage, double failurePercentage) {
    }

    // percentile = (students with marks strictly below) / (all students) * 100
    public double percentile(double marks, List<Double> allMarks) {
        if (allMarks.isEmpty()) {
            return 0;
        }
        long below = allMarks.stream().filter(m -> m < marks).count();
        return below * 100.0 / allMarks.size();
    }

    public Prediction predict(double marks, List<Double> allMarks, FallbackCutoff cutoff) {
        double percentile = percentile(marks, allMarks);

        if (marks < PASS_MARKS) {
            return new Prediction(Grade.E, percentile, allMarks.size() >= MIN_SUBMISSIONS_FOR_RELATIVE);
        }
        if (allMarks.size() >= MIN_SUBMISSIONS_FOR_RELATIVE) {
            return new Prediction(Grade.fromPercentile(percentile), percentile, true);
        }
        return new Prediction(gradeFromCutoffs(marks, cutoff), percentile, false);
    }

    public Grade gradeFromCutoffs(double marks, FallbackCutoff cutoff) {
        if (marks >= cutoff.getCutoffA()) return Grade.A;
        if (marks >= cutoff.getCutoffAMinus()) return Grade.A_MINUS;
        if (marks >= cutoff.getCutoffB()) return Grade.B;
        if (marks >= cutoff.getCutoffBMinus()) return Grade.B_MINUS;
        if (marks >= cutoff.getCutoffC()) return Grade.C;
        if (marks >= cutoff.getCutoffD()) return Grade.D;
        return Grade.E;
    }

    public Stats computeStats(List<Double> scores) {
        int n = scores.size();
        if (n == 0) {
            return new Stats(0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        double mean = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = scores.stream().mapToDouble(x -> (x - mean) * (x - mean)).sum() / n;
        double highest = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double lowest = scores.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        long passed = scores.stream().filter(x -> x >= PASS_MARKS).count();

        return new Stats(n, mean, median(scores), mode(scores), highest, lowest,
                Math.sqrt(variance), passed * 100.0 / n, (n - passed) * 100.0 / n);
    }

    public double median(List<Double> scores) {
        List<Double> sorted = scores.stream().sorted().toList();
        int n = sorted.size();
        if (n == 0) return 0;
        if (n % 2 == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    // Most frequent score; on a tie the lower score wins.
    public double mode(List<Double> scores) {
        Map<Double, Long> counts = scores.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.entrySet().stream()
                .max(Map.Entry.<Double, Long>comparingByValue()
                        .thenComparing(Map.Entry.<Double, Long>comparingByKey(Comparator.reverseOrder())))
                .map(Map.Entry::getKey)
                .orElse(0.0);
    }

    /** Ten buckets: 0-10, 10-20, ... 90-100 (a score of exactly 100 goes in the last bucket). */
    public int[] histogram(List<Double> scores) {
        int[] buckets = new int[10];
        scores.forEach(x -> buckets[Math.min(9, (int) (x / 10))]++);
        return buckets;
    }
}
