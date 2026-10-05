package com.truecommunity.services;

import com.truecommunity.dto.EditorStatementForm;
import com.truecommunity.dto.StatementSearchForm;
import com.truecommunity.exception.InvalidStatementStateException;
import com.truecommunity.exception.InvalidTopicSelectionException;
import com.truecommunity.exception.InvalidUrlException;
import com.truecommunity.exception.StatementAlreadyClaimedException;
import com.truecommunity.exception.StatementInUseException;
import com.truecommunity.exception.StatementNotFoundException;
import com.truecommunity.exception.UnauthorizedEditorActionException;
import com.truecommunity.util.UrlValidator;
import com.truecommunity.model.EditorDecision;
import com.truecommunity.model.Speaker;
import com.truecommunity.model.Statement;
import com.truecommunity.model.StatementStatus;
import com.truecommunity.model.Topic;
import com.truecommunity.model.User;
import com.truecommunity.model.UserActivityType;
import com.truecommunity.repo.CommentRepository;
import com.truecommunity.repo.CommentVoteRepository;
import com.truecommunity.repo.EditorDecisionRepository;
import com.truecommunity.repo.StatementRepository;
import com.truecommunity.repo.TopicRepository;
import com.truecommunity.repo.UserActivityLogRepository;
import com.truecommunity.repo.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Transactional statement workflow implementation. Read methods build the
 * public/editor statement lists, while claim uses a repository-level
 * pessimistic write lock so concurrent editors cannot both claim the same row.
 */
@Service
@Transactional
public class StatementServiceImpl implements StatementService {

    private final StatementRepository statementRepository;

    private final EditorDecisionRepository editorDecisionRepository;

    private final TopicRepository topicRepository;

    private final SpeakerService speakerService;

    private final VoteRepository voteRepository;

    private final CommentRepository commentRepository;

    private final CommentVoteRepository commentVoteRepository;

    private final UserActivityLogRepository userActivityLogRepository;

    private final UserActivityLogService userActivityLogService;

