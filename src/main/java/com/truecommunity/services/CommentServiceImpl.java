package com.truecommunity.services;

import com.truecommunity.dto.CommentInsight;
import com.truecommunity.dto.CommentRequest;
import com.truecommunity.dto.CommentResponse;
import com.truecommunity.exception.StatementNotFoundException;
import com.truecommunity.exception.UnauthorizedDiscussionAccessException;
import com.truecommunity.exception.VotingClosedException;
import com.truecommunity.model.Comment;
import com.truecommunity.model.Role;
import com.truecommunity.model.Statement;
import com.truecommunity.model.StatementStatus;
import com.truecommunity.model.User;
import com.truecommunity.model.UserActivityType;
import com.truecommunity.repo.CommentRepository;
import com.truecommunity.repo.StatementRepository;
import com.truecommunity.repo.VoteRepository;
import com.truecommunity.security.AuthenticatedUserProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Transactional discussion service. It rejects comments after a final decision,
 * requires normal users to vote before reading or joining the discussion, and
 * allows editors to view discussion content for moderation/review work.
 */
@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private static final int INSIGHT_LIMIT = 5;

    private final CommentRepository commentRepository;
    private final StatementRepository statementRepository;
    private final VoteRepository voteRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final UserActivityLogService userActivityLogService;

    public CommentServiceImpl(CommentRepository commentRepository,
                              StatementRepository statementRepository,
                              VoteRepository voteRepository,
                              AuthenticatedUserProvider authenticatedUserProvider,
                              UserActivityLogService userActivityLogService) {
        this.commentRepository = commentRepository;
        this.statementRepository = statementRepository;
        this.voteRepository = voteRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.userActivityLogService = userActivityLogService;
    }

    /**
     * Persists a comment for the authenticated user after validating that the
     * statement exists, is not decided, and the user may participate in the
     * discussion. The activity log entry is written in the same transaction.
     *
     * @param statementId the statement receiving the comment
     * @param request the submitted comment content
     * @param authentication the current authentication
     * @return the saved comment response
     */
    @Override
    public CommentResponse addComment(Long statementId, CommentRequest request, Authentication authentication) {
        User user = authenticatedUserProvider.getAuthenticatedUser(authentication);
        Statement statement = findStatementOrThrow(statementId);
        if (statement.getStatus() == StatementStatus.DECIDED) {
            throw new VotingClosedException("הדיון ננעל לאחר פרסום ההכרעה הסופית.");
        }
        requireDiscussionAccess(statement, user);
        Comment savedComment = commentRepository.save(Comment.create(user, statement, request.getContent()));
        userActivityLogService.log(user, UserActivityType.COMMENTED, statement, savedComment.getId());
        return toCommentResponse(savedComment);
    }

    /**
     * Returns comments for internal callers that already made the discussion
     * visibility decision, ordered newest first.
     *
     * @param statementId the statement whose comments are requested
     * @return comments for the statement
     */
    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long statementId) {
        Statement statement = findStatementOrThrow(statementId);
        return commentRepository.findByStatementOrderByCreatedAtDesc(statement).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    /**
     * Enforces the discussion-access rule before returning comments to an API viewer.
     *
     * @param statementId the statement whose comments are requested
     * @param authentication the current authentication
     * @return comments visible to the viewer
     */
    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsForViewer(Long statementId, Authentication authentication) {
        Statement statement = findStatementOrThrow(statementId);
        User user = currentUserOrNull(authentication);
        requireDiscussionAccess(statement, user);
        return commentRepository.findByStatementOrderByCreatedAtDesc(statement).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentInsight> getTop5Comments(Long statementId) {
        return commentRepository.findTopCommentsByStatementId(statementId, PageRequest.of(0, INSIGHT_LIMIT));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentInsight> getBottom5Comments(Long statementId) {
        return commentRepository.findBottomCommentsByStatementId(statementId, PageRequest.of(0, INSIGHT_LIMIT));
    }

    private Statement findStatementOrThrow(Long statementId) {
        return statementRepository.findById(statementId)
                .orElseThrow(() -> new StatementNotFoundException("Statement not found with id: " + statementId));
    }

    private User currentUserOrNull(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authenticatedUserProvider.getAuthenticatedUser(authentication);
    }

    private void requireDiscussionAccess(Statement statement, User user) {
        if (user == null
                || (user.getRole() != Role.EDITOR && !voteRepository.existsByUserAndStatement(user, statement))) {
            throw new UnauthorizedDiscussionAccessException(
                    "יש להצביע על האמירה לפני הצטרפות או צפייה בדיון.");
        }
    }

    private CommentResponse toCommentResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setCommentId(comment.getId());
        response.setStatementId(comment.getStatement().getId());
        response.setAuthor(comment.getUser().getUsername());
        response.setContent(comment.getContent());
        response.setEvidenceUrl(comment.getEvidenceUrl());
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }
}
