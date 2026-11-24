package com.thapar.gpf.calculation;

import com.thapar.gpf.AppException;
import com.thapar.gpf.database.Cutoffs;
import com.thapar.gpf.database.Database;
import com.thapar.gpf.database.Subject;

import java.time.Year;

/** Saving previous-year cutoffs and choosing which boundaries drive predictions. */
public final class CutoffService {

    private CutoffService() {
    }

    public static void validate(Cutoffs c) {
        double previous = 101;
        for (double value : c.values()) {
            if (!Double.isFinite(value) || value < 35 || value >= previous) {
                throw new AppException("Cutoffs must decrease strictly from A to D, between 35 and 100.");
            }
            previous = value;
        }
    }

    /** Stores a year record; when useForPredictions is set it also becomes the active fallback and grades refresh. */
    public static void saveYearRecord(long subjectId, int academicYear, Cutoffs c, boolean useForPredictions) {
        validate(c);
        if (academicYear < 2000 || academicYear > Year.now().getValue()) {
            throw new AppException("Enter a valid past or current year.");
        }
        Subject.get(subjectId);
        Database.inTransaction(() -> {
            Cutoffs.saveYear(subjectId, academicYear, c);
            if (useForPredictions) setActive(subjectId, c);
        });
    }

    /** Changing the active boundaries refreshes every existing prediction of the subject. */
    public static void setActive(long subjectId, Cutoffs c) {
        validate(c);
        Database.inTransaction(() -> {
            Cutoffs.setActive(subjectId, c);
            MarksService.recalculate(subjectId);
        });
    }
}
