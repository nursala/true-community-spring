package com.truecommunity.dto;

import com.truecommunity.model.Verdict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JSON/service request object for submitting a statement vote.
 */
public class VoteRequest {

    @NotNull
    private Verdict verdict;

    // Crowdsourced classification: the topic suggested by the voter (required).
    @NotNull
    private Long suggestedTopicId;

    @Size(max = 1000)
    private String explanation;

    @Size(max = 500)
    private String evidenceUrl;

    @Size(max = 1000)
    private String evidenceDescription;

    public VoteRequest() {
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public Long getSuggestedTopicId() {
        return suggestedTopicId;
    }

    public void setSuggestedTopicId(Long suggestedTopicId) {
        this.suggestedTopicId = suggestedTopicId;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public String getEvidenceDescription() {
        return evidenceDescription;
    }

    public void setEvidenceDescription(String evidenceDescription) {
        this.evidenceDescription = evidenceDescription;
    }
}
