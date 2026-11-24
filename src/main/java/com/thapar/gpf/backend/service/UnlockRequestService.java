package com.thapar.gpf.backend.service;

import com.thapar.gpf.backend.dto.AdminUnlockRequestView;
import com.thapar.gpf.backend.exception.BadRequestException;
import com.thapar.gpf.backend.exception.ResourceNotFoundException;
import com.thapar.gpf.database.tables.UnlockRequest;
import com.thapar.gpf.database.tables.UnlockStatus;
import com.thapar.gpf.database.tables.Mark;
import com.thapar.gpf.database.tables.User;
import com.thapar.gpf.database.repository.UnlockRequestRepository;
import com.thapar.gpf.database.repository.MarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UnlockRequestService {

    private final UnlockRequestRepository unlockRequestRepository;
    private final MarkRepository markRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private SettingsService settings;

    public UnlockRequestService(UnlockRequestRepository unlockRequestRepository, MarkRepository markRepository) {
        this.unlockRequestRepository = unlockRequestRepository;
        this.markRepository = markRepository;
    }

    // ---------- student side ----------

    @Transactional
    public UnlockRequest create(User student, Long markId, String reason) {
        settings.requireUnlockRequestsOpen();
        Mark mark = markRepository.findByIdAndUserId(markId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Marks not found"));

        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Please tell the admin why you need to edit your marks.");
        }
        if (reason.length() > 500) {
            throw new BadRequestException("Reason must be 500 characters or fewer.");
        }
        if (!mark.isLocked()) {
            throw new BadRequestException("Your marks are already unlocked. You can update them now.");
        }
        if (unlockRequestRepository.existsByMarkIdAndStatus(markId, UnlockStatus.PENDING)) {
            throw new BadRequestException("You already have a pending unlock request for this subject.");
        }

        UnlockRequest request = new UnlockRequest();
        request.setUser(student);
        request.setMark(mark);
        request.setReason(reason.trim());
        return unlockRequestRepository.save(request);
    }

    public org.springframework.data.domain.Page<UnlockRequest> findMine(Long studentId, org.springframework.data.domain.Pageable page) {
        return unlockRequestRepository.findByUserId(studentId, page);
    }

    public boolean hasPending(Long markId) {
        return unlockRequestRepository.existsByMarkIdAndStatus(markId, UnlockStatus.PENDING);
    }

    // ---------- admin side (marks are never copied into the view) ----------

    public org.springframework.data.domain.Page<AdminUnlockRequestView> findForAdmin(org.springframework.data.domain.Pageable page) {
        return unlockRequestRepository.findAll(page)
                .map(r -> new AdminUnlockRequestView(
                        r.getId(),
                        r.getUser().getName(),
                        r.getUser().getRollNumber(),
                        r.getMark().getSubject().getCode(),
                        r.getMark().getSubject().getName(),
                        r.getReason(),
                        r.getStatus(),
                        r.getRejectionReason(),
                        r.getCreatedAt()));
    }

    @Transactional
    public void approve(Long requestId) {
        UnlockRequest request = getPending(requestId);
        request.setStatus(UnlockStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.getMark().setLocked(false);
        markRepository.save(request.getMark());
        unlockRequestRepository.save(request);
    }

    @Transactional
    public void reject(Long requestId, String rejectionReason) {
        UnlockRequest request = getPending(requestId);
        request.setStatus(UnlockStatus.REJECTED);
        request.setRejectionReason(rejectionReason == null || rejectionReason.isBlank()
                ? "No reason given" : rejectionReason.trim());
        request.setReviewedAt(LocalDateTime.now());
        unlockRequestRepository.save(request);
    }

    private UnlockRequest getPending(Long requestId) {
        UnlockRequest request = unlockRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (request.getStatus() != UnlockStatus.PENDING) {
            throw new BadRequestException("This request has already been reviewed.");
        }
        return request;
    }
}
