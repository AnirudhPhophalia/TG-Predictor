package com.thapar.gpf.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A student's request to unlock a submitted mark so it can be corrected. */
@Entity
@Table(name = "edit_requests")
@Getter
@Setter
@NoArgsConstructor
public class EditRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mark_id")
    private Mark mark;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EditStatus status = EditStatus.PENDING;

    private String rejectionReason;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;
}
