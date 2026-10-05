package com.truecommunity.controllers;

import com.truecommunity.dto.VoteRequest;
import com.truecommunity.services.VoteService;
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

/**
 * REST controller for vote submission and viewer-gated vote aggregate endpoints.
 */
@RestController
@RequestMapping("/api/statements")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    /**
     * Submits an authenticated vote for a statement.
     *
     * @param statementId statement id
     * @param request vote payload
     * @param authentication current authentication
     * @return created vote response
     */
    @PostMapping("/{statementId}/votes")
    public ResponseEntity<?> submitVote(@PathVariable Long statementId,
                                        @Valid @RequestBody VoteRequest request,
                                        Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(voteService.submitVote(statementId, request, authentication));
    }

    /**
     * Returns verdict consensus only when the viewer satisfies anti-bias access.
     *
     * @param statementId statement id
     * @param authentication current authentication
     * @return consensus response
     */
    @GetMapping("/{statementId}/consensus")
    public ResponseEntity<?> getConsensus(@PathVariable Long statementId,
                                          Authentication authentication) {
        return ResponseEntity.ok(voteService.getConsensusForViewer(statementId, authentication));
    }

    /**
     * Returns topic consensus only when the viewer satisfies anti-bias access.
     *
     * @param statementId statement id
     * @param authentication current authentication
     * @return topic consensus response
     */
    @GetMapping("/{statementId}/topic-consensus")
    public ResponseEntity<?> getTopicConsensus(@PathVariable Long statementId,
                                               Authentication authentication) {
        return ResponseEntity.ok(voteService.getTopicConsensusForViewer(statementId, authentication));
    }
}
