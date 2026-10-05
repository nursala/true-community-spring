package com.truecommunity.controllers;

import com.truecommunity.dto.RegisterForm;
import com.truecommunity.exception.EmailAlreadyExistsException;
import com.truecommunity.exception.PasswordMismatchException;
import com.truecommunity.exception.UsernameAlreadyExistsException;
import com.truecommunity.services.UserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MVC controller for public login and registration pages.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Shows the login page to anonymous users and redirects authenticated users
     * to their existing landing page.
     *
     * @param authentication current authentication, possibly anonymous
     * @return login template or role-appropriate redirect
     */
    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return isEditor(authentication) ? "redirect:/editor" : "redirect:/dashboard";
        }
        return "login";
    }

    /**
     * Renders the registration form.
     *
     * @param model MVC model
     * @return registration template
     */
    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "register";
    }

    /**
     * Registers a new user and redirects to login on success.
     *
     * @param registerForm submitted registration form
     * @param bindingResult validation result
     * @param redirectAttributes flash attributes for redirect feedback
     * @return registration template on failure or login redirect on success
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm registerForm,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.register(registerForm);
        } catch (PasswordMismatchException ex) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "הסיסמאות אינן תואמות.");
            return "register";
        } catch (EmailAlreadyExistsException ex) {
            bindingResult.rejectValue("email", "email.exists", "האימייל כבר רשום במערכת.");
            return "register";
        } catch (UsernameAlreadyExistsException ex) {
            bindingResult.rejectValue("username", "username.exists", "שם המשתמש כבר תפוס.");
            return "register";
        }

        redirectAttributes.addFlashAttribute("registerSuccess", "ההרשמה הושלמה בהצלחה. אפשר להתחבר.");
        return "redirect:/login";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean isEditor(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_EDITOR".equals(authority.getAuthority()));
    }
}
