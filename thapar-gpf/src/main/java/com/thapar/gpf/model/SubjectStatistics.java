package com.thapar.gpf.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Cached, fully aggregated statistics of a subject. Contains no student identity. */
@Entity
@Table(name = "subject_statistics")
@Getter
@Setter
@NoArgsConstructor
public class SubjectStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "subject_id", unique = true)
    private Subject subject;

    private int totalResponses;
    private double averageMarks;
    private double medianMarks;
    private double modeMarks;
    private double highestMarks;
    private double lowestMarks;
    private double standardDeviation;
    private double passPercentage;
    private double failurePercentage;

    private int gradeA;
    private int gradeAMinus;
    private int gradeB;
    private int gradeBMinus;
    private int gradeC;
    private int gradeD;
    private int gradeE;

    private LocalDateTime lastUpdated = LocalDateTime.now();
}
