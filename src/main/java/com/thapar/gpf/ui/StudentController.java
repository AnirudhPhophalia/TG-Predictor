package com.thapar.gpf.ui;

import com.thapar.gpf.backend.dto.MarksSubmissionRequest;
import com.thapar.gpf.backend.dto.SubmissionRow;
import com.thapar.gpf.backend.exception.BadRequestException;
import com.thapar.gpf.database.tables.Mark;
import com.thapar.gpf.database.tables.Subject;
import com.thapar.gpf.database.tables.User;
import com.thapar.gpf.backend.service.UnlockRequestService;
import com.thapar.gpf.backend.service.MarksService;
import com.thapar.gpf.backend.service.PollService;
import com.thapar.gpf.backend.service.SubjectService;
import com.thapar.gpf.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Comparator;
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
    private final UnlockRequestService unlockRequestService;
    private final PollService pollService;

    public StudentController(UserService userService, SubjectService subjectService,
                             MarksService marksService, UnlockRequestService unlockRequestService,
                             PollService pollService) {
        this.userService = userService;
        this.subjectService = subjectService;
        this.marksService = marksService;
        this.unlockRequestService = unlockRequestService;
        this.pollService = pollService;
    }

    @GetMapping
    public String dashboard(Principal principal, Model model, @RequestParam(required = false) Integer semester,
                            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "0") int open) {
        User student = userService.getByEmail(principal.getName());
        int shown = semester == null ? 0 : semester;
        Integer current = UserService.currentSemester(student);

        List<Mark> myMarks = marksService.findMyMarks(student.getId());
        Page<SubmissionRow> rows = Pagination.of(myMarks.stream()
                        .filter(m -> inSemester(m.getSubject(), shown))
                        .toList(), page, 6)
                .map(m -> new SubmissionRow(m, subjectService.statsFor(m.getSubject())));

        Set<Long> submittedSubjectIds = myMarks.stream()
                .map(m -> m.getSubject().getId())
                .collect(Collectors.toSet());
        Page<Subject> openSubjects = Pagination.of(subjectService.findActive().stream()
                        .filter(s -> !submittedSubjectIds.contains(s.getId()))
                        .filter(s -> inSemester(s, shown))
                        .sorted(currentSemesterFirst(current))
                        .toList(), open, 8);

        model.addAttribute("student", student);
        model.addAttribute("rows", rows);
        model.addAttribute("openSubjects", openSubjects);
        model.addAttribute("totalSubmissions", myMarks.size());
        model.addAttribute("activeSubjects", subjectService.findActive().size());
        model.addAttribute("averageGrade", myMarks.stream().mapToInt(m -> switch (m.getPredictedGrade()) {
            case A -> 10; case A_MINUS -> 9; case B -> 8; case B_MINUS -> 7; case C -> 6; case D -> 5; case E -> 0;
        }).average().orElse(0));
        model.addAttribute("selectedSemester", shown);
        model.addAttribute("currentSemester", current);
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
            model.addAttribute("errors", FormErrors.of(result));
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
        model.addAttribute("hasPendingRequest", unlockRequestService.hasPending(mark.getId()));
        return "student/mark-details";
    }

    @PostMapping("/marks/{markId}/unlock-request")
    public String requestUnlock(@PathVariable Long markId, @RequestParam String reason,
                                Principal principal, RedirectAttributes redirect) {
        User student = userService.getByEmail(principal.getName());
        unlockRequestService.create(student, markId, reason);
        redirect.addFlashAttribute("success", "Unlock request sent to the admin.");
        return "redirect:/student/edit-requests";
    }

    @GetMapping("/edit-requests")
    public String editRequests(Principal principal, @RequestParam(defaultValue = "0") int page, Model model) {
        User student = userService.getByEmail(principal.getName());
        model.addAttribute("requests", Pagination.fetch(page, 10, Pagination.NEWEST_FIRST,
                pageable -> unlockRequestService.findMine(student.getId(), pageable)));
        return "student/edit-requests";
    }

    @GetMapping("/polls")
    public String polls(Principal principal, @RequestParam(defaultValue = "0") int page, Model model) {
        User student = userService.getByEmail(principal.getName());
        var polls = Pagination.fetch(page, 5, Pagination.NEWEST_FIRST, pollService::findPolls);
        var predictions = new java.util.HashMap<Long, com.thapar.gpf.backend.service.GradePredictionService.Prediction>();
        polls.forEach(p -> pollService.myPrediction(p.getId(), student.getId()).ifPresent(result -> predictions.put(p.getId(), result)));
        model.addAttribute("predictions", predictions);
        Set<Long> answered = polls.stream()
                .filter(p -> pollService.hasResponded(p.getId(), student.getId()))
                .map(p -> p.getId())
                .collect(Collectors.toSet());
        model.addAttribute("polls", polls);
        model.addAttribute("answered", answered);
        return "student/polls";
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        model.addAttribute("student", userService.getByEmail(principal.getName()));
        return "student/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Principal principal, @RequestParam String rollNumber, @RequestParam String branch,
                                @RequestParam Integer semester, RedirectAttributes redirect) {
        userService.updateProfile(userService.getByEmail(principal.getName()), rollNumber, branch, semester);
        redirect.addFlashAttribute("success", "Profile updated.");
        return "redirect:/student/profile";
    }

    @GetMapping("/subjects")
    public String subjects(Principal principal, @RequestParam(defaultValue = "") String search,
                            @RequestParam(required = false) Integer semester,
                            @RequestParam(defaultValue = "0") int page, Model model) {
        int shown = semester == null ? 0 : semester;
        Integer current = UserService.currentSemester(userService.getByEmail(principal.getName()));
        model.addAttribute("search", search);
        model.addAttribute("selectedSemester", shown);
        model.addAttribute("currentSemester", current);
        model.addAttribute("subjects", Pagination.of(subjectService.findActive().stream()
                .filter(s -> inSemester(s, shown))
                .filter(s -> (s.getCode() + " " + s.getName() + " " + s.getBranch()).toLowerCase(java.util.Locale.ROOT)
                        .contains(search.toLowerCase(java.util.Locale.ROOT)))
                .sorted(currentSemesterFirst(current)).toList(), page, 12));
        return "student/subjects";
    }

    @GetMapping("/help")
    public String help() { return "student/help"; }

    @PostMapping("/polls/{pollId}/respond")
    public String respond(@PathVariable Long pollId, @RequestParam Double marks,
                          @RequestParam(defaultValue = "0") int page,
                          Principal principal, RedirectAttributes redirect) {
        User student = userService.getByEmail(principal.getName());
        try {
            pollService.respond(student, pollId, marks);
            redirect.addFlashAttribute("success", "Thanks! Your anonymous answer was recorded.");
        } catch (BadRequestException e) {
            redirect.addFlashAttribute("failure", e.getMessage());
        }
        return "redirect:/student/polls?page=" + page;
    }

    /**
     * Current semester first, then earlier ones newest first, then later ones:
     * for a semester 5 student that is 5, 4, 3, 2, 1, 6, 7, 8. Unknown semester: 1 to 8.
     */
    static Comparator<Subject> currentSemesterFirst(Integer current) {
        return Comparator.comparingInt(s -> {
            Integer semester = s.getSemester();
            if (semester == null) return 99;
            if (current == null || semester > current) return semester;
            return current - semester;
        });
    }

    private static boolean inSemester(Subject subject, int semester) {
        return semester == 0 || Integer.valueOf(semester).equals(subject.getSemester());
    }

    private static MarksSubmissionRequest emptyRequest() {
        return new MarksSubmissionRequest(null, null, null, null, null, null, null);
    }

    private static MarksSubmissionRequest toRequest(Mark m) {
        return new MarksSubmissionRequest(m.getTotalMarks(), m.getEstMarks(), m.getMstMarks(),
                m.getQuizMarks(), m.getAssignmentMarks(), m.getLabMarks(), m.getPracticalMarks());
    }
}
