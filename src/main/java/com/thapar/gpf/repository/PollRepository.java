package com.thapar.gpf.repository;

import com.thapar.gpf.model.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PollRepository extends JpaRepository<Poll, Long> {


    List<Poll> findByActiveTrueAndEndTimeAfterOrderByEndTimeAsc(LocalDateTime now);

    long countByActiveTrueAndEndTimeAfter(LocalDateTime now);
}
