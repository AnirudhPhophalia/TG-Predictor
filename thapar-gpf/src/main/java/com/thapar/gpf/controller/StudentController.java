package com.thapar.gpf.controller;

import com.thapar.gpf.dto.MarksSubmissionRequest;
import com.thapar.gpf.dto.SubjectStatDTO;
import com.thapar.gpf.dto.SubmissionRow;
import com.thapar.gpf.exception.BadRequestException;
import com.thapar.gpf.model.Mark;
import com.thapar.gpf.model.Subject;
import com.thapar.gpf.model.User;
import com.thapar.gpf.service.EditRequestService;
import com.thapar.gpf.service.MarksService;
import com.thapar.gpf.service.PollService;
import com.thapar.gpf.service.SubjectService;
import com.thapar.gpf.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final UserService userService;
    private final SubjectService subjectService;
    private final MarksService marksService;
    private final EditRequestService editRequestService;
    private final PollService pollService;

    public StudentController(UserService userService, SubjectService subjectService,
                             MarksService marksService, EditRequestService editRequestService,
                             PollService pollService) {
        this.userService = userService;
        this.subjectService = subjectService;
        this.marksService = marksService;
        this.editRequestService = editRequestService;
        this.pollService = pollService;
    }

    @GetMapping
    public String dashboard(Principal principal, Model model) {
        User student = userService.getByEmail(principal.getName());

        List<Mark> myMarks = marksService.findMyMarks(student.getId());
        List<SubmissionRow> rows = myMarks.stream()
                .map(m -> new SubmissionRow(m, subjectService.statsFor(m.getSubject())))
                .toList();

        Set<Long> submittedSubjectIds = myMarks.stream()
                .map(m -> m.getSubject().getId())
                .collect(Collectors.toSet());
        List<SubjectStatDTO> openSubjects = subjectService.findActive().stream()
                .filter(s -> !submittedSubjectIds.contains(s.getId()))
                .map(subjectService::statsFor)
                .toList();

        model.addAttribute("student", student);
        model.addAttribute("rows", rows);
        model.addAttribute("openSubjects", openSubjects);
        return "student/dashboard";
    }

    @GetMapping("/submit/{subjectId}")
    public String submitForm(@PathVariable Long subjectId, Principal principal,
                             Model model, RedirectAttributes redirect) {
        User student = userService.getByEmail(principal.getName());
        Subject subject = subjectService.get(subjectId);

        Optional<Mark> existing = marksService.findMyMarkForSubject(student.getId(), subjectId);
        if (existing.isPresent() && existing.get().isLocked()) {
            redirect.addFlashAttribute("failure", "Your marks are locked. Send an unlock request to change them.");
            return "redirect:/student/marks/" + existing.get().getId();
        }

        model.addAttribute("subject", subject);
        model.addAttribute("form", existing.map(StudentController::toRequest).orElse(emptyRequest()));
        model.addAttribute("updating", existing.isPresent());
        return "student/submit";
    }

    @PostMapping("/submit/{subjectId}")
    public String submit(@PathVariable Long subjectId,
                         @Valid @ModelAttribute("form") MarksSubmissionRequest form,
                         BindingResult result, Principal principal,
                         Model model, RedirectAttributes redirect) {
        User student = userService.getByEmail(principal.getName());

        if (result.hasErrors()) {
            model.addAttribute("subject", subjectService.get(subjectId));
            model.addAttribute("errors", Messages.of(result));
            model.addAttribute("updating", false);
            return "student/submit";
        }

        // throws MarkLockedException (HTTP 400) when the marks are already locked
        Mark mark = marksService.submit(student, subjectId, form);
        redirect.addFlashAttribute("success", "Marks saved and locked. Your grade has been predicted.");
        return "redirect:/student/marks/" + mark.getId();
    }

    @GetMapping("/marks/{markId}")
    public String markDetails(@PathVariable Long markId, Principal principal, Model model) {
        User student = userService.getByEmail(principal.getName());
        Mark mark = marksService.getMyMark(markId, student.getId());

        model.addAttribute("mark", mark);
        model.addAttribute("stats", subjectService.statsFor(mark.getSubject()));
        model.addAttribute("buckets", marksService.scoreBuckets(mark.getSubject().getId()));
        model.addAttribute("hasPendingRequest", editRequestService.hasPending(mark.getId()));
        return "student/mark-details";
    }

    @PostMapping("/marks/{markId}/unlock-request")
    public String requestUnlock(@PathVariable Long markId, @RequestParam String reason,
                                Principal principal, RedirectAttributes redirect) {
        User student = userService.getByEmail(principal.getName());
        editRequestService.create(student, markId, reason);
        redirect.addFlashAttribute("success", "Unlock request sent to the admin.");
        return "redirect:/student/edit-requests";
    }

    @GetMapping("/edit-requests")
    public String editRequests(Principal principal, Model model) {
        User student = userService.getByEmail(principal.getName());
        model.addAttribute("requests", editRequestService.findMine(student.getId()));
        return "student/edit-requests";
    }

    @GetMapping("/polls")
    public String polls(Principal principal, Model model) {
        User student = userService.getByEmail(principal.getName());
        var polls = pollService.findOpenPolls();
        Set<Long> answered = polls.stream()
                .filter(p -> pollService.hasResponded(p.getId(), student.getId()))
                .map(p -> p.getId())
                .collect(Collectors.toSet());
        model.addAttribute("polls", polls);
        model.addAttribute("answered", answered);
        return "student/polls";
    }

    @PostMapping("/polls/{pollId}/respond")
    public String respond(@PathVariable Long pollId, @RequestParam Double marks,
                          Principal principal, RedirectAttributes redirect) {
        User student = userService.getByEmail(principal.getName());
        try {
            pollService.respond(student, pollId, marks);
            redirect.addFlashAttribute("success", "Thanks! Your anonymous answer was recorded.");
        } catch (BadRequestException e) {
            redirect.addFlashAttribute("failure", e.getMessage());
        }
        return "redirect:/student/polls";
    }

    private static MarksSubmissionRequest emptyRequest() {
        return new MarksSubmissionRequest(null, null, null, null, null, null, null);
    }

    private static MarksSubmissionRequest toRequest(Mark m) {
        return new MarksSubmissionRequest(m.getTotalMarks(), m.getEstMarks(), m.getMstMarks(),
                m.getQuizMarks(), m.getAssignmentMarks(), m.getLabMarks(), m.getPracticalMarks());
    }
}