    public StatementServiceImpl(StatementRepository statementRepository,
                                EditorDecisionRepository editorDecisionRepository,
                                TopicRepository topicRepository,
                                SpeakerService speakerService,
                                VoteRepository voteRepository,
                                CommentRepository commentRepository,
                                CommentVoteRepository commentVoteRepository,
                                UserActivityLogRepository userActivityLogRepository,
                                UserActivityLogService userActivityLogService) {
        this.statementRepository = statementRepository;
        this.editorDecisionRepository = editorDecisionRepository;
        this.topicRepository = topicRepository;
        this.speakerService = speakerService;
        this.voteRepository = voteRepository;
        this.commentRepository = commentRepository;
        this.commentVoteRepository = commentVoteRepository;
        this.userActivityLogRepository = userActivityLogRepository;
        this.userActivityLogService = userActivityLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Statement> getPendingStatements() {
        return statementRepository.findByStatus(StatementStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Statement> getAvailableForVoting(User user, StatementSearchForm form) {
        return filterAndSortStatements(
                statementRepository.findAvailableForVoting(StatementStatus.PENDING, user),
                form
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Statement> getPendingVerificationStatements(User user, StatementSearchForm form) {
        if (user == null) {
            return List.of();
        }
        return filterAndSortStatements(
                statementRepository.findPendingStatementsVotedByUser(
                        user,
                        List.of(StatementStatus.PENDING, StatementStatus.UNDER_REVIEW)
                ),
                form
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EditorDecision> getResolvedStatements(StatementSearchForm form) {
        String sort = form == null ? null : form.getSort();
        List<EditorDecision> filtered = editorDecisionRepository.findAllByOrderByDecidedAtDesc().stream()
                .filter(decision -> matchesFilters(decision.getStatement(), form))
                .toList();

        // Default/date sort needs no vote/comment counts.
        if ("new".equals(sort) || sort == null || sort.isBlank()) {
            return filtered.stream()
                    .sorted(Comparator.comparing(EditorDecision::getDecidedAt).reversed())
                    .toList();
        }

        // Precompute the counts once, then order the decisions by their statement.
        List<Long> ids = filtered.stream().map(decision -> decision.getStatement().getId()).toList();
        Comparator<Statement> byStatement = statementComparator(sort,
                voteCountsForSort(sort, ids), commentCountsForSort(sort, ids));
        return filtered.stream()
                .sorted((first, second) -> byStatement.compare(first.getStatement(), second.getStatement()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllTopicNames() {
        return topicRepository.findAll().stream()
                .map(Topic::getName)
                .sorted()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> getAllTopics() {
        return topicRepository.findAll().stream()
                .sorted(Comparator.comparing(Topic::getName))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Statement> getAvailableClaims() {
        return statementRepository.findByStatusAndClaimedByIsNullOrderByCreatedAtAsc(StatementStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Statement> getMyClaimedTasks(User editor) {
        return statementRepository.findByClaimedByAndStatusOrderByClaimedAtAsc(editor, StatementStatus.UNDER_REVIEW);
    }

    /**
     * Claims a statement after loading it with a pessimistic write lock.
     * Validation is performed after acquiring the lock so a competing claim
     * sees the current committed owner/status and receives a semantic conflict.
     *
     * @param id the statement id
     * @param editor the editor claiming the statement
     * @return the claimed statement
     */
    @Override
    public Statement claimStatement(Long id, User editor) {
        Statement statement = statementRepository.findByIdForClaim(id)
                .orElseThrow(() -> new StatementNotFoundException("Statement not found with id: " + id));

        // The claim path loads this row with a pessimistic write lock, so the
        // status/owner checks are evaluated against the committed current row.
        if (statement.getClaimedBy() != null) {
            throw new StatementAlreadyClaimedException("האמירה כבר שובצה לעורך אחר.");
        }
        if (statement.getStatus() != StatementStatus.PENDING) {
            throw new InvalidStatementStateException("ניתן לשבץ רק אמירות שממתינות לטיפול.");
        }

        statement.claim(editor);
        Statement saved = statementRepository.save(statement);
        userActivityLogService.log(editor, UserActivityType.CLAIMED, saved, saved.getId());
        return saved;
    }

    /**
     * Releases an under-review statement only when it is still assigned to the
     * requesting editor, then logs the editor activity.
     *
     * @param id the statement id
     * @param editor the editor releasing the statement
     */
    @Override
    public void releaseStatement(Long id, User editor) {
        Statement statement = statementRepository.findById(id)
                .orElseThrow(() -> new StatementNotFoundException("Statement not found with id: " + id));

        // Race-safe guard: only the owning editor may release a task that is still under review.
        // This rejects releasing a DECIDED statement or one already released/claimed by someone else.
        if (statement.getStatus() != StatementStatus.UNDER_REVIEW
                || statement.getClaimedBy() == null
                || !statement.getClaimedBy().getId().equals(editor.getId())) {
            throw new UnauthorizedEditorActionException("לא ניתן לשחרר משימה שאינה משויכת אליך.");
        }

        statement.release();
        Statement saved = statementRepository.save(statement);
        userActivityLogService.log(editor, UserActivityType.RELEASED, saved, saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Statement> findById(Long id) {
        return statementRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Statement> search(StatementSearchForm form) {
        if (form != null && hasText(form.getKeyword())) {
            return statementRepository.findByTextContainingIgnoreCase(form.getKeyword().trim());
        }

        if (form != null && form.getStatus() != null) {
            return statementRepository.findByStatusOrderByCreatedAtDesc(form.getStatus());
        }

        if (form != null && hasText(form.getTopic())) {
            return statementRepository.findByTopics_NameIgnoreCase(form.getTopic().trim());
        }

        return getPendingStatements();
    }

    /**
     * Creates a pending statement with validated topic ids and an existing
     * speaker resolved through the speaker service.
     *
     * @param form the editor statement form
     * @return the saved pending statement
     */
    @Override
    public Statement createPendingStatement(EditorStatementForm form) {
        Set<Long> topicIds = form.getTopicIds() == null ? Set.of() : form.getTopicIds();
        List<Topic> topics = topicRepository.findAllByIdIn(topicIds);
        if (topics.size() != topicIds.size()) {
            throw new InvalidTopicSelectionException("אחד או יותר מהנושאים שנבחרו אינם קיימים.");
        }

        Speaker speaker = speakerService.getById(form.getSpeakerId());
        Statement statement = Statement.createPending(
                form.getText().trim(),
                speaker,
                sanitizeUrl(form.getSourceUrl()),
                new HashSet<>(topics)
        );
        return statementRepository.save(statement);
    }

    /**
     * Applies the admin filters in memory over all statements. A numeric keyword
     * matches only the statement with that exact id; a text keyword matches the
     * statement text or speaker name; speaker/topic/status narrow further, and
     * the sort key orders the surviving statements.
     *
     * @param keyword free-text keyword or exact statement id, may be blank
     * @param speakerId speaker filter, or {@code null} for any speaker
     * @param topic topic-name filter, may be blank
     * @param status status filter, or {@code null} for any status
     * @param sort sort key
     * @return the filtered and sorted statements
     */
    @Override
    @Transactional(readOnly = true)
    public List<Statement> getManagedStatements(String keyword, Long speakerId, String topic,
                                                StatementStatus status, String sort) {
        Long exactId = parseIdOrNull(keyword);
        String textNeedle = exactId == null && hasText(keyword) ? keyword.trim().toLowerCase() : null;
        String topicNeedle = hasText(topic) ? topic.trim() : null;

        return statementRepository.findAllBy().stream()
                .filter(statement -> exactId == null || statement.getId().equals(exactId))
                .filter(statement -> textNeedle == null || matchesText(statement, textNeedle))
                .filter(statement -> speakerId == null
                        || (statement.getSpeaker() != null && statement.getSpeaker().getId().equals(speakerId)))
                .filter(statement -> topicNeedle == null
                        || statement.getTopics().stream().anyMatch(t -> t.getName().equalsIgnoreCase(topicNeedle)))
                .filter(statement -> status == null || statement.getStatus() == status)
                .sorted(managedComparator(sort))
                .toList();
    }

    /**
     * Edits every mutable statement field through the cohesive domain method
     * after validating the selected topics and resolving the speaker.
     *
     * @param id the statement id
     * @param form the replacement fields
     * @return the saved statement
     */
    @Override
    public Statement updateStatement(Long id, EditorStatementForm form) {
        Statement statement = statementRepository.findById(id)
                .orElseThrow(() -> new StatementNotFoundException("Statement not found with id: " + id));

        Set<Long> topicIds = form.getTopicIds() == null ? Set.of() : form.getTopicIds();
        List<Topic> topics = topicRepository.findAllByIdIn(topicIds);
        if (topics.size() != topicIds.size()) {
            throw new InvalidTopicSelectionException("אחד או יותר מהנושאים שנבחרו אינם קיימים.");
        }

        Speaker speaker = speakerService.getById(form.getSpeakerId());
        statement.editDetails(
                form.getText().trim(),
                speaker,
                sanitizeUrl(form.getSourceUrl()),
                new HashSet<>(topics)
        );
        return statementRepository.save(statement);
    }

    /**
     * Deletes a statement after the guard passes, first removing its owned rows
     * (comment reactions, comments, and activity logs) so no foreign key blocks
     * the removal; the topic-assignment join rows are removed with the entity.
     *
     * @param id the statement id
     */
    @Override
    public void deleteStatement(Long id) {
        Statement statement = statementRepository.findById(id)
                .orElseThrow(() -> new StatementNotFoundException("Statement not found with id: " + id));

        // A statement is only removable while it is still untouched: no votes,
        // no editor decision, and not claimed/under review.
        if (statement.getStatus() != StatementStatus.PENDING || statement.isClaimed()) {
            throw new StatementInUseException("לא ניתן למחוק אמירה שנמצאת בטיפול או שכבר הוכרעה.");
        }
        if (voteRepository.countByStatement(statement) > 0) {
            throw new StatementInUseException("לא ניתן למחוק אמירה שכבר יש עליה הצבעות.");
        }
        if (editorDecisionRepository.existsByStatement(statement)) {
            throw new StatementInUseException("לא ניתן למחוק אמירה שכבר יש עליה הכרעת עורך.");
        }

        // Clear the statement's own owned rows before deleting it.
        commentVoteRepository.deleteByStatementId(id);
        commentRepository.deleteByStatementId(id);
        userActivityLogRepository.deleteByStatementId(id);
        statement.getTopics().clear();
        statementRepository.delete(statement);
    }

    private Long parseIdOrNull(String keyword) {
        if (!hasText(keyword)) {
            return null;
        }
        try {
            return Long.parseLong(keyword.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean matchesText(Statement statement, String needle) {
        boolean matchesText = statement.getText() != null
                && statement.getText().toLowerCase().contains(needle);
        String speakerName = statement.getSpeaker() != null
                ? statement.getSpeaker().getName()
                : statement.getSpeakerName();
        boolean matchesSpeaker = speakerName != null && speakerName.toLowerCase().contains(needle);
        return matchesText || matchesSpeaker;
    }

    private Comparator<Statement> managedComparator(String sort) {
        if ("old".equals(sort)) {
            return Comparator.comparing(Statement::getCreatedAt);
        }
        if ("id".equals(sort)) {
            return Comparator.comparing(Statement::getId);
        }
        if ("status".equals(sort)) {
            return Comparator.comparing((Statement statement) -> statement.getStatus().ordinal())
                    .thenComparing(Statement::getCreatedAt, Comparator.reverseOrder());
        }
        return Comparator.comparing(Statement::getCreatedAt).reversed();
    }

    private List<Statement> filterAndSortStatements(List<Statement> statements, StatementSearchForm form) {
        String sort = form == null ? null : form.getSort();
        List<Statement> filtered = statements.stream()
                .filter(statement -> matchesFilters(statement, form))
                .toList();
        // Precompute the counts once for the surviving ids, then sort by map lookup.
        List<Long> ids = filtered.stream().map(Statement::getId).toList();
        Map<Long, Long> voteCounts = voteCountsForSort(sort, ids);
        Map<Long, Long> commentCounts = commentCountsForSort(sort, ids);
        return filtered.stream()
                .sorted(statementComparator(sort, voteCounts, commentCounts))
                .toList();
    }

    private boolean matchesFilters(Statement statement, StatementSearchForm form) {
        if (form == null) {
            return true;
        }

        if (hasText(form.getKeyword())) {
            // Keyword matches the statement text OR the speaker name (case-insensitive, null-safe).
            String needle = form.getKeyword().trim().toLowerCase();
            boolean matchesText = statement.getText() != null
                    && statement.getText().toLowerCase().contains(needle);
            String speakerName = statement.getSpeaker() != null
                    ? statement.getSpeaker().getName()
                    : statement.getSpeakerName();
            boolean matchesSpeaker = speakerName != null
                    && speakerName.toLowerCase().contains(needle);
            if (!matchesText && !matchesSpeaker) {
                return false;
            }
        }

        List<String> selectedTopics = form.getTopics();
        return selectedTopics == null
                || selectedTopics.isEmpty()
                || statement.getTopics().stream().map(Topic::getName).anyMatch(selectedTopics::contains);
    }

    /**
     * Builds the statement comparator from precomputed count maps so sorting by
     * votes/comments does not issue a count query per comparison. Ordering
     * (including stable tie-breaks) is identical to the previous per-row version.
     */
    private Comparator<Statement> statementComparator(String sort, Map<Long, Long> voteCounts,
                                                      Map<Long, Long> commentCounts) {
        if ("votes".equals(sort)) {
            return Comparator.comparingLong((Statement statement) ->
                    voteCounts.getOrDefault(statement.getId(), 0L)).reversed();
        }
        if ("comments".equals(sort)) {
            return Comparator.comparingLong((Statement statement) ->
                    commentCounts.getOrDefault(statement.getId(), 0L)).reversed();
        }
        return Comparator.comparing(Statement::getCreatedAt).reversed();
    }

    /** Loads grouped vote counts once when the sort needs them, else an empty map. */
    private Map<Long, Long> voteCountsForSort(String sort, Collection<Long> statementIds) {
        if (!"votes".equals(sort) || statementIds.isEmpty()) {
            return Map.of();
        }
        return countsById(voteRepository.countGroupedByStatementIds(statementIds));
    }

    /** Loads grouped comment counts once when the sort needs them, else an empty map. */
    private Map<Long, Long> commentCountsForSort(String sort, Collection<Long> statementIds) {
        if (!"comments".equals(sort) || statementIds.isEmpty()) {
            return Map.of();
        }
        return countsById(commentRepository.countGroupedByStatementIds(statementIds));
    }

    /** Turns grouped (statementId, count) rows into a map keyed by statement id. */
    private Map<Long, Long> countsById(List<Object[]> rows) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Normalizes an optional source URL, rejecting non-http/https schemes. */
    private String sanitizeUrl(String value) {
        try {
            return UrlValidator.sanitizeOptional(value);
        } catch (IllegalArgumentException ex) {
            throw new InvalidUrlException("יש להזין כתובת מקור בפורמט http או https בלבד.");
        }
    }
}
