package com.truecommunity.controllers;

import com.truecommunity.dto.EditorDecisionForm;
import com.truecommunity.dto.EditorDecisionRequest;
import com.truecommunity.dto.EditorStatementForm;
import com.truecommunity.dto.ReviewItem;
import com.truecommunity.exception.EditorDecisionAlreadyExistsException;
import com.truecommunity.exception.InvalidStatementStateException;
import com.truecommunity.exception.InvalidTopicSelectionException;
import com.truecommunity.exception.InvalidUrlException;
import com.truecommunity.exception.SpeakerNotFoundException;
import com.truecommunity.exception.StatementAlreadyClaimedException;
import com.truecommunity.exception.StatementNotFoundException;
import com.truecommunity.exception.UnauthorizedEditorActionException;
import com.truecommunity.model.User;
import com.truecommunity.model.Verdict;
import com.truecommunity.security.AuthenticatedUserProvider;
import com.truecommunity.services.EditorDecisionService;
import com.truecommunity.services.EditorDeskService;
import com.truecommunity.services.SpeakerService;
import com.truecommunity.services.SseChannels;
import com.truecommunity.services.SseEvents;
import com.truecommunity.services.SseEventService;
import com.truecommunity.services.StatementService;
import com.truecommunity.services.UserActivityLogService;
import com.truecommunity.session.EditorDecisionDrafts;
import com.truecommunity.session.EditorStatementDraft;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MVC controller for the editor workspace. It manages claim/release actions,
 * final decision publication, session-backed decision and statement drafts, and
 * the editor-only {@code editor-desk} SSE stream.
 */
@Controller
@RequestMapping("/editor")
public class EditorDeskController {

    private final StatementService statementService;

    private final EditorDecisionService editorDecisionService;

    private final EditorDeskService editorDeskService;

    private final UserActivityLogService userActivityLogService;

    private final SseEventService sseEventService;

    private final SpeakerService speakerService;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    private final EditorDecisionDrafts editorDecisionDrafts;

    private final EditorStatementDraft editorStatementDraft;

    public EditorDeskController(StatementService statementService,
                                EditorDecisionService editorDecisionService,
                                EditorDeskService editorDeskService,
                                UserActivityLogService userActivityLogService,
                                SseEventService sseEventService,
                                SpeakerService speakerService,
                                AuthenticatedUserProvider authenticatedUserProvider,
                                EditorDecisionDrafts editorDecisionDrafts,
                                EditorStatementDraft editorStatementDraft) {
        this.statementService = statementService;
        this.editorDecisionService = editorDecisionService;
        this.editorDeskService = editorDeskService;
        this.userActivityLogService = userActivityLogService;
        this.sseEventService = sseEventService;
        this.speakerService = speakerService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.editorDecisionDrafts = editorDecisionDrafts;
        this.editorStatementDraft = editorStatementDraft;
    }

    /**
     * Renders the editor desk with available statements, claimed tasks, decision
     * history, analytics, and restored session drafts.
     *
     * @param model MVC model
     * @param authentication current editor authentication
     * @return editor template
     */
    @GetMapping
    public String desk(Model model, Authentication authentication) {
        User editor = authenticatedUserProvider.getAuthenticatedUser(authentication);
        List<ReviewItem> myTasks = editorDeskService.getMyTaskReviewItems(editor);

        model.addAttribute("available", editorDeskService.getAvailableReviewItems());
        model.addAttribute("myTasks", myTasks);
        model.addAttribute("myResolvedTasks", editorDecisionService.getDecisionsByEditor(editor));
        model.addAttribute("editorTopics", statementService.getAllTopics());
        model.addAttribute("speakers", speakerService.getAllSpeakers());
        model.addAttribute("top5Comments", editorDeskService.getTopComments(myTasks));
        model.addAttribute("bottom5Comments", editorDeskService.getBottomComments(myTasks));
        model.addAttribute("topTrustUserVotes", editorDeskService.getTopTrustVotes(myTasks));
        model.addAttribute("topicSuggestions", editorDeskService.getTopicSuggestions(myTasks));
        model.addAttribute("evidenceByStatement", editorDeskService.getEvidence(myTasks));
        model.addAttribute("taskActivity", userActivityLogService.getEditorTaskActivity(myTasks));
        model.addAttribute("verdicts", Verdict.values());
        model.addAttribute("decisionForms", decisionForms(myTasks));
        if (!model.containsAttribute("newStatementForm")) {
            model.addAttribute("newStatementForm", editorStatementDraft.getOrCreate());
        }
        return "editor";
    }

