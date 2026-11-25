package com.thapar.gpf.backend.service;

import com.thapar.gpf.backend.dto.AnalyticsReport;
import com.thapar.gpf.backend.dto.SubjectStatDTO;
import com.thapar.gpf.database.tables.UnlockStatus;
import com.thapar.gpf.database.tables.Role;
import com.thapar.gpf.database.repository.UnlockRequestRepository;
import com.thapar.gpf.database.repository.MarkRepository;
import com.thapar.gpf.database.repository.PollRepository;
import com.thapar.gpf.database.repository.SubjectRepository;
import com.thapar.gpf.database.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final MarkRepository markRepository;
    private final UnlockRequestRepository unlockRequestRepository;
    private final PollRepository pollRepository;
    private final SubjectService subjectService;

    public AnalyticsService(UserRepository userRepository,
                            SubjectRepository subjectRepository,
                            MarkRepository markRepository,
                            UnlockRequestRepository unlockRequestRepository,
                            PollRepository pollRepository,
                            SubjectService subjectService) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.markRepository = markRepository;
        this.unlockRequestRepository = unlockRequestRepository;
        this.pollRepository = pollRepository;
        this.subjectService = subjectService;
    }

    public AnalyticsReport buildReport() {
        List<SubjectStatDTO> subjectStats = subjectService.findAll().stream()
                .map(subjectService::statsFor)
                .map(s -> s.forAdmin(GradePredictionService.MIN_RESPONSES_FOR_ADMIN))
                .toList();
        List<SubjectStatDTO> visible = subjectStats.stream().filter(s -> !s.hidden()).toList();

        int[] gradeTotals = new int[7];
        for (SubjectStatDTO s : visible) {
            gradeTotals[0] += s.gradeA();
            gradeTotals[1] += s.gradeAMinus();
            gradeTotals[2] += s.gradeB();
            gradeTotals[3] += s.gradeBMinus();
            gradeTotals[4] += s.gradeC();
            gradeTotals[5] += s.gradeD();
            gradeTotals[6] += s.gradeE();
        }

        int totalResponses = visible.stream().mapToInt(SubjectStatDTO::totalResponses).sum();
        double overallPass = totalResponses == 0 ? 0
                : visible.stream().mapToDouble(s -> s.passPercentage() * s.totalResponses()).sum() / totalResponses;

        List<SubjectStatDTO> top = visible.stream()
                .sorted(Comparator.comparingDouble(SubjectStatDTO::average).reversed())
                .limit(5)
                .toList();

        List<LocalDateTime> recent = markRepository.findRecentSubmissionTimes(PageRequest.of(0, 10));

        return new AnalyticsReport(
                userRepository.count(),
                userRepository.countByRole(Role.STUDENT),
                userRepository.countByRole(Role.ADMIN),
                subjectRepository.count(),
                subjectRepository.countByActiveTrue(),
                markRepository.count(),
                unlockRequestRepository.countByStatus(UnlockStatus.PENDING),
                pollRepository.countByActiveTrueAndEndTimeAfter(LocalDateTime.now()),
                overallPass,
                gradeTotals,
                subjectStats,
                top,
                recent);
    }

    /** CSV of subject-level statistics: one row per subject, no individual marks. Small subjects are left blank. */
    public String buildStatisticsCsv() {
        StringBuilder csv = new StringBuilder(
                "Code,Name,Responses,Average,Median,Mode,Highest,Lowest,StdDev,Pass %,Fail %,A,A-,B,B-,C,D,E\n");
        for (SubjectStatDTO s : subjectService.findAll().stream()
                .map(subjectService::statsFor)
                .map(x -> x.forAdmin(GradePredictionService.MIN_RESPONSES_FOR_ADMIN))
                .toList()) {
            if (s.hidden()) {
                csv.append(String.format(Locale.ROOT, "%s,\"%s\",%d,,,,,,,,,,,,,,\n",
                        s.code(), s.name().replace("\"", "\"\""), s.totalResponses()));
                continue;
            }
            csv.append(String.format(Locale.ROOT, "%s,\"%s\",%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.1f,%.1f,%d,%d,%d,%d,%d,%d,%d\n",
                    s.code(), s.name().replace("\"", "\"\""), s.totalResponses(), s.average(), s.median(), s.mode(),
                    s.highest(), s.lowest(), s.standardDeviation(), s.passPercentage(), s.failurePercentage(),
                    s.gradeA(), s.gradeAMinus(), s.gradeB(), s.gradeBMinus(), s.gradeC(), s.gradeD(), s.gradeE()));
        }
        return csv.toString();
    }
}
