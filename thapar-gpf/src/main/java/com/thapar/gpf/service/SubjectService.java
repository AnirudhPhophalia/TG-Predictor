package com.thapar.gpf.service;

import com.thapar.gpf.dto.CutoffRequest;
import com.thapar.gpf.dto.SubjectRequest;
import com.thapar.gpf.dto.SubjectStatDTO;
import com.thapar.gpf.exception.BadRequestException;
import com.thapar.gpf.exception.ResourceNotFoundException;
import com.thapar.gpf.model.HistoricalCutoff;
import com.thapar.gpf.model.Subject;
import com.thapar.gpf.repository.HistoricalCutoffRepository;
import com.thapar.gpf.repository.SubjectRepository;
import com.thapar.gpf.repository.SubjectStatisticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final HistoricalCutoffRepository cutoffRepository;
    private final SubjectStatisticsRepository statisticsRepository;

    public SubjectService(SubjectRepository subjectRepository,
                          HistoricalCutoffRepository cutoffRepository,
                          SubjectStatisticsRepository statisticsRepository) {
        this.subjectRepository = subjectRepository;
        this.cutoffRepository = cutoffRepository;
        this.statisticsRepository = statisticsRepository;
    }

    public List<Subject> findAll() {
        return subjectRepository.findAllByOrderByCodeAsc();
    }

    public List<Subject> findActive() {
        return subjectRepository.findByActiveTrueOrderByCodeAsc();
    }

    public Subject get(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
    }

    public SubjectStatDTO statsFor(Subject subject) {
        return SubjectStatDTO.from(subject, statisticsRepository.findBySubjectId(subject.getId()).orElse(null));
    }

    @Transactional
    public Subject create(SubjectRequest request) {
        String code = request.code().trim().toUpperCase();
        if (subjectRepository.existsByCode(code)) {
            throw new BadRequestException("A subject with code " + code + " already exists.");
        }
        Subject subject = new Subject();
        fill(subject, request, code);
        subjectRepository.save(subject);

        // every subject starts with the default historical cutoffs
        HistoricalCutoff cutoff = new HistoricalCutoff();
        cutoff.setSubject(subject);
        cutoffRepository.save(cutoff);
        return subject;
    }

    @Transactional
    public Subject update(Long id, SubjectRequest request) {
        Subject subject = get(id);
        String code = request.code().trim().toUpperCase();
        if (!code.equals(subject.getCode()) && subjectRepository.existsByCode(code)) {
            throw new BadRequestException("A subject with code " + code + " already exists.");
        }
        fill(subject, request, code);
        return subjectRepository.save(subject);
    }

    @Transactional
    public void toggleActive(Long id) {
        Subject subject = get(id);
        subject.setActive(!subject.isActive());
        subjectRepository.save(subject);
    }

    public HistoricalCutoff getCutoff(Subject subject) {
        return cutoffRepository.findBySubjectId(subject.getId()).orElseGet(() -> {
            HistoricalCutoff cutoff = new HistoricalCutoff();
            cutoff.setSubject(subject);
            return cutoffRepository.save(cutoff);
        });
    }

    @Transactional
    public void updateCutoff(Long subjectId, CutoffRequest r) {
        boolean descending = r.cutoffA() > r.cutoffAMinus() && r.cutoffAMinus() > r.cutoffB()
                && r.cutoffB() > r.cutoffBMinus() && r.cutoffBMinus() > r.cutoffC()
                && r.cutoffC() > r.cutoffD();
        boolean inRange = r.cutoffA() <= 100 && r.cutoffD() >= 0;
        if (!descending || !inRange) {
            throw new BadRequestException("Cutoffs must go strictly down from A to D, between 0 and 100.");
        }
        HistoricalCutoff cutoff = getCutoff(get(subjectId));
        cutoff.setCutoffA(r.cutoffA());
        cutoff.setCutoffAMinus(r.cutoffAMinus());
        cutoff.setCutoffB(r.cutoffB());
        cutoff.setCutoffBMinus(r.cutoffBMinus());
        cutoff.setCutoffC(r.cutoffC());
        cutoff.setCutoffD(r.cutoffD());
        cutoffRepository.save(cutoff);
    }

    private void fill(Subject subject, SubjectRequest request, String code) {
        subject.setCode(code);
        subject.setName(request.name().trim());
        subject.setSemester(request.semester());
        subject.setBranch(request.branch() == null ? null : request.branch().trim());
        subject.setCredits(request.credits());
    }
}
