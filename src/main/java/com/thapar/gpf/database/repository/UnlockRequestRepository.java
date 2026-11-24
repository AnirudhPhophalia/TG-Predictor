package com.thapar.gpf.database.repository;

import com.thapar.gpf.database.tables.UnlockRequest;
import com.thapar.gpf.database.tables.UnlockStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnlockRequestRepository extends JpaRepository<UnlockRequest, Long> {

    List<UnlockRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    org.springframework.data.domain.Page<UnlockRequest> findByUserId(Long userId, org.springframework.data.domain.Pageable page);

    boolean existsByMarkIdAndStatus(Long markId, UnlockStatus status);

    long countByStatus(UnlockStatus status);
}
