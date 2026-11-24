package com.thapar.gpf.database.tables;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class PlatformSettings {
    @Id private Long id = 1L;
    private boolean maintenanceMode;
    private boolean allowStudentEdits = true;
    private String systemEmail = "admin@thapar.edu";
}
