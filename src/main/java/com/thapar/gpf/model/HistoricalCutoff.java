package com.thapar.gpf.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Absolute grade boundaries used while a subject has fewer than 100 submissions. */
@Entity
@Table(name = "historical_cutoffs")
@Getter
@Setter
@NoArgsConstructor
public class HistoricalCutoff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "subject_id", unique = true)
    private Subject subject;

    private double cutoffA = 85;
    private double cutoffAMinus = 75;
    private double cutoffB = 65;
    private double cutoffBMinus = 55;
    private double cutoffC = 45;
    private double cutoffD = 35;

    private LocalDateTime createdAt = LocalDateTime.now();
}