    /**
     * Opens the editor-only SSE stream on channel {@code editor-desk}. Access is
     * enforced by the {@code /editor/**} security matcher (ROLE_EDITOR), which
     * returns 403 for non-editors before this handler runs, so no manual role
     * check is needed here.
     *
     * @return SSE emitter subscribed to editor desk events
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter editorDeskEvents() {
        return sseEventService.subscribe(SseChannels.EDITOR_DESK);
    }

    /**
     * Claims a pending statement for the authenticated editor and publishes
     * {@code STATEMENT_CLAIMED} after success.
     *
     * @param id statement id
     * @param authentication current editor authentication
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to editor desk
     */
    @PostMapping("/claim/{id}")
    public String claim(@PathVariable Long id,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        User editor = authenticatedUserProvider.getAuthenticatedUser(authentication);
        try {
            statementService.claimStatement(id, editor);
            publishEditorDeskEvent(SseEvents.STATEMENT_CLAIMED, id);
            redirectAttributes.addFlashAttribute("decisionSuccess", "האמירה שובצה אליך לבדיקה.");
        } catch (StatementAlreadyClaimedException | StatementNotFoundException | InvalidStatementStateException ex) {
            redirectAttributes.addFlashAttribute("decisionError", ex.getMessage());
        }
        return "redirect:/editor";
    }

    /**
     * Releases a statement claimed by the authenticated editor and publishes
     * {@code TASK_RELEASED} after success.
     *
     * @param id statement id
     * @param authentication current editor authentication
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to pending tab
     */
    @PostMapping("/{id}/release")
    public String release(@PathVariable Long id,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        User editor = authenticatedUserProvider.getAuthenticatedUser(authentication);
        try {
            statementService.releaseStatement(id, editor);
            // Returning a task to the pool changes both the desk and the public feed.
            publishEditorDeskEvent(SseEvents.TASK_RELEASED, id);
            publishFeedEvent(SseEvents.TASK_RELEASED, id);
            redirectAttributes.addFlashAttribute("decisionSuccess", "המשימה שוחררה וחזרה למאגר הזמינים.");
        } catch (StatementNotFoundException | UnauthorizedEditorActionException ex) {
            redirectAttributes.addFlashAttribute("decisionError", ex.getMessage());
        }
        return "redirect:/editor?tab=pending";
    }

    /**
     * Publishes a final editor decision from the workspace form. Validation
     * failures return the editor view with errors for the correct statement.
     *
     * @param id statement id
     * @param decisionForm submitted decision form
     * @param bindingResult validation result
     * @param authentication current editor authentication
     * @param model MVC model used on validation failure
     * @param redirectAttributes flash attributes for redirect feedback
     * @return editor view on validation failure or redirect after handling
     */
    @PostMapping("/{id}/decision")
    public String publish(@PathVariable Long id,
                          @Valid @ModelAttribute("decisionForm") EditorDecisionForm decisionForm,
                          BindingResult bindingResult,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            User editor = authenticatedUserProvider.getAuthenticatedUser(authentication);
            List<ReviewItem> myTasks = editorDeskService.getMyTaskReviewItems(editor);
            Map<Long, EditorDecisionForm> forms = decisionForms(myTasks);
            forms.put(id, decisionForm);
            populateDeskModel(model, editor, myTasks, forms);
            model.addAttribute("failedDecisionStatementId", id);
            model.addAttribute("decisionErrors", fieldErrors(bindingResult));
            model.addAttribute("decisionError", "יש לבחור הכרעה סופית ולפחות נושא אחד לפני הפרסום.");
            return "editor";
        }

        try {
            EditorDecisionRequest request = new EditorDecisionRequest();
            request.setFinalVerdict(decisionForm.getFinalVerdict());
            request.setTopicIds(decisionForm.getTopicIds());
            request.setNotes(decisionForm.getNotes());
            request.setFinalEvidenceUrl(decisionForm.getFinalEvidenceUrl());
            editorDecisionService.publishDecision(id, request, authentication);
            publishFeedEvent(SseEvents.DECISION_PUBLISHED, id);
            publishEditorDeskEvent(SseEvents.DECISION_PUBLISHED, id);
            editorDecisionDrafts.clear(id);
            redirectAttributes.addFlashAttribute("decisionSuccess", "ההכרעה פורסמה וציוני האמון עודכנו.");
        } catch (EditorDecisionAlreadyExistsException | StatementNotFoundException
                 | UnauthorizedEditorActionException | InvalidStatementStateException
                 | InvalidTopicSelectionException | InvalidUrlException ex) {
            redirectAttributes.addFlashAttribute("decisionError", ex.getMessage());
        }

