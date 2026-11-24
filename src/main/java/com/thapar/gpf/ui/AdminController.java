package com.thapar.gpf.ui;

import com.thapar.gpf.backend.dto.CutoffRequest;
import com.thapar.gpf.backend.dto.SubjectRequest;
import com.thapar.gpf.backend.exception.ResourceNotFoundException;
import com.thapar.gpf.database.tables.Role;
import com.thapar.gpf.database.tables.Subject;
import com.thapar.gpf.backend.service.AnalyticsService;
import com.thapar.gpf.backend.service.UnlockRequestService;
import com.thapar.gpf.backend.service.PollService;
import com.thapar.gpf.backend.service.SubjectService;
import com.thapar.gpf.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * PRIVACY: this controller deliberately has no access to MarksService, MarkRepository
 * or the Mark entity. It can only see users' metadata, unlock requests (without marks)
 * and aggregated statistics. A unit test enforces this.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final SubjectService subjectService;
    private final UserService userService;
    private final UnlockRequestService unlockRequestService;
    private final PollService pollService;
    private final AnalyticsService analyticsService;
    private final com.thapar.gpf.backend.service.PastYearCutoffService history;
    @org.springframework.beans.factory.annotation.Autowired
    private com.thapar.gpf.backend.service.SettingsService settings;
    @org.springframework.beans.factory.annotation.Autowired
    private com.thapar.gpf.backend.service.StatisticsExportService exports;
    @org.springframework.beans.factory.annotation.Value("${gpf.admin.email:admin@thapar.edu}")
    private String adminEmail;

    public AdminController(SubjectService subjectService, UserService userService,
                           UnlockRequestService unlockRequestService, PollService pollService,
                           AnalyticsService analyticsService, com.thapar.gpf.backend.service.PastYearCutoffService history) {
        this.subjectService = subjectService;
        this.userService = userService;
        this.unlockRequestService = unlockRequestService;
        this.pollService = pollService;
        this.analyticsService = analyticsService;
        this.history = history;
    }

    // ---------- dashboard & analytics ----------

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("report", analyticsService.buildReport());
        return "admin/dashboard";
    }

    @GetMapping("/analytics")
    public String analytics(@RequestParam(defaultValue = "0") int page, Model model) {
        var report = analyticsService.buildReport();
        model.addAttribute("report", report);
        model.addAttribute("subjectStats", Pagination.of(report.subjectStats(), page, 15));
        return "admin/analytics";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "csv") String format) {
        var download = exports.export(format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"subject-statistics." + download.extension() + "\"")
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.bytes());
    }

    // ---------- subjects ----------

    @GetMapping("/subjects")
    public String subjects(@RequestParam(required = false) Long edit,
                           @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("subjects", Pagination.fetch(page, 15, Sort.by("code"), subjectService::findPage));
        Subject editing = edit == null ? null : subjectService.get(edit);
        model.addAttribute("editing", editing);
        model.addAttribute("form", editing == null
                ? new SubjectRequest("", "", null, "", null)
                : new SubjectRequest(editing.getCode(), editing.getName(), editing.getSemester(),
                editing.getBranch(), editing.getCredits()));
        return "admin/subjects";
    }

    @PostMapping("/subjects")
    public String addSubject(@Valid @ModelAttribute("form") SubjectRequest form,
                             BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("failure", FormErrors.first(result));
            return "redirect:/admin/subjects";
        }
        subjectService.create(form);
        redirect.addFlashAttribute("success", "Subject added.");
        return "redirect:/admin/subjects";
    }

    @PostMapping("/subjects/{id}")
    public String updateSubject(@PathVariable Long id,
                                @Valid @ModelAttribute("form") SubjectRequest form,
                                BindingResult result, @RequestParam(defaultValue = "0") int page,
                                RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("failure", FormErrors.first(result));
            return "redirect:/admin/subjects?edit=" + id + "&page=" + page;
        }
        subjectService.update(id, form);
        redirect.addFlashAttribute("success", "Subject updated.");
        return "redirect:/admin/subjects?page=" + page;
    }

    @PostMapping("/subjects/{id}/toggle")
    public String toggleSubject(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                                RedirectAttributes redirect) {
        subjectService.toggleActive(id);
        redirect.addFlashAttribute("success", "Subject status changed.");
        return "redirect:/admin/subjects?page=" + page;
    }

    // ---------- fallback cutoffs ----------

    @GetMapping("/cutoffs")
    public String cutoffs(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "0") int historyPage, Model model) {
        model.addAttribute("history", Pagination.fetch(historyPage, 10,
                Sort.by(Sort.Direction.DESC, "academicYear").and(Sort.by("subject.code")), history::findPage));
        model.addAttribute("currentYear", java.time.Year.now().getValue());
        model.addAttribute("cutoffs", Pagination.fetch(page, 5, Sort.by("code"),
                pageable -> subjectService.findPage(pageable).map(subjectService::getCutoff)));
        return "admin/cutoffs";
    }

    @PostMapping("/cutoffs/{subjectId}")
    public String saveCutoffs(@PathVariable Long subjectId,
                              @Valid @ModelAttribute CutoffRequest request,
                              BindingResult result, @RequestParam int academicYear,
                              @RequestParam(defaultValue = "false") boolean useForPredictions,
                              @RequestParam(defaultValue = "0") int page, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("failure", FormErrors.first(result));
            return "redirect:/admin/cutoffs?page=" + page;
        }
        history.save(subjectId, academicYear, request, useForPredictions);
        redirect.addFlashAttribute("success", "Cutoffs saved.");
        return "redirect:/admin/cutoffs?page=" + page;
    }

    // ---------- users ----------

    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("users", Pagination.fetch(page, 15, Pagination.NEWEST_FIRST, userService::findSummaries));
        model.addAttribute("adminEmail", adminEmail);
        return "admin/users";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("settings", settings.get());
        model.addAttribute("adminEmail", adminEmail);
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam(defaultValue = "false") boolean maintenanceMode,
            @RequestParam(defaultValue = "false") boolean allowStudentEdits,
            @RequestParam String systemEmail, RedirectAttributes redirect) {
        settings.save(maintenanceMode, allowStudentEdits, systemEmail);
        redirect.addFlashAttribute("success", "Settings saved.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam Role role,
                             Principal principal, RedirectAttributes redirect) {
        userService.changeRole(id, role, principal.getName());
        redirect.addFlashAttribute("success", "Role updated.");
        return "redirect:/admin/users";
    }

    // ---------- unlock requests ----------

    @GetMapping("/edit-requests")
    public String editRequests(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("requests", Pagination.fetch(page, 10, Pagination.NEWEST_FIRST, unlockRequestService::findForAdmin));
        return "admin/edit-requests";
    }

    @PostMapping("/edit-requests/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes redirect) {
        unlockRequestService.approve(id);
        redirect.addFlashAttribute("success", "Unlock approved. The student can now update their marks.");
        return "redirect:/admin/edit-requests?page=" + page;
    }

    @PostMapping("/edit-requests/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String reason,
                         @RequestParam(defaultValue = "0") int page, RedirectAttributes redirect) {
        unlockRequestService.reject(id, reason);
        redirect.addFlashAttribute("success", "Request rejected.");
        return "redirect:/admin/edit-requests?page=" + page;
    }

    // ---------- polls ----------

    @GetMapping("/polls")
    public String polls(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("polls", Pagination.fetch(page, 10, Pagination.NEWEST_FIRST, pollService::summarize));
        model.addAttribute("subjects", subjectService.findActive());
        return "admin/polls";
    }

    @PostMapping("/polls")
    public String createPoll(@RequestParam Long subjectId, @RequestParam String question,
                             @RequestParam int durationHours, RedirectAttributes redirect) {
        pollService.create(subjectId, question, durationHours);
        redirect.addFlashAttribute("success", "Poll created.");
        return "redirect:/admin/polls";
    }

    @PostMapping("/polls/{id}/close")
    public String closePoll(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                            RedirectAttributes redirect) {
        pollService.close(id);
        redirect.addFlashAttribute("success", "Poll closed.");
        return "redirect:/admin/polls?page=" + page;
    }

    // aggregate numbers only: count, average, median, highest, lowest, buckets
    @GetMapping("/polls/{id}/responses")
    public String pollResponses(@PathVariable Long id, Model model) {
        model.addAttribute("summary", pollService.summarize(id));
        model.addAttribute("poll", pollService.get(id));
        return "admin/poll-responses";
    }

    @PostMapping("/polls/{id}/cutoffs")
    public String pollCutoffs(@PathVariable Long id, @Valid @ModelAttribute CutoffRequest form,
                              BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) redirect.addFlashAttribute("failure", FormErrors.first(result));
        else {
            pollService.updateCutoffs(id, form);
            redirect.addFlashAttribute("success", "Poll cutoffs updated.");
        }
        return "redirect:/admin/polls/" + id + "/responses";
    }
}
