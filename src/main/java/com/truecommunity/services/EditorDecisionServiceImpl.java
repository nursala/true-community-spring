package com.truecommunity.services;

import com.truecommunity.dto.EditorDecisionRequest;
import com.truecommunity.exception.EditorDecisionAlreadyExistsException;
import com.truecommunity.exception.InvalidStatementStateException;
import com.truecommunity.exception.InvalidTopicSelectionException;
import com.truecommunity.exception.InvalidUrlException;
import com.truecommunity.exception.StatementNotFoundException;
import com.truecommunity.exception.UnauthorizedEditorActionException;
import com.truecommunity.model.EditorDecision;
import com.truecommunity.model.Role;
import com.truecommunity.model.Statement;
import com.truecommunity.model.StatementStatus;
import com.truecommunity.model.Topic;
import com.truecommunity.model.User;
import com.truecommunity.model.UserActivityType;
import com.truecommunity.repo.EditorDecisionRepository;
import com.truecommunity.repo.StatementRepository;
import com.truecommunity.repo.TopicRepository;
import com.truecommunity.security.AuthenticatedUserProvider;
import com.truecommunity.util.UrlValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Transactional editor decision workflow. It validates editor role and task
 * ownership, publishes or reverts final decisions, updates trust scores, and
 * schedules dashboard SSE updates after the surrounding transaction commits.
 */
@Service
@Transactional
public class EditorDecisionServiceImpl implements EditorDecisionService {

    private final EditorDecisionRepository editorDecisionRepository;

    private final StatementRepository statementRepository;

    private final TopicRepository topicRepository;

    private final TrustScoreService trustScoreService;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    private final UserActivityLogService userActivityLogService;

    private final SseEventService sseEventService;

    public EditorDecisionServiceImpl(EditorDecisionRepository editorDecisionRepository,
                                     StatementRepository statementRepository,
                                     TopicRepository topicRepository,
                                     TrustScoreService trustScoreService,
                                     AuthenticatedUserProvider authenticatedUserProvider,
                                     UserActivityLogService userActivityLogService,
                                     SseEventService sseEventService) {
        this.editorDecisionRepository = editorDecisionRepository;
        this.statementRepository = statementRepository;
        this.topicRepository = topicRepository;
        this.trustScoreService = trustScoreService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.userActivityLogService = userActivityLogService;
        this.sseEventService = sseEventService;
    }

    /**
     * Publishes a decision only for an under-review statement claimed by the
     * authenticated editor. The method persists the decision, final topics and
     * evidence URL, marks the statement decided, updates trust scores, logs the
     * action, and schedules affected dashboard notifications after commit.
     *
     * @param statementId the statement being decided
     * @param request the editor decision payload
     * @param authentication the current editor authentication
     * @return the saved decision
     */
    @Override
    public EditorDecision publishDecision(Long statementId, EditorDecisionRequest request, Authentication authentication) {
        Statement statement = findStatementOrThrow(statementId);
        User editor = authenticatedUserProvider.getAuthenticatedUser(authentication);

        validateEditorRole(editor);
        validateStatementAssignedToEditor(statement, editor);

        if (editorDecisionRepository.existsByStatement(statement)) {
            throw new EditorDecisionAlreadyExistsException("כבר קיימת הכרעת עורך עבור אמירה זו.");
        }

        EditorDecision decision = EditorDecision.create(
                statement,
                editor,
                request.getFinalVerdict(),
                request.getNotes()
        );

        EditorDecision savedDecision;
        try {
            // Flush immediately so a concurrent duplicate publish is caught here via
            // the DB unique constraint (uk_editor_decisions_statement) as a 409
            // instead of surfacing at commit as a 500.
            savedDecision = editorDecisionRepository.saveAndFlush(decision);
        } catch (DataIntegrityViolationException ex) {
            throw new EditorDecisionAlreadyExistsException("כבר קיימת הכרעת עורך עבור אמירה זו.");
        }
        assignTopics(statement, request.getTopicIds());
        statement.setFinalEvidenceUrl(sanitizeUrl(request.getFinalEvidenceUrl()));
        statement.markDecided();
        statementRepository.save(statement);
        List<Long> affected = trustScoreService.updateTrustScoresForDecision(statement, request.getFinalVerdict());

        userActivityLogService.log(editor, UserActivityType.DECIDED, statement, savedDecision.getId());

        // Notify the affected voters' dashboards and the acting editor's own panel.
        publishDashboardUpdatesAfterCommit(withEditor(affected, editor));

        return savedDecision;
    }

