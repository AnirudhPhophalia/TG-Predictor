package com.thapar.gpf.backend.dto;

import java.time.LocalDateTime;

/** Aggregate view of a poll. Individual poll answers are never exposed. */
public record PollSummaryDTO(
        Long pollId,
        String question,
        String subjectCode,
        boolean open,
        LocalDateTime endTime,
        int responses,
        double average,
        double median,
        double highest,
        double lowest,
        int[] buckets,
        boolean hidden) {
}
