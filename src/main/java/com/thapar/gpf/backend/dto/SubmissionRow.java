package com.thapar.gpf.backend.dto;

import com.thapar.gpf.database.tables.Mark;

/** A student's own mark together with the aggregated stats of its subject. */
public record SubmissionRow(Mark mark, SubjectStatDTO stats) {
}
