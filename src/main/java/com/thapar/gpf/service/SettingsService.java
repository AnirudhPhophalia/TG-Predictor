package com.thapar.gpf.service;

import com.thapar.gpf.model.PlatformSettings;
import com.thapar.gpf.repository.PlatformSettingsRepository;
import com.thapar.gpf.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {
    private final PlatformSettingsRepository settings;
    public SettingsService(PlatformSettingsRepository settings) { this.settings = settings; }
    public PlatformSettings get() { return settings.findById(1L).orElseGet(PlatformSettings::new); }

    @Transactional
    public void save(boolean maintenanceMode, boolean allowStudentEdits, String systemEmail) {
        if (!UserService.isThaparEmail(systemEmail)) throw new BadRequestException("Use an @thapar.edu contact email.");
        var value = get();
        value.setMaintenanceMode(maintenanceMode);
        value.setAllowStudentEdits(allowStudentEdits);
        value.setSystemEmail(systemEmail.toLowerCase(java.util.Locale.ROOT));
        settings.save(value);
    }

    public void requireSubmissionsOpen() {
        if (get().isMaintenanceMode()) throw new BadRequestException("Submissions are paused for maintenance. Please try again later.");
    }

    public void requireUnlockRequestsOpen() {
        requireSubmissionsOpen();
        if (!get().isAllowStudentEdits()) throw new BadRequestException("Unlock requests are temporarily paused.");
    }
}
