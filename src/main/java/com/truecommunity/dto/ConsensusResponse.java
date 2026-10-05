package com.truecommunity.dto;

import com.truecommunity.model.Verdict;

import java.util.Map;

/**
 * Vote aggregate response containing total votes, verdict counts, percentages,
 * and the derived leading verdict.
 */
public class ConsensusResponse {

    private Long statementId;

    private long totalVotes;

    private Map<Verdict, Long> countsByVerdict;

    private Map<Verdict, Double> percentagesByVerdict;

    public ConsensusResponse() {
    }

    public Long getStatementId() {
        return statementId;
    }

    public void setStatementId(Long statementId) {
        this.statementId = statementId;
    }

    public long getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(long totalVotes) {
        this.totalVotes = totalVotes;
    }

    public Map<Verdict, Long> getCountsByVerdict() {
        return countsByVerdict;
    }

    public void setCountsByVerdict(Map<Verdict, Long> countsByVerdict) {
        this.countsByVerdict = countsByVerdict;
    }

    public Map<Verdict, Double> getPercentagesByVerdict() {
        return percentagesByVerdict;
    }

    public void setPercentagesByVerdict(Map<Verdict, Double> percentagesByVerdict) {
        this.percentagesByVerdict = percentagesByVerdict;
    }

    /**
     * Returns the verdict with the highest count, or {@code null} when there
     * are no votes.
     *
     * @return the leading verdict, if any
     */
    public Verdict leadingVerdict() {
        if (totalVotes == 0) {
            return null;
        }
        return countsByVerdict.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
