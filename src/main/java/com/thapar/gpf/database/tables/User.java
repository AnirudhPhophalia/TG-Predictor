package com.thapar.gpf.database.tables;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    // BCrypt hash, never shown on any page
    @Column(nullable = false)
    private String password;

    private String rollNumber;
    private String branch;
    private Integer semester;
    private Integer admissionYear;
    private String googleSubject;
    private LocalDateTime lastLogin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.STUDENT;

    private LocalDateTime createdAt = LocalDateTime.now();
}
