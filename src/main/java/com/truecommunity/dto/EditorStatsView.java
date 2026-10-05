package com.truecommunity.dto;

import com.truecommunity.model.Verdict;

import java.util.Map;

/** Editor-only dashboard panel: decision count and per-verdict distribution. */
public class EditorStatsView {

    private final int totalDecisions;
    private final Map<Verdict, Integer> verdictDistribution;
    private final int maxCount;

    public EditorStatsView(int totalDecisions, Map<Verdict, Integer> verdictDistribution) {
        this.totalDecisions = totalDecisions;
        this.verdictDistribution = verdictDistribution;
        this.maxCount = verdictDistribution.values().stream().max(Integer::compareTo).orElse(0);
    }

    public int getTotalDecisions() {
        return totalDecisions;
    }

    public Map<Verdict, Integer> getVerdictDistribution() {
        return verdictDistribution;
    }

    /** Largest single-verdict count, used to scale the server-rendered bars. */
    public int getMaxCount() {
        return maxCount;
    }
}
