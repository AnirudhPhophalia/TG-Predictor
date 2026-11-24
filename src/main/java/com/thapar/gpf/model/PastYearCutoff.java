package com.thapar.gpf.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "past_year_cutoffs", uniqueConstraints = @UniqueConstraint(columnNames = {"subject_id", "academicYear"}))
@Getter
@Setter
public class PastYearCutoff {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "subject_id")
    private Subject subject;
    private int academicYear;
    private double cutoffA;
    private double cutoffAMinus;
    private double cutoffB;
    private double cutoffBMinus;
    private double cutoffC;
    private double cutoffD;
    private LocalDateTime updatedAt = LocalDateTime.now();
}
