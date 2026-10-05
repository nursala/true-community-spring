package com.truecommunity.dto;

import java.util.List;

/** Aggregated data for the user dashboard page. */
public class DashboardView {

    private final String username;
    private final String email;
    private final String role;
    private final double trustScore;
    private final int totalVotes;
    private final int decidedCount;
    private final int matchCount;
    private final int accuracyPercent;
    private final List<HistoryRow> history;
    // Editor-only panel; null for regular users.
    private final EditorStatsView editorStats;

    public DashboardView(String username, String email, String role, double trustScore,
                         int totalVotes, int decidedCount, int matchCount, int accuracyPercent,
                         List<HistoryRow> history, EditorStatsView editorStats) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.trustScore = trustScore;
        this.totalVotes = totalVotes;
        this.decidedCount = decidedCount;
        this.matchCount = matchCount;
        this.accuracyPercent = accuracyPercent;
        this.history = history;
        this.editorStats = editorStats;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public double getTrustScore() {
        return trustScore;
    }

    public int getTotalVotes() {
        return totalVotes;
    }

    public int getDecidedCount() {
        return decidedCount;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public int getAccuracyPercent() {
        return accuracyPercent;
    }

    public List<HistoryRow> getHistory() {
        return history;
    }

    public EditorStatsView getEditorStats() {
        return editorStats;
    }
}
