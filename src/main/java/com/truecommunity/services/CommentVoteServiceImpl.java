package com.truecommunity.services;

import com.truecommunity.exception.CommentNotFoundException;
import com.truecommunity.exception.UnauthorizedDiscussionAccessException;
import com.truecommunity.exception.VotingClosedException;
import com.truecommunity.model.Comment;
import com.truecommunity.model.CommentVote;
import com.truecommunity.model.CommentVoteType;
import com.truecommunity.model.Role;
import com.truecommunity.model.Statement;
import com.truecommunity.model.StatementStatus;
import com.truecommunity.model.User;
import com.truecommunity.repo.CommentRepository;
import com.truecommunity.repo.CommentVoteRepository;
import com.truecommunity.repo.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Transactional implementation of comment reactions. It checks discussion
 * access before mutating any reaction, rejects decided statements, toggles the
 * same reaction off, and switches between upvote/downvote when requested.
 */
@Service
@Transactional
public class CommentVoteServiceImpl implements CommentVoteService {

    private final CommentVoteRepository commentVoteRepository;

    private final CommentRepository commentRepository;

    private final VoteRepository voteRepository;

    public CommentVoteServiceImpl(CommentVoteRepository commentVoteRepository,
                                  CommentRepository commentRepository,
                                  VoteRepository voteRepository) {
        this.commentVoteRepository = commentVoteRepository;
        this.commentRepository = commentRepository;
        this.voteRepository = voteRepository;
    }

    /**
     * Mutates a comment reaction only after loading the comment, validating its
     * statement, and confirming the user can access the discussion. Authorization
     * failure happens before create/update/delete operations.
     *
     * @param commentId the comment being reacted to
     * @param user the authenticated user
     * @param voteType the requested reaction type
     * @return the statement id used by callers for redirects and SSE payloads
     */
    @Override
    public Long castVote(Long commentId, User user, CommentVoteType voteType) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found with id: " + commentId));
        Statement statement = comment.getStatement();

        if (statement == null) {
            throw new CommentNotFoundException("Comment not found with id: " + commentId);
        }
        if (statement.getStatus() == StatementStatus.DECIDED) {
            throw new VotingClosedException("לא ניתן להצביע על תגובות לאחר פרסום ההכרעה הסופית.");
        }
        requireDiscussionAccess(statement, user);

        Optional<CommentVote> existing = commentVoteRepository.findByCommentAndUser(comment, user);
        if (existing.isPresent()) {
            CommentVote vote = existing.get();
            if (vote.getVoteType() == voteType) {
                commentVoteRepository.delete(vote); // clicking the same arrow again clears the vote
            } else {
                vote.setVoteType(voteType); // switch up <-> down
                commentVoteRepository.save(vote);
            }
        } else {
            commentVoteRepository.save(CommentVote.create(comment, user, voteType));
        }

        return statement.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public long getNetScore(Long commentId) {
        long up = commentVoteRepository.countByComment_IdAndVoteType(commentId, CommentVoteType.UPVOTE);
        long down = commentVoteRepository.countByComment_IdAndVoteType(commentId, CommentVoteType.DOWNVOTE);
        return up - down;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getNetScores(Collection<Long> commentIds) {
        Map<Long, Long> netScores = new HashMap<>();
        if (commentIds == null || commentIds.isEmpty()) {
            return netScores;
        }
        // Guarantee an entry (0) for every requested comment, then overlay the grouped totals.
        for (Long commentId : commentIds) {
            netScores.put(commentId, 0L);
        }
        for (Object[] row : commentVoteRepository.netScoresByCommentIds(commentIds)) {
            netScores.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return netScores;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, CommentVoteType> getUserVotes(Collection<Long> commentIds, User user) {
        Map<Long, CommentVoteType> votes = new HashMap<>();
        if (user == null || commentIds == null || commentIds.isEmpty()) {
            return votes;
        }
        for (CommentVote vote : commentVoteRepository.findByComment_IdInAndUser(commentIds, user)) {
            votes.put(vote.getComment().getId(), vote.getVoteType());
        }
        return votes;
    }

    private void requireDiscussionAccess(Statement statement, User user) {
        if (user == null
                || (user.getRole() != Role.EDITOR && !voteRepository.existsByUserAndStatement(user, statement))) {
            throw new UnauthorizedDiscussionAccessException(
                    "יש להצביע על האמירה לפני הצטרפות או צפייה בדיון.");
        }
    }
}