    @Override
    public Optional<EditorDecision> findByStatement(Long statementId) {
        Statement statement = findStatementOrThrow(statementId);
        return editorDecisionRepository.findByStatement(statement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EditorDecision> getDecisionsByEditor(User editor) {
        return editorDecisionRepository.findByEditorOrderByDecidedAtDesc(editor);
    }

    /**
     * Reverts a decision only when the authenticated editor originally
     * published it. Trust score changes are reversed, the decision is removed,
     * and the statement is returned to the editor's under-review workspace.
     *
     * @param statementId the statement whose decision should be reverted
     * @param authentication the current editor authentication
     */
    @Override
    public void revertDecision(Long statementId, Authentication authentication) {
        Statement statement = findStatementOrThrow(statementId);
        User editor = authenticatedUserProvider.getAuthenticatedUser(authentication);
        validateEditorRole(editor);

        EditorDecision decision = editorDecisionRepository.findByStatement(statement)
                .orElseThrow(() -> new InvalidStatementStateException("לא קיימת הכרעת עורך עבור אמירה זו."));
        if (!decision.getEditor().getId().equals(editor.getId())) {
            throw new UnauthorizedEditorActionException("רק העורך שפרסם את ההכרעה יכול לבטל אותה.");
        }

        List<Long> affected = trustScoreService.revertTrustScoresForDecision(statement);
        editorDecisionRepository.delete(decision);
        statement.setFinalEvidenceUrl(null);
        if (statement.getClaimedBy() == null || !statement.getClaimedBy().getId().equals(editor.getId())) {
            statement.claim(editor);
        } else {
            statement.setStatus(StatementStatus.UNDER_REVIEW);
        }
        statementRepository.save(statement);

        publishDashboardUpdatesAfterCommit(withEditor(affected, editor));
    }

    /** Returns the affected voter ids plus the acting editor's id (deduplicated). */
    private Set<Long> withEditor(Collection<Long> affectedUserIds, User editor) {
        Set<Long> ids = new LinkedHashSet<>(affectedUserIds);
        ids.add(editor.getId());
        return ids;
    }

    /**
     * Publishes a per-user "DASHBOARD_UPDATED" event to each affected user's channel,
     * but only AFTER the current transaction commits, so nothing is emitted on rollback.
     */
    private void publishDashboardUpdatesAfterCommit(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        // Effectively-final snapshot for the after-commit callback.
        final List<Long> idsToNotify = new ArrayList<>(userIds);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            idsToNotify.forEach(this::publishDashboardUpdate);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                idsToNotify.forEach(EditorDecisionServiceImpl.this::publishDashboardUpdate);
            }
        });
    }

    private void publishDashboardUpdate(Long userId) {
        sseEventService.publish(SseChannels.user(userId), SseEvents.DASHBOARD_UPDATED, Map.of("userId", userId));
    }

    /**
     * Assigns the editor-selected topics to the statement. When no topic ids are
     * supplied (e.g. a REST caller that omits them) the statement's existing
     * classification is left untouched.
     */
    private void assignTopics(Statement statement, Set<Long> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            return;
        }
        List<Topic> topics = topicRepository.findAllByIdIn(topicIds);
        if (topics.size() != topicIds.size()) {
            throw new InvalidTopicSelectionException("אחד או יותר מהנושאים שנבחרו אינם קיימים.");
        }
        statement.setTopics(new HashSet<>(topics));
    }

    private Statement findStatementOrThrow(Long statementId) {
        return statementRepository.findById(statementId)
                .orElseThrow(() -> new StatementNotFoundException("Statement not found with id: " + statementId));
    }

    private void validateEditorRole(User editor) {
        if (editor.getRole() != Role.EDITOR) {
            throw new UnauthorizedEditorActionException("רק עורכים יכולים לפרסם הכרעות.");
        }
    }

    /** Normalizes the optional final evidence URL, rejecting non-http/https schemes. */
    private String sanitizeUrl(String value) {
        try {
            return UrlValidator.sanitizeOptional(value);
        } catch (IllegalArgumentException ex) {
            throw new InvalidUrlException("יש להזין קישור ראיה בפורמט http או https בלבד.");
        }
    }

    private void validateStatementAssignedToEditor(Statement statement, User editor) {
        if (statement.getStatus() != StatementStatus.UNDER_REVIEW) {
            throw new InvalidStatementStateException("ניתן לפרסם הכרעה רק לאמירה שנמצאת בבדיקת עורך.");
        }
        if (statement.getClaimedBy() == null) {
            throw new InvalidStatementStateException("ניתן לפרסם הכרעה רק לאמירה ששובצה לעורך.");
        }
        if (!statement.getClaimedBy().getId().equals(editor.getId())) {
            throw new UnauthorizedEditorActionException("רק העורך שהאמירה שובצה אליו יכול לפרסם הכרעה.");
        }
    }
}
