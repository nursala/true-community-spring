package com.truecommunity.controllers;

import com.truecommunity.dto.CommentRequest;
import com.truecommunity.services.CommentService;
import com.truecommunity.services.SseChannels;
import com.truecommunity.services.SseEvents;
import com.truecommunity.services.SseEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for comment read/create operations under statement API routes.
 */
@RestController
@RequestMapping("/api/statements")
public class CommentController {

    private final CommentService commentService;

    private final SseEventService sseEventService;

    public CommentController(CommentService commentService, SseEventService sseEventService) {
        this.commentService = commentService;
        this.sseEventService = sseEventService;
    }

    /**
     * Returns comments visible to the current API viewer.
     *
     * @param statementId statement id
     * @param authentication current authentication
     * @return visible comments as JSON
     */
    @GetMapping("/{statementId}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long statementId, Authentication authentication) {
        return ResponseEntity.ok(commentService.getCommentsForViewer(statementId, authentication));
    }

    /**
     * Creates a comment and publishes {@code COMMENT_ADDED} after persistence succeeds.
     *
     * @param statementId statement id
     * @param request comment payload
     * @param authentication current authentication
     * @return created comment response
     */
    @PostMapping("/{statementId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long statementId,
                                        @Valid @RequestBody CommentRequest request,
                                        Authentication authentication) {
        Object comment = commentService.addComment(statementId, request, authentication);
        sseEventService.publish(SseChannels.statement(statementId), SseEvents.COMMENT_ADDED,
                Map.of("statementId", statementId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comment);
    }
}
