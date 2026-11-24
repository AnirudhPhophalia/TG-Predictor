package com.thapar.gpf.repository;

import com.thapar.gpf.model.EditRequest;
import com.thapar.gpf.model.EditStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EditRequestRepository extends JpaRepository<EditRequest, Long> {

    List<EditRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    org.springframework.data.domain.Page<EditRequest> findByUserId(Long userId, org.springframework.data.domain.Pageable page);

    boolean existsByMarkIdAndStatus(Long markId, EditStatus status);

    long countByStatus(EditStatus status);
}
