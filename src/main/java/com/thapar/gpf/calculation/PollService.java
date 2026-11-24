package com.thapar.gpf.calculation;

import com.thapar.gpf.AppException;
import com.thapar.gpf.database.Cutoffs;
import com.thapar.gpf.database.Poll;
import com.thapar.gpf.database.Settings;
import com.thapar.gpf.database.Subject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Subject polls: admins ask, students answer once, admins see only aggregates, students see their own prediction. */
public final class PollService {

    private PollService() {
    }

    /** Aggregate view of a poll. Individual answers are never exposed. */
    public record Summary(long pollId, String question, String subjectCode, boolean open, LocalDateTime endTime,
                          int responses, double average, double median, double highest, double lowest,
                          int[] buckets, boolean hidden) {
    }

    public static long create(long subjectId, String question, int durationHours) {
        if (question == null || question.isBlank() || question.trim().length() > 300) {
            throw new AppException("Poll question is required.");
        }
        if (durationHours < 1 || durationHours > 24 * 30) {
            throw new AppException("Duration must be between 1 hour and 30 days.");
        }
        Subject.get(subjectId);
        return Poll.insert(subjectId, question.trim(), LocalDateTime.now().plusHours(durationHours));
    }

    public static Poll get(long pollId) {
        return Poll.byId(pollId).orElseThrow(() -> new AppException("Poll not found"));
    }

    public static void close(long pollId) {
        Poll.close(get(pollId).id());
    }

    public static void setManualCutoffs(long pollId, Cutoffs c) {
        CutoffService.validate(c);
        Poll.setManualCutoffs(get(pollId).id(), c);
    }

    // ---------- student side ----------

    public static void respond(long studentId, long pollId, Double marks) {
        Settings.requireSubmissionsOpen();
        Poll poll = get(pollId);
        if (!poll.isOpen()) throw new AppException("This poll is closed.");
        if (marks == null || !Double.isFinite(marks) || marks < 0 || marks > 100) {
            throw new AppException("Marks must be between 0 and 100.");
        }
        if (Poll.hasResponded(pollId, studentId)) throw new AppException("You have already answered this poll.");
        Poll.addAnswer(pollId, studentId, marks);
    }

    /**
     * The student's own predicted grade from their answer. Manual poll cutoffs, when set,
     * take precedence over percentile grading for this poll only.
     */
    public static Optional<GradeCalculator.Prediction> myPrediction(long pollId, long studentId) {
        Poll poll = get(pollId);
        return Poll.myAnswer(pollId, studentId).map(answer -> {
            List<Double> answers = Poll.answers(pollId);
            if (poll.manualCutoffs() != null) {
                Grade grade = answer < GradeCalculator.PASS_MARKS ? Grade.E
                        : GradeCalculator.gradeFromCutoffs(answer, poll.manualCutoffs());
                return new GradeCalculator.Prediction(grade, GradeCalculator.percentile(answer, answers), false);
            }
            return GradeCalculator.predict(answer, answers, Cutoffs.active(poll.subjectId()));
        });
    }

    // ---------- admin side (aggregates only) ----------

    public static List<Summary> summarizeAll() {
        return Poll.all().stream().map(PollService::summarize).toList();
    }

    public static Summary summarize(long pollId) {
        return summarize(get(pollId));
    }

    private static Summary summarize(Poll poll) {
        List<Double> answers = Poll.answers(poll.id());
        GradeCalculator.Stats stats = GradeCalculator.computeStats(answers);
        String code = poll.subject().code();
        if (stats.count() < GradeCalculator.MIN_RESPONSES_FOR_ADMIN) {
            // too few answers: the numbers would reveal what individual students wrote
            return new Summary(poll.id(), poll.question(), code, poll.isOpen(), poll.endTime(), stats.count(),
                    0, 0, 0, 0, new int[10], true);
        }
        return new Summary(poll.id(), poll.question(), code, poll.isOpen(), poll.endTime(), stats.count(),
                stats.mean(), stats.median(), stats.highest(), stats.lowest(), GradeCalculator.histogram(answers), false);
    }
}
