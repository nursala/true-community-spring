package com.truecommunity.controllers;

import com.truecommunity.dto.ArenaView;
import com.truecommunity.dto.CommentForm;
import com.truecommunity.dto.CommentRequest;
import com.truecommunity.dto.CommentView;
import com.truecommunity.dto.VoteForm;
import com.truecommunity.dto.VoteRequest;
import com.truecommunity.exception.CommentNotFoundException;
import com.truecommunity.exception.InvalidStatementStateException;
import com.truecommunity.exception.InvalidTopicSelectionException;
import com.truecommunity.exception.InvalidUrlException;
import com.truecommunity.exception.StatementNotFoundException;
import com.truecommunity.exception.UnauthorizedDiscussionAccessException;
import com.truecommunity.exception.VoteAlreadyExistsException;
import com.truecommunity.exception.VotingClosedException;
import com.truecommunity.model.CommentVoteType;
import com.truecommunity.model.Role;
import com.truecommunity.model.Statement;
import com.truecommunity.model.StatementStatus;
import com.truecommunity.model.User;
import com.truecommunity.model.Verdict;
import com.truecommunity.services.ArenaService;
import com.truecommunity.services.CommentService;
import com.truecommunity.services.CommentVoteService;
import com.truecommunity.services.EditorDecisionService;
import com.truecommunity.services.StatementService;
import com.truecommunity.services.SseChannels;
import com.truecommunity.services.SseEvents;
import com.truecommunity.services.SseEventService;
import com.truecommunity.services.UserService;
import com.truecommunity.services.VoteService;
import com.truecommunity.session.RecentlyViewedStatements;
import com.truecommunity.session.VoteDrafts;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MVC controller for the statement arena page. It renders statement details,
 * handles vote/comment form redirects, stores vote drafts in the HTTP session,
 * and exposes statement-scoped SSE updates after discussion access is allowed.
 */
@Controller
public class ArenaController {

    private final StatementService statementService;

    private final VoteService voteService;

    private final CommentService commentService;

    private final UserService userService;

    private final CommentVoteService commentVoteService;

    private final ArenaService arenaService;

    private final SseEventService sseEventService;

    private final EditorDecisionService editorDecisionService;

    private final RecentlyViewedStatements recentlyViewed;

    private final VoteDrafts voteDrafts;

    public ArenaController(StatementService statementService,
                           VoteService voteService,
                           CommentService commentService,
                           UserService userService,
                           CommentVoteService commentVoteService,
                           ArenaService arenaService,
                           SseEventService sseEventService,
                           EditorDecisionService editorDecisionService,
                           RecentlyViewedStatements recentlyViewed,
                           VoteDrafts voteDrafts) {
        this.statementService = statementService;
        this.voteService = voteService;
        this.commentService = commentService;
        this.userService = userService;
        this.commentVoteService = commentVoteService;
        this.arenaService = arenaService;
        this.sseEventService = sseEventService;
        this.editorDecisionService = editorDecisionService;
        this.recentlyViewed = recentlyViewed;
        this.voteDrafts = voteDrafts;
    }

    /**
     * Renders one statement arena and records it in the per-session recent list.
     *
     * @param id statement id
     * @param commentSort optional comment sort mode
     * @param model MVC model
     * @param authentication current authentication, possibly anonymous
     * @return arena template or feed redirect when the statement is missing
     */
    @GetMapping("/arena/{id}")
    public String arena(@PathVariable Long id,
                        @RequestParam(name = "commentSort", required = false) String commentSort,
                        Model model,
                        Authentication authentication) {
        Optional<Statement> found = statementService.findById(id);
        if (found.isEmpty()) {
            return "redirect:/feed";
        }
        Statement statement = found.get();

        recentlyViewed.add(statement.getId(), statement.getText());

        User currentUser = currentUserOrNull(authentication);
        boolean alreadyVoted = currentUser != null && voteService.hasUserAlreadyVoted(id, currentUser.getId());
        boolean isEditor = currentUser != null && currentUser.getRole() == Role.EDITOR;
        // Anti-bias rule: hide community stats from regular users until they vote.
        boolean canViewStats = isEditor || alreadyVoted;

        if (!model.containsAttribute("voteForm")) {
            // Voters suggest a topic but do not see editor-assigned classification here.
            model.addAttribute("voteForm", voteDrafts.getOrCreate(id));
        }
        if (!model.containsAttribute("commentForm")) {
            model.addAttribute("commentForm", new CommentForm());
        }

        ArenaView arena = arenaService.buildArenaView(id, currentUser);
        model.addAttribute("statement", statement);
        model.addAttribute("recentlyViewed", recentlyViewed.getItems());
        model.addAttribute("consensus", arena.getConsensus());
        model.addAttribute("leadingVerdict", arena.getLeadingVerdict());
        model.addAttribute("leadingPercent", arena.getLeadingPercent());
        model.addAttribute("comments", sortComments(arena.getComments(), commentSort));
        model.addAttribute("commentSort", normalizeSort(commentSort));
        model.addAttribute("verdicts", Verdict.values());
        model.addAttribute("voteTopics", arena.getVoteTopics());
        model.addAttribute("alreadyVoted", alreadyVoted);
        model.addAttribute("canViewStats", canViewStats);
        model.addAttribute("canViewDiscussion", arena.isCanViewDiscussion());
        model.addAttribute("related", arena.getRelated());
        // Expose the editor's final decision only once the statement is DECIDED (single lookup, null-safe).
        model.addAttribute("editorDecision",
                statement.getStatus() == StatementStatus.DECIDED
                        ? editorDecisionService.findByStatement(id).orElse(null)
                        : null);

        return "arena";
    }

