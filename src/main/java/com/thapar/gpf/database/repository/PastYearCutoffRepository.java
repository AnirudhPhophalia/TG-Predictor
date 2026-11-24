package com.thapar.gpf.database.repository;

import com.thapar.gpf.database.tables.PastYearCutoff;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PastYearCutoffRepository extends JpaRepository<PastYearCutoff, Long> {
    Optional<PastYearCutoff> findBySubjectIdAndAcademicYear(Long subjectId, int academicYear);
}
