package com.thapar.gpf.login;

import com.thapar.gpf.ui.FormErrors;
import com.thapar.gpf.backend.dto.RegisterRequest;
import com.thapar.gpf.backend.exception.BadRequestException;
import com.thapar.gpf.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
public class LoginController {

    private final UserService userService;
    @Value("${gpf.demo.enabled:false}") private boolean demoEnabled;
    @Value("${gpf.google.enabled:false}") private boolean googleEnabled;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin ? "redirect:/admin" : "redirect:/student";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("demoEnabled", demoEnabled);
        model.addAttribute("googleEnabled", googleEnabled);
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        requireDemo();
        model.addAttribute("form", new RegisterRequest("", "", "", "", "", null));
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterRequest form,
                           BindingResult result, Model model, RedirectAttributes redirect) {
        requireDemo();
        if (result.hasErrors()) {
            model.addAttribute("errors", FormErrors.of(result));
            return "register";
        }
        try {
            userService.register(form);
        } catch (BadRequestException e) {
            model.addAttribute("errors", java.util.List.of(e.getMessage()));
            return "register";
        }
        redirect.addFlashAttribute("success", "Account created. You can log in now.");
        return "redirect:/login";
    }

    private void requireDemo() {
        if (!demoEnabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
