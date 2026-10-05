package com.truecommunity.repo;

import com.truecommunity.model.EditorDecision;
import com.truecommunity.model.Statement;
import com.truecommunity.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for final editor decisions and decision-history
 * projections.
 */
public interface EditorDecisionRepository extends JpaRepository<EditorDecision, Long> {

    Optional<EditorDecision> findByStatement(Statement statement);

    boolean existsByStatement(Statement statement);

    /**
     * Loads all decisions for the given statement ids in one query, used to batch
     * the dashboard history lookup instead of one query per vote.
     *
     * @param statementIds the statement ids to load decisions for
     * @return decisions whose statement id is in the set
     */
    List<EditorDecision> findByStatement_IdIn(Collection<Long> statementIds);

    @EntityGraph(attributePaths = {"statement", "statement.topics", "statement.speaker", "editor"})
    List<EditorDecision> findByEditorOrderByDecidedAtDesc(User editor);

    @EntityGraph(attributePaths = {"statement", "statement.topics", "statement.speaker"})
    List<EditorDecision> findAllByOrderByDecidedAtDesc();

    /**
     * Returns compact statement/verdict tuples for leaderboard accuracy
     * aggregation without loading full decisions.
     *
     * @return rows shaped as {@code [statementId, finalVerdict]}
     */
    @Query("SELECT d.statement.id, d.finalVerdict FROM EditorDecision d")
    List<Object[]> findStatementFinalVerdicts();
}
