package com.truecommunity.controllers;

import com.truecommunity.dto.EditorDecisionRequest;
import com.truecommunity.dto.EditorDecisionResponse;
import com.truecommunity.model.EditorDecision;
import com.truecommunity.services.EditorDecisionService;
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
 * REST controller for editor decision publication and lookup.
 */
@RestController
@RequestMapping("/api/editor")
public class EditorDecisionController {

    private final EditorDecisionService editorDecisionService;

    private final SseEventService sseEventService;

    public EditorDecisionController(EditorDecisionService editorDecisionService, SseEventService sseEventService) {
        this.editorDecisionService = editorDecisionService;
        this.sseEventService = sseEventService;
    }

    /**
     * Publishes a final decision through the REST API and emits public/editor
     * SSE events after the service call succeeds.
     *
     * @param statementId statement id
     * @param request decision payload
     * @param authentication current editor authentication
     * @return created decision response
     */
    @PostMapping("/statements/{statementId}/decision")
    public ResponseEntity<EditorDecisionResponse> publishDecision(@PathVariable Long statementId,
                                                                  @Valid @RequestBody EditorDecisionRequest request,
                                                                  Authentication authentication) {
        EditorDecision decision = editorDecisionService.publishDecision(statementId, request, authentication);
        sseEventService.publish(SseChannels.PUBLIC_FEED, SseEvents.DECISION_PUBLISHED, Map.of("statementId", statementId));
        sseEventService.publish(SseChannels.EDITOR_DESK, SseEvents.DECISION_PUBLISHED, Map.of("statementId", statementId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toEditorDecisionResponse(decision));
    }

    /**
     * Returns the final editor decision for a statement when one exists.
     *
     * @param statementId statement id
     * @return decision response or {@code 404 Not Found}
     */
    @GetMapping("/statements/{statementId}/decision")
    public ResponseEntity<EditorDecisionResponse> getDecision(@PathVariable Long statementId) {
        return editorDecisionService.findByStatement(statementId)
                .map(this::toEditorDecisionResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private EditorDecisionResponse toEditorDecisionResponse(EditorDecision decision) {
        EditorDecisionResponse response = new EditorDecisionResponse();
        response.setId(decision.getId());
        response.setStatementId(decision.getStatement().getId());
        response.setEditorUsername(decision.getEditor().getUsername());
        response.setFinalVerdict(decision.getFinalVerdict());
        response.setNotes(decision.getNotes());
        response.setDecidedAt(decision.getDecidedAt());
        return response;
    }
}
