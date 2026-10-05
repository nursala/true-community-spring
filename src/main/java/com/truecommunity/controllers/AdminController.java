package com.truecommunity.controllers;

import com.truecommunity.dto.AdminUsersView;
import com.truecommunity.dto.EditorStatementForm;
import com.truecommunity.dto.SpeakerForm;
import com.truecommunity.dto.UserDemotionResult;
import com.truecommunity.exception.CannotDemoteLastEditorException;
import com.truecommunity.exception.CannotDemoteProtectedUserException;
import com.truecommunity.exception.CannotDemoteSelfException;
import com.truecommunity.exception.SpeakerInUseException;
import com.truecommunity.exception.StatementInUseException;
import com.truecommunity.model.Role;
import com.truecommunity.model.Speaker;
import com.truecommunity.model.Statement;
import com.truecommunity.model.StatementStatus;
import com.truecommunity.model.Topic;
import com.truecommunity.model.User;
import com.truecommunity.security.AuthenticatedUserProvider;
import com.truecommunity.services.SpeakerService;
import com.truecommunity.services.SseChannels;
import com.truecommunity.services.SseEvents;
import com.truecommunity.services.SseEventService;
import com.truecommunity.services.StatementService;
import com.truecommunity.services.UserService;
import com.truecommunity.session.AdminStatementSearch;
import com.truecommunity.session.AdminUserSearch;
import com.truecommunity.util.UrlValidator;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MVC controller for editor-protected administration pages: speaker management,
 * user role management, and their live SSE update streams.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final SpeakerService speakerService;

    private final UserService userService;

    private final StatementService statementService;

    private final SseEventService sseEventService;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    private final AdminUserSearch adminUserSearch;

    private final AdminStatementSearch adminStatementSearch;

    public AdminController(SpeakerService speakerService,
                           UserService userService,
                           StatementService statementService,
                           SseEventService sseEventService,
                           AuthenticatedUserProvider authenticatedUserProvider,
                           AdminUserSearch adminUserSearch,
                           AdminStatementSearch adminStatementSearch) {
        this.speakerService = speakerService;
        this.userService = userService;
        this.statementService = statementService;
        this.sseEventService = sseEventService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.adminUserSearch = adminUserSearch;
        this.adminStatementSearch = adminStatementSearch;
    }

    /**
     * Renders the speaker management page with create/edit forms.
     *
     * @param model MVC model
     * @return speaker management template
     */
    @GetMapping("/speakers")
    public String speakers(Model model) {
        populateSpeakersModel(model, null, null);
        return "admin/speakers";
    }

    /**
     * Opens the editor-only speaker-management SSE stream on channel {@code admin-speakers}.
     *
     * @return SSE emitter subscribed to speaker events
     */
    @GetMapping(value = "/speakers/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter speakerEvents() {
        return sseEventService.subscribe(SseChannels.ADMIN_SPEAKERS);
    }

    /**
     * Creates a speaker and publishes {@code SPEAKER_CREATED} after service success.
     *
     * @param form submitted speaker fields
     * @param bindingResult validation result
     * @param model MVC model used when validation fails
     * @param redirectAttributes flash attributes for redirect feedback
     * @return speaker page view on validation failure or redirect after success
     */
    @PostMapping("/speakers")
    public String addSpeaker(@Valid @ModelAttribute("speakerForm") SpeakerForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        validateSpeakerForm(form, null, bindingResult);
        if (bindingResult.hasErrors()) {
            populateSpeakersModel(model, null, form);
            return "admin/speakers";
        }

        Speaker speaker = speakerService.createSpeaker(form);
        publishSpeakerEvent(SseEvents.SPEAKER_CREATED, speaker.getId());
        redirectAttributes.addFlashAttribute("success", "הדובר נוסף בהצלחה.");
        return "redirect:/admin/speakers";
    }

    /**
     * Updates a speaker and publishes {@code SPEAKER_UPDATED} after service success.
     *
     * @param id speaker id
     * @param form submitted replacement fields
     * @param bindingResult validation result
     * @param model MVC model used when validation fails
     * @param redirectAttributes flash attributes for redirect feedback
     * @return speaker page view on validation failure or redirect after success
     */
    @PostMapping("/speakers/{id}")
    public String editSpeaker(@PathVariable Long id,
                              @Valid @ModelAttribute("editSpeakerForm") SpeakerForm form,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        validateSpeakerForm(form, id, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("editSpeakerErrors", fieldErrors(bindingResult));
            populateSpeakersModel(model, id, form);
            return "admin/speakers";
        }

        Speaker speaker = speakerService.updateSpeaker(id, form);
        publishSpeakerEvent(SseEvents.SPEAKER_UPDATED, speaker.getId());
        redirectAttributes.addFlashAttribute("success", "פרטי הדובר עודכנו.");
        return "redirect:/admin/speakers";
    }

    /**
     * Deletes a speaker unless it is referenced by statements, then redirects
     * with Hebrew flash feedback and publishes {@code SPEAKER_DELETED} only on success.
     *
     * @param id speaker id
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to speaker management
     */
    @PostMapping("/speakers/{id}/delete")
    public String deleteSpeaker(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            speakerService.deleteSpeaker(id);
            publishSpeakerEvent(SseEvents.SPEAKER_DELETED, id);
            redirectAttributes.addFlashAttribute("success", "הדובר נמחק בהצלחה.");
        } catch (SpeakerInUseException ex) {
            redirectAttributes.addFlashAttribute("error", "לא ניתן למחוק דובר שמשויך לאמירות קיימות.");
        }
        return "redirect:/admin/speakers";
    }

    /**
     * Renders the editor-protected user-management page.
     *
     * @param model MVC model
     * @return user management template
     */
    @GetMapping("/users")
    public String users(Model model) {
        populateUsersModel(model);
        return "admin/users";
    }

    /**
     * Opens the editor-only user-management SSE stream on channel {@code admin-users}.
     *
     * @return SSE emitter subscribed to user role events
     */
    @GetMapping(value = "/users/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter userEvents() {
        return sseEventService.subscribe(SseChannels.ADMIN_USERS);
    }

    /**
     * Stores the admin user search query in the current HTTP session.
     *
     * @param query optional username/email query
     * @return redirect to user management
     */
    @PostMapping("/users/search")
    public String searchUsers(@RequestParam(name = "q", required = false) String query) {
        adminUserSearch.setQuery(query);
        return "redirect:/admin/users";
    }

    /**
     * Clears the session-backed admin user search query.
     *
     * @return redirect to user management
     */
    @PostMapping("/users/search/clear")
    public String clearUserSearch() {
        adminUserSearch.clear();
        return "redirect:/admin/users";
    }

    /**
     * Promotes a user to editor and publishes {@code USER_PROMOTED} when the role changes.
     *
     * @param id user id
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to user management
     */
    @PostMapping("/users/{id}/promote")
    public String promoteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean roleWillChange = userService.findById(id)
                .map(user -> user.getRole() != Role.EDITOR)
                .orElse(true);
        User promoted = userService.promoteToEditor(id);
        if (roleWillChange) {
            publishAdminUserEvent(SseEvents.USER_PROMOTED, promoted.getId());
        }
        redirectAttributes.addFlashAttribute("success", "המשתמש " + promoted.getUsername() + " שודרג לעורך.");
        return "redirect:/admin/users";
    }

    /**
     * Demotes an editor to user, releases any claimed tasks through the service,
     * and publishes role/task SSE events after successful service completion.
     *
     * @param id user id
     * @param authentication current editor authentication
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to user management
     */
    @PostMapping("/users/{id}/demote")
    public String demoteUser(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User currentEditor = authenticatedUserProvider.getAuthenticatedUser(authentication);
        try {
            UserDemotionResult result = userService.demoteToUser(id, currentEditor);
            if (result.roleChanged()) {
                publishAdminUserEvent(SseEvents.USER_DEMOTED, result.userId());
            }
            result.releasedStatementIds().forEach(this::publishTaskReleased);
            redirectAttributes.addFlashAttribute("success", "העורך הורד לדרגת משתמש רגיל.");
        } catch (CannotDemoteSelfException ex) {
            redirectAttributes.addFlashAttribute("error", "לא ניתן להוריד את עצמך מתפקיד העורך.");
        } catch (CannotDemoteProtectedUserException ex) {
            redirectAttributes.addFlashAttribute("error", "לא ניתן להוריד את העורך הראשי המוגן של המערכת.");
        } catch (CannotDemoteLastEditorException ex) {
            redirectAttributes.addFlashAttribute("error", "לא ניתן להוריד את העורך האחרון שנותר במערכת.");
        }
        return "redirect:/admin/users";
    }

    /**
     * Renders the editor-protected statement management page with the current
     * session filters applied.
     *
     * @param model MVC model
     * @return statement management template
     */
    @GetMapping("/statements")
    public String statements(Model model) {
        populateStatementsModel(model, null, null);
        return "admin/statements";
    }

    /**
     * Stores the statement search/filter/sort selection in the session and
     * redirects back to the management page.
     *
     * @param keyword keyword or exact statement id
     * @param speakerId optional speaker filter
     * @param topic optional topic-name filter
     * @param status optional status filter
     * @param sort optional sort key
     * @return redirect to statement management
     */
    @PostMapping("/statements/search")
    public String searchStatements(@RequestParam(name = "keyword", required = false) String keyword,
                                   @RequestParam(name = "speakerId", required = false) Long speakerId,
                                   @RequestParam(name = "topic", required = false) String topic,
                                   @RequestParam(name = "status", required = false) StatementStatus status,
                                   @RequestParam(name = "sort", required = false) String sort) {
        adminStatementSearch.setKeyword(keyword);
        adminStatementSearch.setSpeakerId(speakerId);
        adminStatementSearch.setTopic(topic);
        adminStatementSearch.setStatus(status);
        adminStatementSearch.setSort(sort);
        return "redirect:/admin/statements";
    }

    /**
     * Clears the session-backed statement filters and restores the default sort.
     *
     * @return redirect to statement management
     */
    @PostMapping("/statements/search/clear")
    public String clearStatementSearch() {
        adminStatementSearch.clear();
        return "redirect:/admin/statements";
    }

    /**
     * Edits all fields of a statement at any status, re-rendering the page with
     * inline Hebrew validation errors on bad input.
     *
     * @param id statement id
     * @param form submitted replacement fields
     * @param bindingResult validation result
     * @param model MVC model used when validation fails
     * @param redirectAttributes flash attributes for redirect feedback
     * @return management view on validation failure or redirect after success
     */
    @PostMapping("/statements/{id}")
    public String editStatement(@PathVariable Long id,
                                @Valid @ModelAttribute("editStatementForm") EditorStatementForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (hasText(form.getSourceUrl()) && !isHttpUrl(form.getSourceUrl())) {
            bindingResult.rejectValue("sourceUrl", "invalidUrl", "יש להזין כתובת http או https בלבד.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("editStatementErrors", fieldErrors(bindingResult));
            populateStatementsModel(model, id, form);
            return "admin/statements";
        }

        statementService.updateStatement(id, form);
        redirectAttributes.addFlashAttribute("success", "האמירה עודכנה בהצלחה.");
        return "redirect:/admin/statements";
    }

    /**
     * Deletes a statement when the guard allows it, otherwise shows a Hebrew
     * flash error explaining why the deletion was blocked.
     *
     * @param id statement id
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to statement management
     */
    @PostMapping("/statements/{id}/delete")
    public String deleteStatement(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            statementService.deleteStatement(id);
            redirectAttributes.addFlashAttribute("success", "האמירה נמחקה בהצלחה.");
        } catch (StatementInUseException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/statements";
    }

    private void publishSpeakerEvent(String eventName, Long speakerId) {
        sseEventService.publish(SseChannels.ADMIN_SPEAKERS, eventName, Map.of("speakerId", speakerId));
    }

    private void publishAdminUserEvent(String eventName, Long userId) {
        sseEventService.publish(SseChannels.ADMIN_USERS, eventName, Map.of("userId", userId));
    }

    private void publishTaskReleased(Long statementId) {
        Map<String, Long> payload = Map.of("statementId", statementId);
        sseEventService.publish(SseChannels.EDITOR_DESK, SseEvents.TASK_RELEASED, payload);
        sseEventService.publish(SseChannels.PUBLIC_FEED, SseEvents.TASK_RELEASED, payload);
    }

    private void populateSpeakersModel(Model model, Long editSpeakerId, SpeakerForm rejectedEditForm) {
        List<Speaker> speakers = speakerService.getAllSpeakers();
        Map<Long, SpeakerForm> editForms = new LinkedHashMap<>();
        for (Speaker speaker : speakers) {
            SpeakerForm form = speaker.getId().equals(editSpeakerId) && rejectedEditForm != null
                    ? rejectedEditForm
                    : new SpeakerForm(speaker.getName(), speaker.getRole(), speaker.getProfileUrl(), speaker.getImageUrl());
            editForms.put(speaker.getId(), form);
        }
        if (!model.containsAttribute("speakerForm")) {
            model.addAttribute("speakerForm", editSpeakerId == null && rejectedEditForm != null
                    ? rejectedEditForm
                    : new SpeakerForm());
        }
        model.addAttribute("speakers", speakers);
        model.addAttribute("editSpeakerForms", editForms);
        model.addAttribute("editSpeakerId", editSpeakerId);
        if (!model.containsAttribute("editSpeakerErrors")) {
            model.addAttribute("editSpeakerErrors", Map.of());
        }
    }

    private void populateUsersModel(Model model) {
        AdminUsersView view = userService.getAdminUsersView(adminUserSearch.getQuery());
        model.addAttribute("editors", view.getEditors());
        model.addAttribute("regularUsers", view.getUsers());
        model.addAttribute("usersCapped", view.isUsersCapped());
        model.addAttribute("searchQuery", adminUserSearch.getQuery());
        model.addAttribute("searchActive", adminUserSearch.isActive());
    }

    private void populateStatementsModel(Model model, Long editStatementId, EditorStatementForm rejectedEditForm) {
        List<Statement> statements = statementService.getManagedStatements(
                adminStatementSearch.getKeyword(),
                adminStatementSearch.getSpeakerId(),
                adminStatementSearch.getTopic(),
                adminStatementSearch.getStatus(),
                adminStatementSearch.getSort()
        );

        Map<Long, EditorStatementForm> editForms = new LinkedHashMap<>();
        for (Statement statement : statements) {
            EditorStatementForm form = statement.getId().equals(editStatementId) && rejectedEditForm != null
                    ? rejectedEditForm
                    : formFromStatement(statement);
            editForms.put(statement.getId(), form);
        }

        model.addAttribute("statements", statements);
        model.addAttribute("editStatementForms", editForms);
        model.addAttribute("speakers", speakerService.getAllSpeakers());
        model.addAttribute("topics", statementService.getAllTopics());
        model.addAttribute("statuses", StatementStatus.values());
        model.addAttribute("editStatementId", editStatementId);
        if (!model.containsAttribute("editStatementErrors")) {
            model.addAttribute("editStatementErrors", Map.of());
        }
        model.addAttribute("searchKeyword", adminStatementSearch.getKeyword());
        model.addAttribute("searchSpeakerId", adminStatementSearch.getSpeakerId());
        model.addAttribute("searchTopic", adminStatementSearch.getTopic());
        model.addAttribute("searchStatus", adminStatementSearch.getStatus());
        model.addAttribute("searchSort", adminStatementSearch.getSort());
        model.addAttribute("searchActive", adminStatementSearch.isActive());
    }

    private EditorStatementForm formFromStatement(Statement statement) {
        EditorStatementForm form = new EditorStatementForm();
        form.setText(statement.getText());
        form.setSpeakerId(statement.getSpeaker() == null ? null : statement.getSpeaker().getId());
        form.setSourceUrl(statement.getSourceUrl());
        form.setTopicIds(statement.getTopics().stream()
                .map(Topic::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        return form;
    }

    private Map<String, String> fieldErrors(BindingResult bindingResult) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return errors;
    }

    private void validateSpeakerForm(SpeakerForm form, Long currentId, BindingResult bindingResult) {
        if (hasText(form.getProfileUrl()) && !isHttpUrl(form.getProfileUrl())) {
            bindingResult.rejectValue("profileUrl", "invalidUrl", "יש להזין כתובת http או https בלבד.");
        }
        if (hasText(form.getImageUrl()) && !isHttpUrl(form.getImageUrl())) {
            bindingResult.rejectValue("imageUrl", "invalidUrl", "יש להזין כתובת http או https בלבד.");
        }

        if (hasText(form.getName())) {
            speakerService.findByNameIgnoreCase(form.getName().trim())
                    .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                    .ifPresent(existing -> bindingResult.rejectValue("name", "duplicate", "שם הדובר כבר קיים."));
        }
    }

    private boolean isHttpUrl(String value) {
        return UrlValidator.isValidHttpUrl(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