        return "redirect:/editor?tab=workspace";
    }

    /**
     * Auto-saves one per-statement decision draft in the current HTTP session.
     *
     * @param id statement id
     * @param decisionForm current decision form values
     * @return {@code 204 No Content}
     */
    @PostMapping("/{id}/decision/draft")
    public ResponseEntity<Void> saveDraft(@PathVariable Long id,
                                          @ModelAttribute("decisionForm") EditorDecisionForm decisionForm) {
        editorDecisionDrafts.save(id, decisionForm);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clears one decision draft from the current HTTP session.
     *
     * @param id statement id
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to workspace tab
     */
    @PostMapping("/{id}/decision/draft/clear")
    public String clearDraft(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        editorDecisionDrafts.clear(id);
        redirectAttributes.addFlashAttribute("decisionSuccess", "טיוטת ההכרעה נמחקה.");
        return "redirect:/editor?tab=workspace";
    }

    /**
     * Creates a new pending statement from the editor modal and clears the
     * add-statement draft only after successful creation.
     *
     * @param form submitted statement form
     * @param bindingResult validation result
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to pending tab
     */
    @PostMapping("/statement/add")
    public String addStatement(@Valid @ModelAttribute("newStatementForm") EditorStatementForm form,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("decisionError", "יש להשלים את פרטי האמירה.");
            return "redirect:/editor?tab=pending";
        }

        try {
            Long statementId = statementService.createPendingStatement(form).getId();
            publishFeedEvent(SseEvents.STATEMENT_CREATED, statementId);
            publishEditorDeskEvent(SseEvents.STATEMENT_CREATED, statementId);
            editorStatementDraft.clear();
            redirectAttributes.addFlashAttribute("statementDraftCleared", true);
            redirectAttributes.addFlashAttribute("decisionSuccess", "האמירה נוספה לרשימת ההמתנה.");
            return "redirect:/editor?tab=pending";
        } catch (InvalidTopicSelectionException | SpeakerNotFoundException | InvalidUrlException ex) {
            redirectAttributes.addFlashAttribute("decisionError", ex.getMessage());
            return "redirect:/editor?tab=pending";
        }
    }

    /**
     * Auto-saves the add-statement modal draft in the current HTTP session.
     *
     * @param form current add-statement form values
     * @return {@code 204 No Content}
     */
    @PostMapping("/statement/draft")
    public ResponseEntity<Void> saveStatementDraft(@ModelAttribute("newStatementForm") EditorStatementForm form) {
        editorStatementDraft.save(form);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clears the add-statement draft from the current HTTP session.
     *
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to pending tab
     */
    @PostMapping("/statement/draft/clear")
    public String clearStatementDraft(RedirectAttributes redirectAttributes) {
        editorStatementDraft.clear();
        redirectAttributes.addFlashAttribute("decisionSuccess", "טיוטת האמירה נמחקה.");
        return "redirect:/editor?tab=pending";
    }

    /**
     * Reverts a previously published decision by the authenticated editor and
     * publishes decision-revert SSE events after success.
     *
     * @param id statement id
     * @param authentication current editor authentication
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to workspace tab
     */
    @PostMapping("/statement/{id}/revert")
    public String revertDecision(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            editorDecisionService.revertDecision(id, authentication);
            // Trust scores change on revert too, so notify the public feed (leaderboard SPA).
            publishFeedEvent(SseEvents.DECISION_REVERTED, id);
            publishEditorDeskEvent(SseEvents.DECISION_REVERTED, id);
            redirectAttributes.addFlashAttribute("decisionSuccess", "ההכרעה בוטלה והאמירה חזרה לבדיקה.");
        } catch (StatementNotFoundException | UnauthorizedEditorActionException | InvalidStatementStateException ex) {
            redirectAttributes.addFlashAttribute("decisionError", ex.getMessage());
        }
        return "redirect:/editor?tab=workspace";
    }

    private Map<Long, EditorDecisionForm> decisionForms(List<ReviewItem> myTasks) {
        Map<Long, EditorDecisionForm> forms = new LinkedHashMap<>();
        for (ReviewItem task : myTasks) {
            forms.put(task.getId(), editorDecisionDrafts.getOrCreate(task.getId()));
        }
        return forms;
    }

    /** Collects the first validation message per field for inline rendering in the shared fragment. */
    private Map<String, String> fieldErrors(BindingResult bindingResult) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return errors;
    }

    private void publishFeedEvent(String eventName, Long statementId) {
        sseEventService.publish(SseChannels.PUBLIC_FEED, eventName, Map.of("statementId", statementId));
    }

    private void publishEditorDeskEvent(String eventName, Long statementId) {
        sseEventService.publish(SseChannels.EDITOR_DESK, eventName, Map.of("statementId", statementId));
    }

    private void populateDeskModel(Model model, User editor, List<ReviewItem> myTasks,
                                   Map<Long, EditorDecisionForm> decisionForms) {
        model.addAttribute("available", editorDeskService.getAvailableReviewItems());
        model.addAttribute("myTasks", myTasks);
        model.addAttribute("myResolvedTasks", editorDecisionService.getDecisionsByEditor(editor));
        model.addAttribute("editorTopics", statementService.getAllTopics());
        model.addAttribute("speakers", speakerService.getAllSpeakers());
        model.addAttribute("top5Comments", editorDeskService.getTopComments(myTasks));
        model.addAttribute("bottom5Comments", editorDeskService.getBottomComments(myTasks));
        model.addAttribute("topTrustUserVotes", editorDeskService.getTopTrustVotes(myTasks));
        model.addAttribute("topicSuggestions", editorDeskService.getTopicSuggestions(myTasks));
        model.addAttribute("evidenceByStatement", editorDeskService.getEvidence(myTasks));
        model.addAttribute("taskActivity", userActivityLogService.getEditorTaskActivity(myTasks));
        model.addAttribute("verdicts", Verdict.values());
        model.addAttribute("decisionForms", decisionForms);
        if (!model.containsAttribute("newStatementForm")) {
            model.addAttribute("newStatementForm", editorStatementDraft.getOrCreate());
        }
    }
}
