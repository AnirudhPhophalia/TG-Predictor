package com.thapar.gpf.repository;

import com.thapar.gpf.model.HistoricalCutoff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HistoricalCutoffRepository extends JpaRepository<HistoricalCutoff, Long> {

    Optional<HistoricalCutoff> findBySubjectId(Long subjectId);
}