    /**
     * Opens the statement-scoped SSE stream on channel {@code statement:<id>}.
     * The current viewer must satisfy the same discussion-access rule as the page.
     *
     * @param id statement id
     * @param authentication current authentication
     * @return SSE emitter subscribed to statement events
     */
    @GetMapping(value = "/arena/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter statementEvents(@PathVariable Long id, Authentication authentication) {
        User currentUser = currentUserOrNull(authentication);
        if (!canViewDiscussion(id, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return sseEventService.subscribe(statementChannel(id));
    }

    /**
     * Returns only the discussion comments fragment, server-rendered, for in-place
     * SSE refreshes. Enforces the same gating as the SSE endpoint so the discussion
     * is never leaked to viewers who may not see it.
     *
     * @param id statement id
     * @param commentSort optional comment sort mode
     * @param model MVC model
     * @param authentication current authentication
     * @return Thymeleaf comments fragment
     */
    @GetMapping("/arena/{id}/comments-fragment")
    public String commentsFragment(@PathVariable Long id,
                                   @RequestParam(name = "commentSort", required = false) String commentSort,
                                   Model model,
                                   Authentication authentication) {
        Statement statement = statementService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        User currentUser = currentUserOrNull(authentication);
        if (!canViewDiscussion(id, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        ArenaView arena = arenaService.buildArenaView(id, currentUser);
        model.addAttribute("statement", statement);
        model.addAttribute("comments", sortComments(arena.getComments(), commentSort));
        model.addAttribute("commentSort", normalizeSort(commentSort));
        model.addAttribute("canViewDiscussion", true);
        return "arena :: commentsList";
    }

    /**
     * Handles MVC vote submission and clears the vote draft only after success.
     *
     * @param id statement id
     * @param voteForm submitted vote form
     * @param bindingResult validation result
     * @param authentication current authentication
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to the arena page
     */
    @PostMapping("/arena/{id}/vote")
    public String vote(@PathVariable Long id,
                       @Valid @ModelAttribute("voteForm") VoteForm voteForm,
                       BindingResult bindingResult,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("voteError", "בחרו נושא וסיווג לפני ההצבעה.");
            return "redirect:/arena/" + id;
        }

        try {
            voteService.submitVote(id, toVoteRequest(voteForm), authentication);
            voteDrafts.clear(id);
            redirectAttributes.addFlashAttribute("voteSuccess", "ההצבעה נקלטה. תודה על התרומה לקונצנזוס.");
        } catch (VoteAlreadyExistsException | InvalidTopicSelectionException | InvalidUrlException
                 | StatementNotFoundException | InvalidStatementStateException | VotingClosedException ex) {
            redirectAttributes.addFlashAttribute("voteError", ex.getMessage());
        }

        return "redirect:/arena/" + id;
    }

    /**
     * Auto-saves one statement vote draft in the current HTTP session.
     *
     * @param id statement id
     * @param voteForm current form values
     * @return {@code 204 No Content}
     */
    @PostMapping("/arena/{id}/vote/draft")
    public ResponseEntity<Void> saveVoteDraft(@PathVariable Long id,
                                              @ModelAttribute("voteForm") VoteForm voteForm) {
        voteDrafts.save(id, voteForm);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clears one statement vote draft from the current HTTP session.
     *
     * @param id statement id
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to the arena page
     */
    @PostMapping("/arena/{id}/vote/draft/clear")
    public String clearVoteDraft(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        voteDrafts.clear(id);
        redirectAttributes.addFlashAttribute("voteSuccess", "טיוטת ההצבעה נמחקה.");
        return "redirect:/arena/" + id;
    }

    /**
     * Handles MVC comment submission. Authorization and closed-statement checks
     * are enforced by the comment service.
     *
     * @param id statement id
     * @param commentForm submitted comment form
     * @param bindingResult validation result
     * @param authentication current authentication
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to the arena page
     */
    @PostMapping("/arena/{id}/comment")
    public String comment(@PathVariable Long id,
                          @Valid @ModelAttribute("commentForm") CommentForm commentForm,
                          BindingResult bindingResult,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("commentError", "לא ניתן לשלוח תגובה ריקה.");
            return "redirect:/arena/" + id;
        }

        try {
            CommentRequest request = new CommentRequest();
            request.setContent(commentForm.getContent());
            commentService.addComment(id, request, authentication);
            sseEventService.publish(statementChannel(id), SseEvents.COMMENT_ADDED, Map.of("statementId", id));
            redirectAttributes.addFlashAttribute("commentSuccess", "התגובה נוספה.");
        } catch (StatementNotFoundException | VotingClosedException ex) {
            redirectAttributes.addFlashAttribute("commentError", ex.getMessage());
        }

        return "redirect:/arena/" + id;
    }

    /**
     * Handles MVC comment reactions and publishes {@code COMMENT_VOTE_UPDATED}
     * after a successful reaction mutation.
     *
     * @param commentId comment id
     * @param type requested reaction type
     * @param authentication current authentication
     * @param redirectAttributes flash attributes for redirect feedback
     * @return redirect to the owning arena page or feed fallback
     */
    @PostMapping("/arena/comments/{commentId}/vote")
    public String voteOnComment(@PathVariable Long commentId,
                                @RequestParam CommentVoteType type,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User user = currentUserOrNull(authentication);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            Long statementId = commentVoteService.castVote(commentId, user, type);
            long netScore = commentVoteService.getNetScore(commentId);
            sseEventService.publish(statementChannel(statementId), SseEvents.COMMENT_VOTE_UPDATED,
                    Map.of("statementId", statementId, "commentId", commentId, "netScore", netScore));
            return "redirect:/arena/" + statementId;
        } catch (UnauthorizedDiscussionAccessException ex) {
            redirectAttributes.addFlashAttribute("commentError",
                    "יש להצביע על האמירה לפני הצטרפות או צפייה בדיון.");
            return "redirect:/feed";
        } catch (CommentNotFoundException | VotingClosedException ex) {
            redirectAttributes.addFlashAttribute("commentError", ex.getMessage());
            return "redirect:/feed";
        }
    }

    private VoteRequest toVoteRequest(VoteForm form) {
        VoteRequest request = new VoteRequest();
        request.setVerdict(form.getVerdict());
        // Crowdsourced classification: forward the voter's single topic suggestion.
        request.setSuggestedTopicId(form.getSuggestedTopicId());
        // The optional comment text is published as a public comment by the service.
        request.setExplanation(form.getCommentText());
        request.setEvidenceUrl(form.getEvidenceUrl());
        return request;
    }

    /** Normalises the requested comment sort to a known value ("top", "low", or "new" default). */
    private String normalizeSort(String commentSort) {
        if ("top".equalsIgnoreCase(commentSort)) {
            return "top";
        }
        if ("low".equalsIgnoreCase(commentSort)) {
            return "low";
        }
        return "new";
    }

    /**
     * Sorts the discussion comments: "top" by net score descending, "low" by net score
     * ascending (both with newest as tiebreaker), otherwise newest first. Shared by the
     * page handler and the fragment endpoint.
     */
    private List<CommentView> sortComments(List<CommentView> comments, String commentSort) {
        Comparator<CommentView> byNewest = Comparator.comparing(CommentView::getCreatedAt).reversed();
        Comparator<CommentView> comparator;
        if ("top".equalsIgnoreCase(commentSort)) {
            comparator = Comparator.comparingLong(CommentView::getNetScore).reversed().thenComparing(byNewest);
        } else if ("low".equalsIgnoreCase(commentSort)) {
            comparator = Comparator.comparingLong(CommentView::getNetScore).thenComparing(byNewest);
        } else {
            comparator = byNewest;
        }
        return comments.stream().sorted(comparator).toList();
    }

    private boolean canViewDiscussion(Long statementId, User currentUser) {
        return currentUser != null
                && (currentUser.getRole() == Role.EDITOR
                || voteService.hasUserAlreadyVoted(statementId, currentUser.getId()));
    }

    private String statementChannel(Long statementId) {
        return SseChannels.statement(statementId);
    }

    private User currentUserOrNull(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
