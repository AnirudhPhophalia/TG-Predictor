package com.thapar.gpf.backend.service;

import com.thapar.gpf.backend.dto.MarksSubmissionRequest;
import com.thapar.gpf.backend.exception.BadRequestException;
import com.thapar.gpf.backend.exception.MarkLockedException;
import com.thapar.gpf.backend.exception.ResourceNotFoundException;
import com.thapar.gpf.database.tables.*;
import com.thapar.gpf.database.repository.MarkRepository;
import com.thapar.gpf.database.repository.SubjectStatisticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class MarksService {

    private final MarkRepository markRepository;
    private final SubjectStatisticsRepository statisticsRepository;
    private final SubjectService subjectService;
    private final GradePredictionService predictionService;
    @org.springframework.beans.factory.annotation.Autowired
    private SettingsService settings;

    public MarksService(MarkRepository markRepository,
                        SubjectStatisticsRepository statisticsRepository,
                        SubjectService subjectService,
                        GradePredictionService predictionService) {
        this.markRepository = markRepository;
        this.statisticsRepository = statisticsRepository;
        this.subjectService = subjectService;
        this.predictionService = predictionService;
    }

    public List<Mark> findMyMarks(Long studentId) {
        return markRepository.findByUserId(studentId).stream()
                .sorted(Comparator.comparing(Mark::getSubmittedAt).reversed())
                .toList();
    }

    @org.springframework.context.event.EventListener
    @Transactional
    public void cutoffsChanged(CutoffsChangedEvent event) {
        recalculate(subjectService.get(event.subjectId()));
    }

    public Optional<Mark> findMyMarkForSubject(Long studentId, Long subjectId) {
        return markRepository.findByUserIdAndSubjectId(studentId, subjectId);
    }

    public Mark getMyMark(Long markId, Long studentId) {
        return markRepository.findByIdAndUserId(markId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Marks not found"));
    }

    /** Anonymous scores of a subject, used for the histogram on the student's detail page. */
    public int[] scoreBuckets(Long subjectId) {
        return predictionService.histogram(markRepository.findTotalMarksBySubjectId(subjectId));
    }

    @Transactional
    public Mark submit(User student, Long subjectId, MarksSubmissionRequest request) {
        settings.requireSubmissionsOpen();
        Double[] values = {request.totalMarks(), request.estMarks(), request.mstMarks(), request.quizMarks(),
                request.assignmentMarks(), request.labMarks(), request.practicalMarks()};
        double[] maximums = {100, 40, 30, 10, 20, 20, 20};
        for (int i = 0; i < values.length; i++) {
            Double value = values[i];
            if ((i == 0 && value == null) || (value != null
                    && (!Double.isFinite(value) || value < 0 || value > maximums[i]))) {
                throw new BadRequestException("Enter valid marks within the displayed limits.");
            }
        }
        Subject subject = subjectService.get(subjectId);
        if (!subject.isActive()) {
            throw new BadRequestException("This subject is not open for submissions.");
        }

        Optional<Mark> existing = markRepository.findByUserIdAndSubjectId(student.getId(), subjectId);
        if (existing.isPresent() && existing.get().isLocked()) {
            throw new MarkLockedException();
        }

        Mark mark = existing.orElseGet(Mark::new);
        mark.setUser(student);
        mark.setSubject(subject);
        mark.setTotalMarks(request.totalMarks());
        mark.setEstMarks(request.estMarks());
        mark.setMstMarks(request.mstMarks());
        mark.setQuizMarks(request.quizMarks());
        mark.setAssignmentMarks(request.assignmentMarks());
        mark.setLabMarks(request.labMarks());
        mark.setPracticalMarks(request.practicalMarks());
        mark.setLocked(true);
        mark.setSubmittedAt(LocalDateTime.now());
        markRepository.saveAndFlush(mark);

        recalculate(subject);
        return mark;
    }

    /**
     * Percentiles and grades depend on the whole cohort, so after every change we
     * recompute them for every mark of the subject and refresh the cached statistics.
     */
    @Transactional
    public void recalculate(Subject subject) {
        List<Mark> marks = markRepository.findBySubjectId(subject.getId());
        List<Double> scores = marks.stream().map(Mark::getTotalMarks).toList();
        FallbackCutoff cutoff = subjectService.getCutoff(subject);

        SubjectStatistics stats = statisticsRepository.findBySubjectId(subject.getId()).orElseGet(() -> {
            SubjectStatistics s = new SubjectStatistics();
            s.setSubject(subject);
            return s;
        });
        stats.setGradeA(0);
        stats.setGradeAMinus(0);
        stats.setGradeB(0);
        stats.setGradeBMinus(0);
        stats.setGradeC(0);
        stats.setGradeD(0);
        stats.setGradeE(0);

        for (Mark mark : marks) {
            GradePredictionService.Prediction prediction =
                    predictionService.predict(mark.getTotalMarks(), scores, cutoff);
            mark.setPercentile(prediction.percentile());
            mark.setPredictedGrade(prediction.grade());
            switch (prediction.grade()) {
                case A -> stats.setGradeA(stats.getGradeA() + 1);
                case A_MINUS -> stats.setGradeAMinus(stats.getGradeAMinus() + 1);
                case B -> stats.setGradeB(stats.getGradeB() + 1);
                case B_MINUS -> stats.setGradeBMinus(stats.getGradeBMinus() + 1);
                case C -> stats.setGradeC(stats.getGradeC() + 1);
                case D -> stats.setGradeD(stats.getGradeD() + 1);
                case E -> stats.setGradeE(stats.getGradeE() + 1);
            }
        }
        markRepository.saveAll(marks);

        GradePredictionService.Stats result = predictionService.computeStats(scores);
        stats.setTotalResponses(result.count());
        stats.setAverageMarks(result.mean());
        stats.setMedianMarks(result.median());
        stats.setModeMarks(result.mode());
        stats.setHighestMarks(result.highest());
        stats.setLowestMarks(result.lowest());
        stats.setStandardDeviation(result.standardDeviation());
        stats.setPassPercentage(result.passPercentage());
        stats.setFailurePercentage(result.failurePercentage());
        stats.setLastUpdated(LocalDateTime.now());
        statisticsRepository.save(stats);
    }
}
