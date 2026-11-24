package com.thapar.gpf.calculation;

import com.thapar.gpf.AppException;
import com.thapar.gpf.database.Cutoffs;
import com.thapar.gpf.database.Database;
import com.thapar.gpf.database.Mark;
import com.thapar.gpf.database.Settings;
import com.thapar.gpf.database.Subject;
import com.thapar.gpf.database.SubjectStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * From a submitted score to a predicted grade. A submission is validated, stored and locked,
 * then every grade of that subject is recalculated, because each percentile depends on the
 * whole class. Not used by the admin screens.
 */
public final class MarksService {

    public static final String LOCKED_MESSAGE =
            "Your marks for this subject are already submitted and locked. Send an unlock request to edit them.";

    private static final double[] MAXIMUMS = {100, 30, 40, 10, 20, 20, 20};

    private MarksService() {
    }

    /** Total marks are mandatory; the breakdown is optional (null when left blank). */
    public record Submission(Double totalMarks, Double estMarks, Double mstMarks, Double quizMarks,
                             Double assignmentMarks, Double labMarks, Double practicalMarks) {
        Double[] values() {
            return new Double[]{totalMarks, estMarks, mstMarks, quizMarks, assignmentMarks, labMarks, practicalMarks};
        }
    }

    public static Mark submit(long studentId, long subjectId, Submission s) {
        Settings.requireSubmissionsOpen();
        if (s.totalMarks() == null) throw new AppException("Total marks are required");
        Double[] values = s.values();
        for (int i = 0; i < values.length; i++) {
            Double value = values[i];
            if ((i == 0 && value == null)
                    || (value != null && (!Double.isFinite(value) || value < 0 || value > MAXIMUMS[i]))) {
                throw new AppException("Enter valid marks within the displayed limits.");
            }
        }
        Subject subject = Subject.get(subjectId);
        if (!subject.active()) throw new AppException("This subject is not open for submissions.");

        Optional<Mark> existing = Mark.mineForSubject(studentId, subjectId);
        if (existing.isPresent() && existing.get().locked()) throw new AppException(LOCKED_MESSAGE);

        return Database.transaction(() -> {
            long id = Mark.store(existing.map(Mark::id).orElse(null), studentId, subjectId, s.totalMarks(),
                    s.estMarks(), s.mstMarks(), s.quizMarks(), s.assignmentMarks(), s.labMarks(), s.practicalMarks());
            recalculate(subjectId);
            return Mark.mine(studentId, id).orElseThrow();
        });
    }

    /** Recomputes every percentile and grade of a subject, then refreshes its cached statistics. */
    public static void recalculate(long subjectId) {
        List<Mark.Score> marks = Mark.scoresWithIds(subjectId);
        List<Double> scores = marks.stream().map(Mark.Score::totalMarks).toList();
        Cutoffs cutoffs = Cutoffs.active(subjectId);

        int[] gradeCounts = new int[Grade.values().length];
        List<Object[]> predictions = new ArrayList<>();
        for (Mark.Score mark : marks) {
            GradeCalculator.Prediction p = GradeCalculator.predict(mark.totalMarks(), scores, cutoffs);
            gradeCounts[p.grade().ordinal()]++;
            predictions.add(new Object[]{p.grade(), p.percentile(), mark.markId()});
        }
        Database.inTransaction(() -> {
            Mark.savePredictions(predictions);
            SubjectStats.save(subjectId, GradeCalculator.computeStats(scores), gradeCounts);
        });
    }

    /** Anonymous score distribution of a subject, for the histogram on the student's detail page. */
    public static int[] scoreBuckets(long subjectId) {
        return GradeCalculator.histogram(Mark.scores(subjectId));
    }
}
