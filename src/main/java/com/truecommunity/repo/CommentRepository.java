package com.truecommunity.repo;

import com.truecommunity.dto.CommentInsight;
import com.truecommunity.model.Comment;
import com.truecommunity.model.Statement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Persistence operations for statement discussion comments and comment-derived
 * editor insights.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByStatementOrderByCreatedAtDesc(Statement statement);

    long countByStatement(Statement statement);

    /**
     * Removes every comment on a statement. Used by the guarded admin statement
     * delete after the comment reactions have been cleared.
     *
     * @param statementId the owning statement id
     */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.statement.id = :statementId")
    void deleteByStatementId(@Param("statementId") Long statementId);

    /**
     * Counts comments for many statement cards in one grouped query to avoid
     * per-card count queries in the feed.
     *
     * @param ids statement ids to count
     * @return rows shaped as {@code [statementId, count]}
     */
    @Query("SELECT c.statement.id, COUNT(c) FROM Comment c WHERE c.statement.id IN :ids GROUP BY c.statement.id")
    List<Object[]> countGroupedByStatementIds(@Param("ids") Collection<Long> ids);

    /**
     * Finds the highest-scoring comments for editor review. Net score is
     * calculated as upvotes minus downvotes.
     *
     * @param statementId the statement id
     * @param pageable page size limiting the number of insights
     * @return comment insight rows ordered by score descending
     */
    @Query("""
            SELECT new com.truecommunity.dto.CommentInsight(
                comment.id, comment.user.username, comment.content, comment.evidenceUrl, comment.createdAt,
                COALESCE(SUM(CASE
                    WHEN commentVote.voteType = com.truecommunity.model.CommentVoteType.UPVOTE THEN 1
                    WHEN commentVote.voteType = com.truecommunity.model.CommentVoteType.DOWNVOTE THEN -1
                    ELSE 0
                END), 0)
            )
            FROM Comment comment
            LEFT JOIN CommentVote commentVote ON commentVote.comment = comment
            WHERE comment.statement.id = :statementId
            GROUP BY comment.id, comment.user.username, comment.content, comment.evidenceUrl, comment.createdAt
            ORDER BY COALESCE(SUM(CASE
                WHEN commentVote.voteType = com.truecommunity.model.CommentVoteType.UPVOTE THEN 1
                WHEN commentVote.voteType = com.truecommunity.model.CommentVoteType.DOWNVOTE THEN -1
                ELSE 0
            END), 0) DESC, comment.createdAt DESC
            """)
    List<CommentInsight> findTopCommentsByStatementId(@Param("statementId") Long statementId, Pageable pageable);

    /**
     * Finds the lowest-scoring comments for editor review. Net score is
     * calculated as upvotes minus downvotes.
     *
     * @param statementId the statement id
     * @param pageable page size limiting the number of insights
     * @return comment insight rows ordered by score ascending
     */
    @Query("""
            SELECT new com.truecommunity.dto.CommentInsight(
                comment.id, comment.user.username, comment.content, comment.evidenceUrl, comment.createdAt,
                COALESCE(SUM(CASE
                    WHEN commentVote.voteType = com.truecommunity.model.CommentVoteType.UPVOTE THEN 1
                    WHEN commentVote.voteType = com.truecommunity.model.CommentVoteType.DOWNVOTE THEN -1
                    ELSE 0
                END), 0)
            )
            FROM Comment comment
            LEFT JOIN CommentVote commentVote ON commentVote.comment = comment
            WHERE comment.statement.id = :statementId
            GROUP BY comment.id, comment.user.username, comment.content, comment.evidenceUrl, comment.createdAt
            ORDER BY COALESCE(SUM(CASE
                WHEN commentVote.voteType = com.truecommunity.model.CommentVoteType.UPVOTE THEN 1
                WHEN commentVote.voteType = com.truecommunity.model.CommentVoteType.DOWNVOTE THEN -1
                ELSE 0
            END), 0) ASC, comment.createdAt ASC
            """)
    List<CommentInsight> findBottomCommentsByStatementId(@Param("statementId") Long statementId, Pageable pageable);
}
