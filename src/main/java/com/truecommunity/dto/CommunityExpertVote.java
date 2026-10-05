package com.truecommunity.dto;

import com.truecommunity.model.Verdict;

/** A reasoned vote submitted by a community member with a high trust score. */
public class CommunityExpertVote {

    private final String username;
    private final Double trustScore;
    private final Verdict verdict;
    private final String explanation;
    private final String evidenceUrl;

    public CommunityExpertVote(String username, Double trustScore, Verdict verdict, String explanation, String evidenceUrl) {
        this.username = username;
        this.trustScore = trustScore;
        this.verdict = verdict;
        this.explanation = explanation;
        this.evidenceUrl = evidenceUrl;
    }

    public String getUsername() { return username; }
    public Double getTrustScore() { return trustScore; }
    public Verdict getVerdict() { return verdict; }
    public String getExplanation() { return explanation; }
    public String getEvidenceUrl() { return evidenceUrl; }
}
