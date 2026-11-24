package com.thapar.gpf;

import com.thapar.gpf.database.tables.Grade;
import com.thapar.gpf.database.tables.FallbackCutoff;
import com.thapar.gpf.backend.service.GradePredictionService;
import com.thapar.gpf.backend.service.GradePredictionService.Prediction;
import com.thapar.gpf.backend.service.GradePredictionService.Stats;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class GradePredictionServiceTest {

    private final GradePredictionService service = new GradePredictionService();
    private final FallbackCutoff defaults = new FallbackCutoff();

    /** 100 students scoring 36.0, 36.5 ... 85.5: the student with score 36 + k/2 has exactly k students below. */
    private List<Double> cohortOf100() {
        return IntStream.range(0, 100).mapToObj(k -> 36 + k / 2.0).toList();
    }

    private double scoreAtRank(int k) {
        return 36 + k / 2.0;
    }

    @Test
    void percentileCountsStudentsStrictlyBelow() {
        List<Double> cohort = List.of(10.0, 20.0, 20.0, 30.0);
        assertEquals(0.0, service.percentile(10, cohort));
        assertEquals(25.0, service.percentile(20, cohort));   // ties are not "below"
        assertEquals(75.0, service.percentile(30, cohort));
        assertEquals(0.0, service.percentile(50, List.of()));
    }

    @Test
    void relativeGradingBandsStartAtOneHundredSubmissions() {
        List<Double> cohort = cohortOf100();
        // rank k => percentile k
        assertEquals(Grade.A, service.predict(scoreAtRank(90), cohort, defaults).grade());
        assertEquals(Grade.A_MINUS, service.predict(scoreAtRank(89), cohort, defaults).grade());
        assertEquals(Grade.A_MINUS, service.predict(scoreAtRank(80), cohort, defaults).grade());
        assertEquals(Grade.B, service.predict(scoreAtRank(79), cohort, defaults).grade());
        assertEquals(Grade.B, service.predict(scoreAtRank(55), cohort, defaults).grade());
        assertEquals(Grade.B_MINUS, service.predict(scoreAtRank(54), cohort, defaults).grade());
        assertEquals(Grade.B_MINUS, service.predict(scoreAtRank(30), cohort, defaults).grade());
        assertEquals(Grade.C, service.predict(scoreAtRank(29), cohort, defaults).grade());
        assertEquals(Grade.C, service.predict(scoreAtRank(20), cohort, defaults).grade());
        assertEquals(Grade.D, service.predict(scoreAtRank(19), cohort, defaults).grade());
        assertEquals(Grade.D, service.predict(scoreAtRank(0), cohort, defaults).grade());
        assertTrue(service.predict(scoreAtRank(90), cohort, defaults).relative());
        assertEquals(90.0, service.predict(scoreAtRank(90), cohort, defaults).percentile());
    }

    @Test
    void failingMarksAreAlwaysEEvenInRelativeMode() {
        // 99 students below 35 means a score of 34.5 has a high percentile, but 34.5 is still a fail
        List<Double> cohort = new ArrayList<>(IntStream.range(0, 99).mapToObj(i -> 10.0 + i / 10.0).toList());
        cohort.add(34.5);
        Prediction p = service.predict(34.5, cohort, defaults);
        assertTrue(p.percentile() >= 99);
        assertEquals(Grade.E, p.grade());
        assertTrue(p.relative());

    }

    @Test
    void fallsBackToHistoricalCutoffsBelowOneHundredSubmissions() {
        List<Double> small = List.of(40.0, 60.0, 90.0);
        assertEquals(Grade.A, service.predict(85, small, defaults).grade());
        assertEquals(Grade.A_MINUS, service.predict(75, small, defaults).grade());
        assertEquals(Grade.B, service.predict(65, small, defaults).grade());
        assertEquals(Grade.B_MINUS, service.predict(55, small, defaults).grade());
        assertEquals(Grade.C, service.predict(45, small, defaults).grade());
        assertEquals(Grade.D, service.predict(35, small, defaults).grade());
        assertEquals(Grade.E, service.predict(34.99, small, defaults).grade());
        assertFalse(service.predict(85, small, defaults).relative());
    }

    @Test
    void customCutoffsAreRespected() {
        FallbackCutoff hard = new FallbackCutoff();
        hard.setCutoffA(95);
        assertEquals(Grade.A_MINUS, service.predict(90, List.of(90.0), hard).grade());
    }

    @Test
    void statisticsMatchHandCalculation() {
        // mean 5, population std dev 2
        List<Double> scores = List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0);
        Stats stats = service.computeStats(scores);
        assertEquals(8, stats.count());
        assertEquals(5.0, stats.mean(), 1e-9);
        assertEquals(4.5, stats.median(), 1e-9);
        assertEquals(4.0, stats.mode(), 1e-9);
        assertEquals(2.0, stats.standardDeviation(), 1e-9);
        assertEquals(9.0, stats.highest());
        assertEquals(2.0, stats.lowest());
        assertEquals(0.0, stats.passPercentage());
        assertEquals(100.0, stats.failurePercentage());
    }

    @Test
    void passFailSplitUsesThirtyFive() {
        Stats stats = service.computeStats(List.of(34.9, 35.0, 80.0, 10.0));
        assertEquals(50.0, stats.passPercentage());
        assertEquals(50.0, stats.failurePercentage());
    }

    @Test
    void emptyCohortGivesZeroes() {
        Stats stats = service.computeStats(List.of());
        assertEquals(0, stats.count());
        assertEquals(0.0, stats.mean());
    }

    @Test
    void modeTieGoesToLowerScore() {
        assertEquals(10.0, service.mode(List.of(20.0, 10.0, 20.0, 10.0)));
    }

    @Test
    void histogramHasTenDecileBucketsAndHundredGoesInTheLast() {
        int[] buckets = service.histogram(List.of(0.0, 9.9, 10.0, 55.0, 99.0, 100.0));
        assertEquals(10, buckets.length);
        assertEquals(2, buckets[0]);
        assertEquals(1, buckets[1]);
        assertEquals(1, buckets[5]);
        assertEquals(2, buckets[9]);
    }
}
