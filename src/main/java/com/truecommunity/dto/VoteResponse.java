package com.truecommunity.dto;

import com.truecommunity.model.Verdict;

import java.time.LocalDateTime;

/**
 * JSON-safe response describing a saved vote without exposing unrelated entity state.
 */
public class VoteResponse {

    private Long voteId;

    private Long statementId;

    private Long userId;

    private Verdict verdict;

    private String evidenceUrl;

    private Long suggestedTopicId;

    private String suggestedTopicName;

    private LocalDateTime createdAt;

    public VoteResponse() {
    }

    public Long getSuggestedTopicId() {
        return suggestedTopicId;
    }

    public void setSuggestedTopicId(Long suggestedTopicId) {
        this.suggestedTopicId = suggestedTopicId;
    }

    public String getSuggestedTopicName() {
        return suggestedTopicName;
    }

    public void setSuggestedTopicName(String suggestedTopicName) {
        this.suggestedTopicName = suggestedTopicName;
    }

    public Long getVoteId() {
        return voteId;
    }

    public void setVoteId(Long voteId) {
        this.voteId = voteId;
    }

    public Long getStatementId() {
        return statementId;
    }

    public void setStatementId(Long statementId) {
        this.statementId = statementId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
