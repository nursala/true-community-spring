package com.truecommunity.repo;

import com.truecommunity.model.Statement;
import com.truecommunity.model.StatementStatus;
import com.truecommunity.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for statement browsing, editor claiming, and feed
 * buckets.
 */
public interface StatementRepository extends JpaRepository<Statement, Long> {

    @EntityGraph(attributePaths = {"topics", "speaker", "claimedBy"})
    Optional<Statement> findById(Long id);

    /**
     * Loads a statement row with a pessimistic write lock for the claim workflow.
     * The query intentionally avoids collection joins so locking applies to the
     * statement row used for owner/status validation.
     *
     * @param id the statement id
     * @return the locked statement when present
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT statement FROM Statement statement WHERE statement.id = :id")
    Optional<Statement> findByIdForClaim(@Param("id") Long id);

    /**
     * Loads every statement with its topics, speaker, and claiming editor for
     * the admin management page, which filters and sorts in memory.
     *
     * @return all statements with associations eagerly fetched
     */
    @EntityGraph(attributePaths = {"topics", "speaker", "claimedBy"})
    List<Statement> findAllBy();

    @EntityGraph(attributePaths = {"topics", "speaker", "claimedBy"})
    List<Statement> findByStatus(StatementStatus status);

    @EntityGraph(attributePaths = {"topics", "speaker", "claimedBy"})
    List<Statement> findByTopics_NameIgnoreCase(String topicName);

    @EntityGraph(attributePaths = {"topics", "speaker", "claimedBy"})
    List<Statement> findByTextContainingIgnoreCase(String keyword);

    @EntityGraph(attributePaths = {"topics", "speaker", "claimedBy"})
    List<Statement> findByStatusOrderByCreatedAtDesc(StatementStatus status);

    /**
     * Finds pending statements still available to the viewer for voting,
     * excluding statements already voted on by that user.
     *
     * @param status the expected open status
     * @param user the current user, or {@code null} for anonymous browsing
     * @return available statements ordered newest first
     */
    @Query("""
            SELECT statement
            FROM Statement statement
            WHERE statement.status = :status
              AND (:user IS NULL OR NOT EXISTS (
                  SELECT vote
                  FROM Vote vote
                  WHERE vote.statement = statement AND vote.user = :user
              ))
            ORDER BY statement.createdAt DESC
            """)
    @EntityGraph(attributePaths = {"topics", "speaker", "claimedBy"})
    List<Statement> findAvailableForVoting(@Param("status") StatementStatus status,
                                            @Param("user") User user);

    /**
     * Finds statements the user already voted on that are still pending or under
     * editor review, used by the "pending verification" feed tab.
     *
     * @param user the voter
     * @param statuses statuses included in the pending-verification bucket
     * @return voted statements ordered newest first
     */
    @Query("""
            SELECT statement
            FROM Statement statement
            WHERE statement.status IN :statuses
              AND EXISTS (
                  SELECT vote
                  FROM Vote vote
                  WHERE vote.statement = statement AND vote.user = :user
              )
            ORDER BY statement.createdAt DESC
            """)
    @EntityGraph(attributePaths = {"topics", "speaker", "claimedBy"})
    List<Statement> findPendingStatementsVotedByUser(@Param("user") User user,
                                                       @Param("statuses") Collection<StatementStatus> statuses);

    /**
     * Finds unclaimed pending statements for the editor pending pool.
     *
     * @param status the pending status
     * @return claimable statements ordered oldest first
     */
    @EntityGraph(attributePaths = {"topics", "speaker", "claimedBy"})
    List<Statement> findByStatusAndClaimedByIsNullOrderByCreatedAtAsc(StatementStatus status);

    /**
     * Finds under-review tasks claimed by one editor for the workspace tab.
     *
     * @param claimedBy the editor who owns the tasks
     * @param status the under-review status
     * @return claimed tasks ordered by claim time
     */
    @EntityGraph(attributePaths = {"topics", "speaker", "claimedBy"})
    List<Statement> findByClaimedByAndStatusOrderByClaimedAtAsc(User claimedBy, StatementStatus status);

    @EntityGraph(attributePaths = {"topics", "speaker", "claimedBy"})
    List<Statement> findByClaimedByAndStatus(User claimedBy, StatementStatus status);

    boolean existsBySpeaker_Id(Long speakerId);
}
