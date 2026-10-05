package com.truecommunity.services;

import com.truecommunity.model.CommentVoteType;
import com.truecommunity.model.User;

import java.util.Collection;
import java.util.Map;

/**
 * Defines reaction operations for statement comments. Reaction mutations must
 * follow the same discussion-access rules as comment viewing and creation.
 */
public interface CommentVoteService {

    /**
     * Casts, switches, or clears the user's reaction on a comment.
     *
     * @param commentId the comment being reacted to
     * @param user the authenticated user
     * @param voteType the requested reaction type
     * @return the statement id the comment belongs to
     */
    Long castVote(Long commentId, User user, CommentVoteType voteType);

    /** Net score = upvotes - downvotes. */
    long getNetScore(Long commentId);

    /**
     * Net scores for many comments in one grouped query, keyed by comment id.
     * Comments with no reactions are returned with a score of {@code 0}.
     *
     * @param commentIds the comment ids to score
     * @return comment id to net score (every requested id present)
     */
    Map<Long, Long> getNetScores(Collection<Long> commentIds);

    /**
     * The viewer's reactions across many comments in one query, keyed by comment id.
     * Comments the viewer has not reacted to are absent from the map.
     *
     * @param commentIds the comment ids to look up
     * @param user the viewing user, or {@code null} for anonymous
     * @return comment id to the viewer's reaction type
     */
    Map<Long, CommentVoteType> getUserVotes(Collection<Long> commentIds, User user);
}
