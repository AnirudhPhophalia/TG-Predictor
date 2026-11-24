package com.thapar.gpf.database.repository;

import com.thapar.gpf.database.tables.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsByCode(String code);

    Optional<Subject> findByCodeIgnoreCase(String code);

    List<Subject> findAllByOrderByCodeAsc();

    List<Subject> findByActiveTrueOrderByCodeAsc();

    long countByActiveTrue();
}
