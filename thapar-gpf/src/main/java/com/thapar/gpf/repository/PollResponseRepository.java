package com.thapar.gpf.repository;

import com.thapar.gpf.model.PollResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PollResponseRepository extends JpaRepository<PollResponse, Long> {

    boolean existsByPollIdAndUserId(Long pollId, Long userId);

    // Anonymous scores only - no user information.
    @Query("select r.marks from PollResponse r where r.poll.id = :pollId")
    List<Double> findMarksByPollId(Long pollId);
}
