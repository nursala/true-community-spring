package com.truecommunity.dto;

import com.truecommunity.model.Verdict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * MVC form for statement voting. The same fields are stored in the
 * per-statement vote draft session bean.
 */
public class VoteForm implements Serializable {

    private static final long serialVersionUID = 1L;

    // Verdict (required).
    @NotNull
    private Verdict verdict;

    // Crowdsourced classification: the voter suggests one topic (required).
    @NotNull
    private Long suggestedTopicId;

    // Optional free-text comment, published as a public Comment when present.
    @Size(max = 1000)
    private String commentText;

    @Size(max = 500)
    private String evidenceUrl;

    public VoteForm() {
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

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }
}
