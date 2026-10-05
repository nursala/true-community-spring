package com.truecommunity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Final editor decision for a statement. The one-to-one statement relationship
 * is unique so a statement can have at most one published decision.
 */
@Entity
@Table(
        name = "editor_decisions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_editor_decisions_statement",
                columnNames = "statement_id"
        )
)
public class EditorDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "final_verdict", nullable = false, length = 30)
    private Verdict finalVerdict;

    @Size(max = 2000)
    @Column(length = 2000)
    private String notes;

    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt = LocalDateTime.now();

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "statement_id", nullable = false, unique = true)
    private Statement statement;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "editor_id", nullable = false)
    private User editor;

    protected EditorDecision() {
    }

    /**
     * Creates a final decision for a statement assigned to an editor.
     *
     * @param statement the decided statement
     * @param editor the deciding editor
     * @param finalVerdict the editor's final verdict
     * @param notes optional editor notes
     * @return the new decision entity
     */
    public static EditorDecision create(Statement statement, User editor, Verdict finalVerdict, String notes) {
        EditorDecision decision = new EditorDecision();
        decision.statement = statement;
        decision.editor = editor;
        decision.finalVerdict = finalVerdict;
        decision.notes = notes;
        return decision;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Statement getStatement() {
        return statement;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    public User getEditor() {
        return editor;
    }

    public void setEditor(User editor) {
        this.editor = editor;
    }
}
