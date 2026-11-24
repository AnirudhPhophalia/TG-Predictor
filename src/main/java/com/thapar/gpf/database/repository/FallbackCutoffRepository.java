package com.thapar.gpf.database.repository;

import com.thapar.gpf.database.tables.FallbackCutoff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FallbackCutoffRepository extends JpaRepository<FallbackCutoff, Long> {

    Optional<FallbackCutoff> findBySubjectId(Long subjectId);
}
