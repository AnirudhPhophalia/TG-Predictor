package com.thapar.gpf.service;

import com.thapar.gpf.dto.AdminEditRequestView;
import com.thapar.gpf.exception.BadRequestException;
import com.thapar.gpf.exception.ResourceNotFoundException;
import com.thapar.gpf.model.EditRequest;
import com.thapar.gpf.model.EditStatus;
import com.thapar.gpf.model.Mark;
import com.thapar.gpf.model.User;
import com.thapar.gpf.repository.EditRequestRepository;
import com.thapar.gpf.repository.MarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EditRequestService {

    private final EditRequestRepository editRequestRepository;
    private final MarkRepository markRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private SettingsService settings;

    public EditRequestService(EditRequestRepository editRequestRepository, MarkRepository markRepository) {
        this.editRequestRepository = editRequestRepository;
        this.markRepository = markRepository;
    }

    // ---------- student side ----------

    @Transactional
    public EditRequest create(User student, Long markId, String reason) {
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
        if (editRequestRepository.existsByMarkIdAndStatus(markId, EditStatus.PENDING)) {
            throw new BadRequestException("You already have a pending unlock request for this subject.");
        }

        EditRequest request = new EditRequest();
        request.setUser(student);
        request.setMark(mark);
        request.setReason(reason.trim());
        return editRequestRepository.save(request);
    }

    public org.springframework.data.domain.Page<EditRequest> findMine(Long studentId, org.springframework.data.domain.Pageable page) {
        return editRequestRepository.findByUserId(studentId, page);
    }

    public boolean hasPending(Long markId) {
        return editRequestRepository.existsByMarkIdAndStatus(markId, EditStatus.PENDING);
    }

    // ---------- admin side (marks are never copied into the view) ----------

    public org.springframework.data.domain.Page<AdminEditRequestView> findForAdmin(org.springframework.data.domain.Pageable page) {
        return editRequestRepository.findAll(page)
                .map(r -> new AdminEditRequestView(
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
        EditRequest request = getPending(requestId);
        request.setStatus(EditStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.getMark().setLocked(false);
        markRepository.save(request.getMark());
        editRequestRepository.save(request);
    }

    @Transactional
    public void reject(Long requestId, String rejectionReason) {
        EditRequest request = getPending(requestId);
        request.setStatus(EditStatus.REJECTED);
        request.setRejectionReason(rejectionReason == null || rejectionReason.isBlank()
                ? "No reason given" : rejectionReason.trim());
        request.setReviewedAt(LocalDateTime.now());
        editRequestRepository.save(request);
    }

    private EditRequest getPending(Long requestId) {
        EditRequest request = editRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (request.getStatus() != EditStatus.PENDING) {
            throw new BadRequestException("This request has already been reviewed.");
        }
        return request;
    }
}
