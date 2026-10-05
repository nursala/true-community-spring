package com.truecommunity.services;

import com.truecommunity.dto.CommunityExpertVote;
import com.truecommunity.dto.ConsensusResponse;
import com.truecommunity.dto.TopicConsensusResponse;
import com.truecommunity.dto.VoteRequest;
import com.truecommunity.dto.VoteResponse;
import com.truecommunity.model.Evidence;
import com.truecommunity.model.User;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Defines voting, vote-derived evidence, and consensus access operations.
 * Viewer-aware methods enforce the anti-bias rule that normal users may see
 * aggregate vote data only after voting on the statement; editors are allowed
 * to inspect those aggregates for review work.
 */
public interface VoteService {

    /**
     * Records the authenticated user's vote for a statement.
     *
     * @param statementId the statement being voted on
     * @param request the vote payload submitted by the user
     * @param authentication the current Spring Security authentication
     * @return the saved vote response
     */
    VoteResponse submitVote(Long statementId, VoteRequest request, Authentication authentication);

    /**
     * Returns raw consensus data for trusted internal/editor use.
     *
     * @param statementId the statement whose vote totals should be calculated
     * @return verdict counts and percentages for the statement
     */
    ConsensusResponse getConsensus(Long statementId);

    /**
     * Builds raw consensus for many statements in one grouped query, keyed by
     * statement id, for pages that already resolved viewer access for the whole
     * bucket (e.g. the feed's pending items and the editor desk). Every requested
     * id is present in the result, with all-zero counts when a statement has no
     * votes.
     *
     * @param statementIds the statement ids to build consensus for
     * @return an insertion-ordered map of statement id to consensus
     */
    Map<Long, ConsensusResponse> getConsensusByStatementIds(Collection<Long> statementIds);

    /**
     * Returns consensus only when the supplied viewer is allowed to see
     * vote-derived aggregate data.
     *
     * @param statementId the statement whose consensus is requested
     * @param user the viewer, or {@code null} for anonymous access
     * @return verdict counts and percentages visible to the viewer
     */
    ConsensusResponse getConsensusForViewer(Long statementId, User user);

    /**
     * Authentication-based variant of {@link #getConsensusForViewer(Long, User)}.
     *
     * @param statementId the statement whose consensus is requested
     * @param authentication the current Spring Security authentication
     * @return verdict counts and percentages visible to the viewer
     */
    ConsensusResponse getConsensusForViewer(Long statementId, Authentication authentication);

    /**
     * Returns topic suggestions derived from votes for internal/editor views.
     *
     * @param statementId the statement whose topic suggestions should be counted
     * @return topic names with vote counts
     */
    List<TopicConsensusResponse> getTopicConsensus(Long statementId);

    /**
     * Returns topic consensus only after the viewer passes the anti-bias access rule.
     *
     * @param statementId the statement whose topic suggestions should be counted
     * @param authentication the current Spring Security authentication
     * @return topic names with vote counts visible to the viewer
     */
    List<TopicConsensusResponse> getTopicConsensusForViewer(Long statementId, Authentication authentication);

    List<CommunityExpertVote> getTopTrustUserVotes(Long statementId);

    List<Evidence> getEvidenceForStatement(Long statementId);

    /**
     * Checks vote existence without loading all votes.
     *
     * @param statementId the statement id
     * @param userId the user id
     * @return {@code true} when the user has already voted on the statement
     */
    boolean hasUserAlreadyVoted(Long statementId, Long userId);
}
