package com.thapar.gpf.controller;

import com.thapar.gpf.dto.CutoffRequest;
import com.thapar.gpf.dto.SubjectRequest;
import com.thapar.gpf.exception.ResourceNotFoundException;
import com.thapar.gpf.model.Role;
import com.thapar.gpf.model.Subject;
import com.thapar.gpf.service.AnalyticsService;
import com.thapar.gpf.service.EditRequestService;
import com.thapar.gpf.service.PollService;
import com.thapar.gpf.service.SubjectService;
import com.thapar.gpf.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
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
    private final EditRequestService editRequestService;
    private final PollService pollService;
    private final AnalyticsService analyticsService;

    public AdminController(SubjectService subjectService, UserService userService,
                           EditRequestService editRequestService, PollService pollService,
                           AnalyticsService analyticsService) {
        this.subjectService = subjectService;
        this.userService = userService;
        this.editRequestService = editRequestService;
        this.pollService = pollService;
        this.analyticsService = analyticsService;
    }

    // ---------- dashboard & analytics ----------

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("report", analyticsService.buildReport());
        return "admin/dashboard";
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        model.addAttribute("report", analyticsService.buildReport());
        return "admin/analytics";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        byte[] csv = analyticsService.buildStatisticsCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"subject-statistics.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // ---------- subjects ----------

    @GetMapping("/subjects")
    public String subjects(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("subjects", subjectService.findAll());
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
            redirect.addFlashAttribute("failure", Messages.first(result));
            return "redirect:/admin/subjects";
        }
        subjectService.create(form);
        redirect.addFlashAttribute("success", "Subject added.");
        return "redirect:/admin/subjects";
    }

    @PostMapping("/subjects/{id}")
    public String updateSubject(@PathVariable Long id,
                                @Valid @ModelAttribute("form") SubjectRequest form,
                                BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("failure", Messages.first(result));
            return "redirect:/admin/subjects?edit=" + id;
        }
        subjectService.update(id, form);
        redirect.addFlashAttribute("success", "Subject updated.");
        return "redirect:/admin/subjects";
    }

    @PostMapping("/subjects/{id}/toggle")
    public String toggleSubject(@PathVariable Long id, RedirectAttributes redirect) {
        subjectService.toggleActive(id);
        redirect.addFlashAttribute("success", "Subject status changed.");
        return "redirect:/admin/subjects";
    }

    // ---------- fallback cutoffs ----------

    @GetMapping("/cutoffs")
    public String cutoffs(Model model) {
        model.addAttribute("cutoffs", subjectService.findAll().stream()
                .map(subjectService::getCutoff)
                .toList());
        return "admin/cutoffs";
    }

    @PostMapping("/cutoffs/{subjectId}")
    public String saveCutoffs(@PathVariable Long subjectId,
                              @Valid @ModelAttribute CutoffRequest request,
                              BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("failure", Messages.first(result));
            return "redirect:/admin/cutoffs";
        }
        subjectService.updateCutoff(subjectId, request);
        redirect.addFlashAttribute("success", "Cutoffs saved.");
        return "redirect:/admin/cutoffs";
    }

    // ---------- users ----------

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAllSummaries());
        return "admin/users";
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
    public String editRequests(Model model) {
        model.addAttribute("requests", editRequestService.findAllForAdmin());
        return "admin/edit-requests";
    }

    @PostMapping("/edit-requests/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        editRequestService.approve(id);
        redirect.addFlashAttribute("success", "Unlock approved. The student can now update their marks.");
        return "redirect:/admin/edit-requests";
    }

    @PostMapping("/edit-requests/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String reason,
                         RedirectAttributes redirect) {
        editRequestService.reject(id, reason);
        redirect.addFlashAttribute("success", "Request rejected.");
        return "redirect:/admin/edit-requests";
    }

    // ---------- polls ----------

    @GetMapping("/polls")
    public String polls(Model model) {
        model.addAttribute("polls", pollService.summarizeAll());
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
    public String closePoll(@PathVariable Long id, RedirectAttributes redirect) {
        pollService.close(id);
        redirect.addFlashAttribute("success", "Poll closed.");
        return "redirect:/admin/polls";
    }

    // aggregate numbers only: count, average, median, highest, lowest, buckets
    @GetMapping("/polls/{id}/responses")
    public String pollResponses(@PathVariable Long id, Model model) {
        model.addAttribute("summary", pollService.summarize(id));
        return "admin/poll-responses";
    }
}
