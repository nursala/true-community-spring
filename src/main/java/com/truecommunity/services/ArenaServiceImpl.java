package com.truecommunity.services;

import com.truecommunity.dto.ArenaView;
import com.truecommunity.dto.CommentResponse;
import com.truecommunity.dto.CommentView;
import com.truecommunity.dto.ConsensusResponse;
import com.truecommunity.model.CommentVoteType;
import com.truecommunity.model.Role;
import com.truecommunity.model.Statement;
import com.truecommunity.model.User;
import com.truecommunity.model.Verdict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Read-only arena view assembler. It combines statement-related voting,
 * consensus, comment, reaction, topic, and related-statement data while
 * suppressing discussion and aggregate data until the viewer is eligible.
 */
@Service
@Transactional(readOnly = true)
public class ArenaServiceImpl implements ArenaService {

    private static final int RELATED_LIMIT = 4;

    private final StatementService statementService;
    private final VoteService voteService;
    private final CommentService commentService;
    private final CommentVoteService commentVoteService;

    public ArenaServiceImpl(StatementService statementService,
                            VoteService voteService,
                            CommentService commentService,
                            CommentVoteService commentVoteService) {
        this.statementService = statementService;
        this.voteService = voteService;
        this.commentService = commentService;
        this.commentVoteService = commentVoteService;
    }

    /**
     * Builds the arena page model for a statement. Normal users see discussion
     * data only after voting; editors may view it directly.
     *
     * @param statementId the statement being opened
     * @param currentUser the viewer, or {@code null} for anonymous access
     * @return the assembled arena view
     */
    @Override
    public ArenaView buildArenaView(Long statementId, User currentUser) {
        boolean canViewDiscussion = canViewDiscussion(statementId, currentUser);
        ConsensusResponse consensus = canViewDiscussion
                ? voteService.getConsensusForViewer(statementId, currentUser)
                : null;
        var leading = consensus == null ? null : consensus.leadingVerdict();

        return new ArenaView(
                consensus,
                leading,
                leadingPercent(consensus, leading),
                canViewDiscussion ? commentViews(statementId, currentUser) : List.of(),
                relatedStatements(statementId),
                statementService.getAllTopics(),
                canViewDiscussion
        );
    }

    private boolean canViewDiscussion(Long statementId, User currentUser) {
        if (currentUser == null) {
            return false;
        }
        return currentUser.getRole() == Role.EDITOR
                || voteService.hasUserAlreadyVoted(statementId, currentUser.getId());
    }

    private List<CommentView> commentViews(Long statementId, User currentUser) {
        List<CommentResponse> comments = commentService.getComments(statementId);
        List<Long> commentIds = comments.stream().map(CommentResponse::getCommentId).toList();
        // Batch the two per-comment reaction lookups into one grouped query each.
        Map<Long, Long> netScores = commentVoteService.getNetScores(commentIds);
        Map<Long, CommentVoteType> myVotes = commentVoteService.getUserVotes(commentIds, currentUser);
        return comments.stream()
                .map(comment -> new CommentView(
                        comment.getCommentId(),
                        comment.getAuthor(),
                        comment.getContent(),
                        comment.getEvidenceUrl(),
                        comment.getCreatedAt(),
                        netScores.getOrDefault(comment.getCommentId(), 0L),
                        myVotes.get(comment.getCommentId())
                ))
                .toList();
    }

    private List<Statement> relatedStatements(Long currentId) {
        return statementService.getPendingStatements().stream()
                .filter(statement -> !statement.getId().equals(currentId))
                .limit(RELATED_LIMIT)
                .toList();
    }

    private int leadingPercent(ConsensusResponse consensus, Verdict leading) {
        if (leading == null) {
            return 0;
        }
        Double pct = consensus.getPercentagesByVerdict().get(leading);
        return pct == null ? 0 : (int) Math.round(pct);
    }
}
