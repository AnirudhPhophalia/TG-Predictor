package com.thapar.gpf.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private Integer semester;
    private String branch;
    private Double credits;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();
}
