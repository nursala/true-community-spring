package com.truecommunity.repo;

import com.truecommunity.dto.CommunityExpertVote;
import com.truecommunity.model.Statement;
import com.truecommunity.model.User;
import com.truecommunity.model.Verdict;
import com.truecommunity.model.Vote;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Persistence operations for statement votes, vote existence checks, and
 * vote-derived aggregate projections.
 */
public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByUserAndStatement(User user, Statement statement);

    List<Vote> findByStatement(Statement statement);

    List<Vote> findByUser(User user);

    long countByStatement(Statement statement);

    long countByStatementAndVerdict(Statement statement, Verdict verdict);

    /**
     * Returns high-trust voter explanations for editor review.
     *
     * @param statementId the statement being reviewed
     * @param pageable page size limiting the number of rows
     * @return trusted vote summaries ordered by trust score
     */
    @Query("""
            SELECT new com.truecommunity.dto.CommunityExpertVote(
                vote.user.username, vote.user.trustScore, vote.verdict, vote.explanation, vote.evidenceUrl
            )
            FROM Vote vote
            WHERE vote.statement.id = :statementId
              AND vote.explanation IS NOT NULL
              AND TRIM(vote.explanation) <> ''
            ORDER BY vote.user.trustScore DESC, vote.createdAt DESC
            """)
    List<CommunityExpertVote> findTopTrustUserVotesByStatementId(@Param("statementId") Long statementId,
                                                                   Pageable pageable);

    /**
     * Counts voter-suggested topics for a statement.
     *
     * @param statementId the statement id
     * @return rows shaped as {@code [topicName, count]}
     */
    @Query("""
            SELECT v.suggestedTopic.name, COUNT(v)
            FROM Vote v
            WHERE v.statement.id = :statementId AND v.suggestedTopic IS NOT NULL
            GROUP BY v.suggestedTopic.name
            ORDER BY COUNT(v) DESC
            """)
    List<Object[]> countVotesPerTopicForStatement(@Param("statementId") Long statementId);

    /**
     * Counts votes for many statement cards in one grouped query to avoid
     * per-card count queries in the feed.
     *
     * @param ids statement ids to count
     * @return rows shaped as {@code [statementId, count]}
     */
    @Query("SELECT v.statement.id, COUNT(v) FROM Vote v WHERE v.statement.id IN :ids GROUP BY v.statement.id")
    List<Object[]> countGroupedByStatementIds(@Param("ids") Collection<Long> ids);

    /**
     * Counts votes per verdict for many statements in one grouped query so that
     * consensus for a whole feed/desk page can be built without a per-statement
     * count loop.
     *
     * @param ids statement ids to count
     * @return rows shaped as {@code [statementId, verdict, count]}
     */
    @Query("SELECT v.statement.id, v.verdict, COUNT(v) FROM Vote v WHERE v.statement.id IN :ids "
            + "GROUP BY v.statement.id, v.verdict")
    List<Object[]> countVerdictsGroupedByStatementIds(@Param("ids") Collection<Long> ids);

    /**
     * Returns compact vote tuples for one-pass leaderboard aggregation.
     *
     * @return rows shaped as {@code [userId, statementId, verdict]}
     */
    @Query("SELECT v.user.id, v.statement.id, v.verdict FROM Vote v")
    List<Object[]> findVoteAggregationTuples();
}
