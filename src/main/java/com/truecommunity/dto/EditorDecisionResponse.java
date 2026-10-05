package com.truecommunity.dto;

import com.truecommunity.model.Verdict;

import java.time.LocalDateTime;

/**
 * JSON-safe representation of a published editor decision.
 */
public class EditorDecisionResponse {

    private Long id;

    private Long statementId;

    private String editorUsername;

    private Verdict finalVerdict;

    private String notes;

    private LocalDateTime decidedAt;

    public EditorDecisionResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStatementId() {
        return statementId;
    }

    public void setStatementId(Long statementId) {
        this.statementId = statementId;
    }

    public String getEditorUsername() {
        return editorUsername;
    }

    public void setEditorUsername(String editorUsername) {
        this.editorUsername = editorUsername;
    }

    public Verdict getFinalVerdict() {
        return finalVerdict;
    }

    public void setFinalVerdict(Verdict finalVerdict) {
        this.finalVerdict = finalVerdict;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }
}
