package com.thapar.gpf.database.repository;

import com.thapar.gpf.database.tables.PollResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PollResponseRepository extends JpaRepository<PollResponse, Long> {

    boolean existsByPollIdAndUserId(Long pollId, Long userId);
    java.util.Optional<PollResponse> findByPollIdAndUserId(Long pollId, Long userId);

    // Anonymous scores only - no user information.
    @Query("select r.marks from PollResponse r where r.poll.id = :pollId")
    List<Double> findMarksByPollId(Long pollId);
}
