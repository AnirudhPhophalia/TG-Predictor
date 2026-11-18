package com.thapar.gpf.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** The user link only exists to enforce one response per student; admins only see aggregates. */
@Entity
@Table(name = "poll_responses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class PollResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "poll_id")
    private Poll poll;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private double marks;

    private LocalDateTime submittedAt = LocalDateTime.now();
}
