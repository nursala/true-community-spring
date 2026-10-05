package com.truecommunity.services;

import com.truecommunity.dto.CommentInsight;
import com.truecommunity.dto.CommentRequest;
import com.truecommunity.dto.CommentResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Defines comment creation and retrieval rules for statement discussions.
 * Viewer-aware retrieval protects comments until the viewer is allowed to
 * participate in the discussion.
 */
public interface CommentService {

    /**
     * Adds a comment for the authenticated viewer when the discussion is open.
     *
     * @param statementId the statement receiving the comment
     * @param request the submitted comment content
     * @param authentication the current authentication
     * @return the saved comment response
     */
    CommentResponse addComment(Long statementId, CommentRequest request, Authentication authentication);

    List<CommentResponse> getComments(Long statementId);

    /**
     * Returns comments only when the viewer satisfies the discussion-access rule.
     *
     * @param statementId the statement whose comments are requested
     * @param authentication the current authentication
     * @return comments visible to the viewer
     */
    List<CommentResponse> getCommentsForViewer(Long statementId, Authentication authentication);

    List<CommentInsight> getTop5Comments(Long statementId);

    List<CommentInsight> getBottom5Comments(Long statementId);
}
