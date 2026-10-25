package com.thapar.gpf.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "polls")
@Getter
@Setter
@NoArgsConstructor
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false, length = 300)
    private String question;

    private LocalDateTime endTime;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isOpen() {
        return active && endTime != null && endTime.isAfter(LocalDateTime.now());
    }
}
