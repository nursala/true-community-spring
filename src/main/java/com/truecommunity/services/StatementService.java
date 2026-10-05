package com.truecommunity.services;

import com.truecommunity.dto.EditorStatementForm;
import com.truecommunity.dto.StatementSearchForm;
import com.truecommunity.model.EditorDecision;
import com.truecommunity.model.Statement;
import com.truecommunity.model.StatementStatus;
import com.truecommunity.model.Topic;
import com.truecommunity.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Coordinates statement browsing, search, creation, and editor claim/release
 * workflow operations.
 */
public interface StatementService {

    List<Statement> getPendingStatements();

    List<Statement> getAvailableForVoting(User user, StatementSearchForm form);

    List<Statement> getPendingVerificationStatements(User user, StatementSearchForm form);

    List<EditorDecision> getResolvedStatements(StatementSearchForm form);

    List<String> getAllTopicNames();

    List<Topic> getAllTopics();

    List<Statement> getAvailableClaims();

    List<Statement> getMyClaimedTasks(User editor);

    /**
     * Assigns a pending statement to the supplied editor.
     *
     * @param id the statement id
     * @param editor the editor claiming the statement
     * @return the claimed statement
     */
    Statement claimStatement(Long id, User editor);

    /**
     * Releases a statement currently assigned to the supplied editor.
     *
     * @param id the statement id
     * @param editor the editor releasing the statement
     */
    void releaseStatement(Long id, User editor);

    Optional<Statement> findById(Long id);

    List<Statement> search(StatementSearchForm form);

    /**
     * Creates a new pending statement from the editor-submitted form.
     *
     * @param form the statement text, speaker, source, and topic selections
     * @return the persisted pending statement
     */
    Statement createPendingStatement(EditorStatementForm form);

    /**
     * Lists statements for the admin management page, applying the active
     * keyword/id, speaker, topic, and status filters and the sort order entirely
     * server-side. A numeric keyword is treated as an exact statement-id lookup.
     *
     * @param keyword free-text keyword or exact statement id, may be blank
     * @param speakerId speaker filter, or {@code null} for any speaker
     * @param topic topic-name filter, may be blank
     * @param status status filter, or {@code null} for any status
     * @param sort sort key: {@code new}, {@code old}, {@code id}, or {@code status}
     * @return the filtered and sorted statements
     */
    List<Statement> getManagedStatements(String keyword, Long speakerId, String topic,
                                         StatementStatus status, String sort);

    /**
     * Edits all mutable fields of a statement at any status through the cohesive
     * {@code Statement.editDetails} domain method, keeping the speaker-name
     * snapshot and validating topic selections.
     *
     * @param id the statement id
     * @param form the replacement text, speaker, source, and topic selections
     * @return the saved statement
     */
    Statement updateStatement(Long id, EditorStatementForm form);

    /**
     * Deletes a statement only when it has no votes, no editor decision, and is
     * not claimed/under review, cleaning up the statement's own owned rows
     * (comments, comment reactions, activity logs, and topic assignments).
     *
     * @param id the statement id
     */
    void deleteStatement(Long id);
}
