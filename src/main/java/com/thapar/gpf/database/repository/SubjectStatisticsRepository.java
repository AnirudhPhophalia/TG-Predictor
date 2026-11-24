package com.thapar.gpf.database.repository;

import com.thapar.gpf.database.tables.SubjectStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectStatisticsRepository extends JpaRepository<SubjectStatistics, Long> {

    Optional<SubjectStatistics> findBySubjectId(Long subjectId);
}
