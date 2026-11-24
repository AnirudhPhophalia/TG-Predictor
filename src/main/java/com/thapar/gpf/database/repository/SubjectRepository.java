package com.thapar.gpf.database.repository;

import com.thapar.gpf.database.tables.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsByCode(String code);

    List<Subject> findAllByOrderByCodeAsc();

    List<Subject> findByActiveTrueOrderByCodeAsc();

    long countByActiveTrue();
}
