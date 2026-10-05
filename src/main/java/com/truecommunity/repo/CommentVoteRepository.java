package com.truecommunity.repo;

import com.truecommunity.model.Comment;
import com.truecommunity.model.CommentVote;
import com.truecommunity.model.CommentVoteType;
import com.truecommunity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for one user's reaction to one comment.
 */
public interface CommentVoteRepository extends JpaRepository<CommentVote, Long> {

    Optional<CommentVote> findByCommentAndUser(Comment comment, User user);

    boolean existsByCommentAndUser(Comment comment, User user);

    long countByComment_IdAndVoteType(Long commentId, CommentVoteType voteType);

    Optional<CommentVote> findByComment_IdAndUser(Long commentId, User user);

    /**
     * Computes the net score (upvotes minus downvotes) for many comments in one
     * grouped query so an arena discussion can be built without a per-comment
     * count loop.
     *
     * @param commentIds the comment ids to score
     * @return rows shaped as {@code [commentId, netScore]}
     */
    @Query("SELECT cv.comment.id, "
            + "SUM(CASE WHEN cv.voteType = com.truecommunity.model.CommentVoteType.UPVOTE THEN 1 "
            + "WHEN cv.voteType = com.truecommunity.model.CommentVoteType.DOWNVOTE THEN -1 ELSE 0 END) "
            + "FROM CommentVote cv WHERE cv.comment.id IN :commentIds GROUP BY cv.comment.id")
    List<Object[]> netScoresByCommentIds(@Param("commentIds") Collection<Long> commentIds);

    /**
     * Loads one viewer's reactions across many comments in one query so the
     * per-comment "my vote" marker can be resolved without a query per comment.
     *
     * @param commentIds the comment ids to look up
     * @param user the viewing user
     * @return the viewer's reactions on those comments
     */
    List<CommentVote> findByComment_IdInAndUser(Collection<Long> commentIds, User user);

    /**
     * Removes every reaction on comments belonging to a statement. Used by the
     * guarded admin statement delete to clear owned rows before the comments and
     * the statement itself are removed.
     *
     * @param statementId the owning statement id
     */
    @Modifying
    @Query("DELETE FROM CommentVote cv WHERE cv.comment.statement.id = :statementId")
    void deleteByStatementId(@Param("statementId") Long statementId);
}
