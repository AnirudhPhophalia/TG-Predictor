package com.thapar.gpf.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One student's marks in one subject. Only the owning student and the
 * statistics engine ever read this entity; no admin code path touches it.
 */
@Entity
@Table(name = "marks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "subject_id"}),
        indexes = {@Index(columnList = "user_id"), @Index(columnList = "subject_id")})
@Getter
@Setter
@NoArgsConstructor
public class Mark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false)
    private double totalMarks;

    // optional breakdown
    private Double estMarks;
    private Double mstMarks;
    private Double quizMarks;
    private Double assignmentMarks;
    private Double labMarks;
    private Double practicalMarks;

    @Enumerated(EnumType.STRING)
    private Grade predictedGrade;

    private Double percentile;

    @Column(name = "is_locked", nullable = false)
    private boolean locked = true;

    private LocalDateTime submittedAt;
}
