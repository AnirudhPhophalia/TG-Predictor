package com.thapar.gpf.dto;

import com.thapar.gpf.model.Mark;

/** A student's own mark together with the aggregated stats of its subject. */
public record SubmissionRow(Mark mark, SubjectStatDTO stats) {
}
