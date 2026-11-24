package com.thapar.gpf.database.repository;

import com.thapar.gpf.database.tables.PlatformSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformSettingsRepository extends JpaRepository<PlatformSettings, Long> { }
