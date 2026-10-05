package com.truecommunity.dto;

import com.truecommunity.model.Verdict;

import java.time.LocalDateTime;

/**
 * One row of a user's voting history, including the trust-score impact once
 * an editor has published a final verdict for the statement.
 */
public class HistoryRow {

    private final Long statementId;
    private final String statementText;
    private final Verdict userVerdict;
    private final Verdict editorVerdict;
    private final Double scoreChange;
    private final String scoreReason;
    private final LocalDateTime votedAt;
    private final HistoryOutcome outcome;

    public HistoryRow(Long statementId, String statementText, Verdict userVerdict,
                      Verdict editorVerdict, Double scoreChange, String scoreReason,
                      LocalDateTime votedAt) {
        this.statementId = statementId;
        this.statementText = statementText;
        this.userVerdict = userVerdict;
        this.editorVerdict = editorVerdict;
        this.scoreChange = scoreChange;
        this.scoreReason = scoreReason;
        this.votedAt = votedAt;
        this.outcome = computeOutcome(userVerdict, editorVerdict);
    }

    private static HistoryOutcome computeOutcome(Verdict userVerdict, Verdict editorVerdict) {
        if (editorVerdict == null) {
            return HistoryOutcome.UNDECIDED;
        }
        return editorVerdict == userVerdict ? HistoryOutcome.MATCHED : HistoryOutcome.MISMATCHED;
    }

    public Long getStatementId() {
        return statementId;
    }

    public String getStatementText() {
        return statementText;
    }

    public Verdict getUserVerdict() {
        return userVerdict;
    }

    public Verdict getEditorVerdict() {
        return editorVerdict;
    }

    public Double getScoreChange() {
        return scoreChange;
    }

    public String getScoreReason() {
        return scoreReason;
    }

    public LocalDateTime getVotedAt() {
        return votedAt;
    }

    public HistoryOutcome getOutcome() {
        return outcome;
    }
}
