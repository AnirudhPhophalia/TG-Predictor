package com.thapar.gpf.service;

import com.thapar.gpf.dto.PollSummaryDTO;
import com.thapar.gpf.exception.BadRequestException;
import com.thapar.gpf.exception.ResourceNotFoundException;
import com.thapar.gpf.model.Poll;
import com.thapar.gpf.model.PollResponse;
import com.thapar.gpf.model.User;
import com.thapar.gpf.repository.PollRepository;
import com.thapar.gpf.repository.PollResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PollService {

    private final PollRepository pollRepository;
    private final PollResponseRepository responseRepository;
    private final SubjectService subjectService;
    private final GradePredictionService predictionService;

    public PollService(PollRepository pollRepository,
                       PollResponseRepository responseRepository,
                       SubjectService subjectService,
                       GradePredictionService predictionService) {
        this.pollRepository = pollRepository;
        this.responseRepository = responseRepository;
        this.subjectService = subjectService;
        this.predictionService = predictionService;
    }

    @Transactional
    public Poll create(Long subjectId, String question, int durationHours) {
        if (question == null || question.isBlank()) {
            throw new BadRequestException("Poll question is required.");
        }
        if (durationHours < 1 || durationHours > 24 * 30) {
            throw new BadRequestException("Duration must be between 1 hour and 30 days.");
        }
        Poll poll = new Poll();
        poll.setSubject(subjectService.get(subjectId));
        poll.setQuestion(question.trim());
        poll.setEndTime(LocalDateTime.now().plusHours(durationHours));
        return pollRepository.save(poll);
    }

    @Transactional
    public void close(Long pollId) {
        Poll poll = get(pollId);
        poll.setActive(false);
        pollRepository.save(poll);
    }

    public Poll get(Long pollId) {
        return pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll not found"));
    }

    // ---------- student side ----------

    public List<Poll> findOpenPolls() {
        return pollRepository.findByActiveTrueAndEndTimeAfterOrderByEndTimeAsc(LocalDateTime.now());
    }

    public boolean hasResponded(Long pollId, Long studentId) {
        return responseRepository.existsByPollIdAndUserId(pollId, studentId);
    }

    @Transactional
    public void respond(User student, Long pollId, Double marks) {
        Poll poll = get(pollId);
        if (!poll.isOpen()) {
            throw new BadRequestException("This poll is closed.");
        }
        if (marks == null || marks < 0 || marks > 100) {
            throw new BadRequestException("Marks must be between 0 and 100.");
        }
        if (responseRepository.existsByPollIdAndUserId(pollId, student.getId())) {
            throw new BadRequestException("You have already answered this poll.");
        }
        PollResponse response = new PollResponse();
        response.setPoll(poll);
        response.setUser(student);
        response.setMarks(marks);
        responseRepository.save(response);
    }

    // ---------- admin side (aggregates only) ----------

    public List<PollSummaryDTO> summarizeAll() {
        return pollRepository.findAllByOrderByCreatedAtDesc().stream().map(this::summarize).toList();
    }

    public PollSummaryDTO summarize(Long pollId) {
        return summarize(get(pollId));
    }

    private PollSummaryDTO summarize(Poll poll) {
        List<Double> marks = responseRepository.findMarksByPollId(poll.getId());
        GradePredictionService.Stats stats = predictionService.computeStats(marks);
        if (stats.count() < GradePredictionService.MIN_RESPONSES_FOR_ADMIN) {
            // too few answers: the numbers would reveal what individual students wrote
            return new PollSummaryDTO(poll.getId(), poll.getQuestion(), poll.getSubject().getCode(),
                    poll.isOpen(), poll.getEndTime(), stats.count(), 0, 0, 0, 0, new int[10], true);
        }
        return new PollSummaryDTO(poll.getId(), poll.getQuestion(), poll.getSubject().getCode(),
                poll.isOpen(), poll.getEndTime(), stats.count(), stats.mean(), stats.median(),
                stats.highest(), stats.lowest(), predictionService.histogram(marks), false);
    }
}
