package com.truecommunity.dto;

/** One enriched leaderboard entry (also reused for the viewer's own "me" row). */
public class LeaderboardUserResponse {

    private int rank;

    private Long id;

    private String username;

    private String role;

    private Double trustScore;

    private int totalVotes;

    private int accuracyPercent;

    private boolean currentUser;

    public LeaderboardUserResponse() {
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Double getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(Double trustScore) {
        this.trustScore = trustScore;
    }

    public int getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(int totalVotes) {
        this.totalVotes = totalVotes;
    }

    public int getAccuracyPercent() {
        return accuracyPercent;
    }

    public void setAccuracyPercent(int accuracyPercent) {
        this.accuracyPercent = accuracyPercent;
    }

    public boolean isCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(boolean currentUser) {
        this.currentUser = currentUser;
    }
}
