package com.truecommunity.services;

import com.truecommunity.model.Statement;
import com.truecommunity.model.Verdict;

import java.util.List;

/**
 * Applies and reverts trust-score changes that follow editor final decisions.
 */
public interface TrustScoreService {

    /**
     * Applies score changes for all votes on a decided statement.
     *
     * @param statement the decided statement
     * @param finalVerdict the editor's final verdict
     * @return ids of voters whose score changed
     */
    List<Long> updateTrustScoresForDecision(Statement statement, Verdict finalVerdict);

    /**
     * Reverts score changes previously applied for a statement decision.
     *
     * @param statement the statement whose decision is being reverted
     * @return ids of voters whose score changed
     */
    List<Long> revertTrustScoresForDecision(Statement statement);
}
