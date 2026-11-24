package com.thapar.gpf.calculation;

import com.thapar.gpf.database.EditRequest;
import com.thapar.gpf.database.Mark;
import com.thapar.gpf.database.Poll;
import com.thapar.gpf.database.Subject;
import com.thapar.gpf.database.SubjectStats;
import com.thapar.gpf.database.User;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The final numbers admins see. Everything here is a count, an average or a timestamp:
 * nothing that could be traced back to one student's marks.
 */
public final class Analytics {

    private Analytics() {
    }

    public record Report(long totalUsers, long totalStudents, long totalAdmins, long totalSubjects, long activeSubjects,
                         long totalSubmissions, long pendingRequests, long openPolls, double overallPassPercentage,
                         int[] gradeTotals, List<SubjectStats> subjectStats, List<SubjectStats> topSubjects,
                         List<LocalDateTime> recentSubmissionTimes) {
    }

    /** Per-subject statistics with small subjects (fewer than 5 responses) hidden. */
    public static List<SubjectStats> subjectStatsForAdmin() {
        return Subject.all().stream().map(SubjectStats::of).map(SubjectStats::forAdmin).toList();
    }

    public static Report build() {
        List<SubjectStats> all = subjectStatsForAdmin();
        List<SubjectStats> visible = all.stream().filter(s -> !s.hidden()).toList();

        int[] gradeTotals = new int[Grade.values().length];
        for (SubjectStats s : visible) {
            for (int i = 0; i < gradeTotals.length; i++) gradeTotals[i] += s.gradeCounts()[i];
        }
        int responses = visible.stream().mapToInt(SubjectStats::totalResponses).sum();
        double overallPass = responses == 0 ? 0
                : visible.stream().mapToDouble(s -> s.passPercentage() * s.totalResponses()).sum() / responses;
        List<SubjectStats> top = visible.stream()
                .sorted(Comparator.comparingDouble(SubjectStats::average).reversed()).limit(5).toList();

        return new Report(User.count(), User.countByRole(User.Role.STUDENT), User.countByRole(User.Role.ADMIN),
                Subject.count(), Subject.countActive(), Mark.count(), EditRequest.countPending(), Poll.countOpen(),
                overallPass, gradeTotals, all, top, Mark.recentSubmissionTimes(10));
    }

    /** CSV of subject-level statistics: one row per subject, no individual marks. Small subjects are left blank. */
    public static String csv() {
        StringBuilder csv = new StringBuilder(
                "Code,Name,Responses,Average,Median,Mode,Highest,Lowest,StdDev,Pass %,Fail %,A,A-,B,B-,C,D,E\n");
        for (SubjectStats s : subjectStatsForAdmin()) {
            String name = s.name().replace("\"", "\"\"");
            if (s.hidden()) {
                csv.append(String.format(Locale.ROOT, "%s,\"%s\",%d,,,,,,,,,,,,,,\n", s.code(), name, s.totalResponses()));
                continue;
            }
            int[] g = s.gradeCounts();
            csv.append(String.format(Locale.ROOT,
                    "%s,\"%s\",%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.1f,%.1f,%d,%d,%d,%d,%d,%d,%d\n",
                    s.code(), name, s.totalResponses(), s.average(), s.median(), s.mode(), s.highest(), s.lowest(),
                    s.standardDeviation(), s.passPercentage(), s.failurePercentage(),
                    g[0], g[1], g[2], g[3], g[4], g[5], g[6]));
        }
        return csv.toString();
    }
}
