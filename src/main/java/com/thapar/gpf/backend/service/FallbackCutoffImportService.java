package com.thapar.gpf.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thapar.gpf.backend.dto.CutoffRequest;
import com.thapar.gpf.backend.dto.HistoricalCutoffImport;
import com.thapar.gpf.backend.exception.BadRequestException;
import com.thapar.gpf.database.repository.SubjectRepository;
import com.thapar.gpf.database.tables.Subject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FallbackCutoffImportService {
    private final ObjectMapper objectMapper;
    private final SubjectRepository subjects;
    private final SubjectService subjectService;

    public FallbackCutoffImportService(ObjectMapper objectMapper, SubjectRepository subjects,
                                       SubjectService subjectService) {
        this.objectMapper = objectMapper;
        this.subjects = subjects;
        this.subjectService = subjectService;
    }

    @Transactional
    public int importCutoffs(InputStream input) {
        HistoricalCutoffImport file;
        try {
            file = objectMapper.readValue(input, HistoricalCutoffImport.class);
        } catch (IOException e) {
            throw new BadRequestException("Upload a valid cutoff-only JSON file.");
        }
        if (file == null || file.historicalCutoffs() == null || file.historicalCutoffs().isEmpty()) {
            throw new BadRequestException("The file contains no historical cutoffs.");
        }

        Set<String> importedCodes = new HashSet<>();
        List<ResolvedCutoff> resolved = file.historicalCutoffs().stream()
                .map(entry -> {
                    if (entry == null || entry.subjectCode() == null || entry.subjectCode().isBlank()) {
                        throw new BadRequestException("Every cutoff record must include a subject code.");
                    }
                    String code = entry.subjectCode().trim().toUpperCase(Locale.ROOT);
                    if (!importedCodes.add(code)) {
                        throw new BadRequestException("The file contains duplicate entries for " + code + ".");
                    }
                    Long subjectId = subjects.findByCodeIgnoreCase(code)
                            .map(Subject::getId)
                            .orElseThrow(() -> new BadRequestException("Subject " + code + " was not found."));
                    return new ResolvedCutoff(subjectId, new CutoffRequest(entry.cutoffA(), entry.cutoffAMinus(),
                            entry.cutoffB(), entry.cutoffBMinus(), entry.cutoffC(), entry.cutoffD()));
                })
                .toList();

        for (ResolvedCutoff cutoff : resolved) {
            subjectService.updateCutoff(cutoff.subjectId(), cutoff.request());
        }
        return resolved.size();
    }

    private record ResolvedCutoff(Long subjectId, CutoffRequest request) {
    }
}
