package com.truecommunity.controllers;

import com.truecommunity.dto.HistoryOutcome;
import com.truecommunity.dto.PasswordChangeForm;
import com.truecommunity.dto.ProfileUpdateForm;
import com.truecommunity.exception.PasswordMismatchException;
import com.truecommunity.exception.UsernameAlreadyExistsException;
import com.truecommunity.model.User;
import com.truecommunity.model.Verdict;
import com.truecommunity.security.AuthenticatedUserProvider;
import com.truecommunity.services.DashboardService;
import com.truecommunity.services.ProfileService;
import com.truecommunity.services.SseChannels;
import com.truecommunity.services.SseEventService;
import com.truecommunity.session.DashboardHistoryFilter;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MVC controller for authenticated dashboard pages, profile updates, password
 * changes, session-backed history filters, and per-user dashboard SSE updates.
 */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    private final ProfileService profileService;

    private final DashboardHistoryFilter historyFilter;

    private final SseEventService sseEventService;

    public DashboardController(DashboardService dashboardService,
                              AuthenticatedUserProvider authenticatedUserProvider,
                              ProfileService profileService,
                              DashboardHistoryFilter historyFilter,
                              SseEventService sseEventService) {
        this.dashboardService = dashboardService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.profileService = profileService;
        this.historyFilter = historyFilter;
        this.sseEventService = sseEventService;
    }

    /**
     * Renders the authenticated user's dashboard and updates the session-backed
     * history filter from optional query parameters.
     *
     * @param outcome optional outcome filter
     * @param verdict optional verdict filter
     * @param model MVC model
     * @param authentication current authentication
     * @return dashboard template
     */
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String outcome,
                            @RequestParam(required = false) String verdict,
                            Model model,
                            Authentication authentication) {
        User user = authenticatedUserProvider.getAuthenticatedUser(authentication);
        applyFilterParams(outcome, verdict);
        addLiveData(model, user);

        if (!model.containsAttribute("profileUpdateForm")) {
            ProfileUpdateForm profileForm = new ProfileUpdateForm();
            profileForm.setUsername(user.getUsername());
            model.addAttribute("profileUpdateForm", profileForm);
        }
        if (!model.containsAttribute("passwordChangeForm")) {
            model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        }
        return "dashboard";
    }

    /**
     * Returns only the live data fragment, honoring the same session filter, for SSE swaps.
     *
     * @param model MVC model
     * @param authentication current authentication
     * @return dashboard live-data fragment
     */
    @GetMapping("/dashboard/data-fragment")
    public String dashboardDataFragment(Model model, Authentication authentication) {
        User user = authenticatedUserProvider.getAuthenticatedUser(authentication);
        addLiveData(model, user);
        return "dashboard :: liveData";
    }

    /**
     * Opens a per-user SSE stream. The channel is derived from the authenticated user only.
     *
     * @param authentication current authentication
     * @return SSE emitter subscribed to dashboard updates
     */
    @GetMapping(value = "/dashboard/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter dashboardEvents(Authentication authentication) {
        User user = authenticatedUserProvider.getAuthenticatedUser(authentication);
        return sseEventService.subscribe(userChannel(user.getId()));
    }

    /**
     * Updates the current user's username and redirects with flash feedback.
     *
     * @param form submitted profile form
     * @param bindingResult validation result
     * @param authentication current authentication
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to dashboard
     */
    @PostMapping("/dashboard/profile")
    public String updateProfile(@Valid @ModelAttribute("profileUpdateForm") ProfileUpdateForm form,
                                BindingResult bindingResult,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        User user = authenticatedUserProvider.getAuthenticatedUser(authentication);
        if (!bindingResult.hasErrors()) {
            try {
                profileService.updateUsername(user, form.getUsername());
                redirectAttributes.addFlashAttribute("profileSuccess", "שם המשתמש עודכן בהצלחה.");
                return "redirect:/dashboard";
            } catch (UsernameAlreadyExistsException ex) {
                bindingResult.rejectValue("username", "username.exists", "שם המשתמש כבר תפוס על ידי משתמש אחר.");
            } catch (IllegalArgumentException ex) {
                bindingResult.rejectValue("username", "username.size", "שם המשתמש חייב להיות באורך 3 עד 50 תווים.");
            }
        }
        // Re-render the dashboard with inline field errors and reopen the edit-username modal.
        addLiveData(model, user);
        if (!model.containsAttribute("passwordChangeForm")) {
            model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        }
        model.addAttribute("openModal", "username");
        return "dashboard";
    }

    /**
     * Changes the current user's password and redirects with flash feedback.
     *
     * @param form submitted password form
     * @param bindingResult validation result
     * @param authentication current authentication
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to dashboard
     */
    @PostMapping("/dashboard/password")
    public String changePassword(@Valid @ModelAttribute("passwordChangeForm") PasswordChangeForm form,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        User user = authenticatedUserProvider.getAuthenticatedUser(authentication);
        if (!bindingResult.hasErrors() && !form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "אימות הסיסמה אינו תואם לסיסמה החדשה.");
        }
        if (!bindingResult.hasErrors()) {
            try {
                profileService.changePassword(user, form.getCurrentPassword(), form.getNewPassword());
                redirectAttributes.addFlashAttribute("passwordSuccess", "הסיסמה עודכנה בהצלחה.");
                return "redirect:/dashboard";
            } catch (PasswordMismatchException ex) {
                bindingResult.rejectValue("currentPassword", "password.current", "הסיסמה הנוכחית שגויה.");
            }
        }
        // Re-render the dashboard with inline field errors and reopen the change-password modal.
        addLiveData(model, user);
        if (!model.containsAttribute("profileUpdateForm")) {
            ProfileUpdateForm profileForm = new ProfileUpdateForm();
            profileForm.setUsername(user.getUsername());
            model.addAttribute("profileUpdateForm", profileForm);
        }
        model.addAttribute("openModal", "password");
        return "dashboard";
    }

    /** Stores any supplied filter params into the session bean (blank verdict clears it). */
    private void applyFilterParams(String outcome, String verdict) {
        if (outcome != null) {
            try {
                historyFilter.setOutcome(HistoryOutcome.valueOf(outcome));
            } catch (IllegalArgumentException ignored) {
                // Ignore unknown outcome values; keep the previous selection.
            }
        }
        if (verdict != null) {
            if (verdict.isBlank()) {
                historyFilter.setVerdict(null);
            } else {
                try {
                    historyFilter.setVerdict(Verdict.valueOf(verdict));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown verdict values; keep the previous selection.
                }
            }
        }
    }

    private void addLiveData(Model model, User user) {
        model.addAttribute("dashboard",
                dashboardService.buildDashboard(user, historyFilter.getOutcome(), historyFilter.getVerdict()));
        model.addAttribute("verdicts", Verdict.values());
        model.addAttribute("historyFilter", historyFilter);
    }

    private String userChannel(Long userId) {
        return SseChannels.user(userId);
    }
}
