package com.thapar.gpf.backend.service;

import com.thapar.gpf.backend.dto.CutoffRequest;
import com.thapar.gpf.database.tables.PastYearCutoff;
import com.thapar.gpf.database.repository.PastYearCutoffRepository;
import com.thapar.gpf.backend.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.Year;

@Service
public class PastYearCutoffService {
    private final PastYearCutoffRepository records;
    private final SubjectService subjects;

    public PastYearCutoffService(PastYearCutoffRepository records, SubjectService subjects) {
        this.records = records;
        this.subjects = subjects;
    }

    public org.springframework.data.domain.Page<PastYearCutoff> findPage(org.springframework.data.domain.Pageable page) {
        return records.findAll(page);
    }

    public static void validate(CutoffRequest r) {
        Double[] values = {r.cutoffA(), r.cutoffAMinus(), r.cutoffB(), r.cutoffBMinus(), r.cutoffC(), r.cutoffD()};
        double previous = 101;
        for (Double value : values) {
            if (value == null || !Double.isFinite(value) || value < 35 || value >= previous) {
                throw new BadRequestException("Cutoffs must decrease strictly from A to D, between 35 and 100.");
            }
            previous = value;
        }
    }

    @Transactional
    public void save(Long subjectId, int year, CutoffRequest r, boolean useForPredictions) {
        validate(r);
        if (year < 2000 || year > Year.now().getValue()) throw new BadRequestException("Enter a valid past or current year.");
        var record = records.findBySubjectIdAndAcademicYear(subjectId, year).orElseGet(PastYearCutoff::new);
        record.setSubject(subjects.get(subjectId));
        record.setAcademicYear(year);
        record.setCutoffA(r.cutoffA());
        record.setCutoffAMinus(r.cutoffAMinus());
        record.setCutoffB(r.cutoffB());
        record.setCutoffBMinus(r.cutoffBMinus());
        record.setCutoffC(r.cutoffC());
        record.setCutoffD(r.cutoffD());
        record.setUpdatedAt(LocalDateTime.now());
        records.save(record);
        if (useForPredictions) subjects.updateCutoff(subjectId, r);
    }
}
