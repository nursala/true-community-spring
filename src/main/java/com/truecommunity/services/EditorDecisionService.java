package com.truecommunity.services;

import com.truecommunity.dto.EditorDecisionRequest;
import com.truecommunity.model.EditorDecision;
import com.truecommunity.model.User;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

/**
 * Defines editor decision operations for publishing, finding, listing, and
 * reverting final statement decisions.
 */
public interface EditorDecisionService {

    /**
     * Publishes a final decision for a statement assigned to the authenticated editor.
     *
     * @param statementId the statement being decided
     * @param request the final verdict, topic, notes, and evidence fields
     * @param authentication the current editor authentication
     * @return the saved editor decision
     */
    EditorDecision publishDecision(Long statementId, EditorDecisionRequest request, Authentication authentication);

    Optional<EditorDecision> findByStatement(Long statementId);

    List<EditorDecision> getDecisionsByEditor(User editor);

    /**
     * Reverts a decision previously published by the authenticated editor.
     *
     * @param statementId the statement whose decision should be reverted
     * @param authentication the current editor authentication
     */
    void revertDecision(Long statementId, Authentication authentication);
}
