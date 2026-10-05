package com.truecommunity.dto;

import com.truecommunity.model.EditorDecision;
import com.truecommunity.model.Statement;

import java.util.List;
import java.util.Map;

/** Read-model for the discovery feed page, assembled by the service layer. */
public class FeedView {

    private final List<Statement> availableItems;
    private final List<Statement> pendingItems;
    private final Map<Long, ConsensusResponse> pendingConsensus;
    private final List<EditorDecision> resolvedItems;
    private final List<String> allTopics;
    // Precomputed vote/comment counts for the available cards, keyed by statement id (no N+1).
    private final Map<Long, Long> availableVoteCounts;
    private final Map<Long, Long> availableCommentCounts;

    public FeedView(List<Statement> availableItems, List<Statement> pendingItems,
                    Map<Long, ConsensusResponse> pendingConsensus, List<EditorDecision> resolvedItems,
                    List<String> allTopics, Map<Long, Long> availableVoteCounts,
                    Map<Long, Long> availableCommentCounts) {
        this.availableItems = availableItems;
        this.pendingItems = pendingItems;
        this.pendingConsensus = pendingConsensus;
        this.resolvedItems = resolvedItems;
        this.allTopics = allTopics;
        this.availableVoteCounts = availableVoteCounts;
        this.availableCommentCounts = availableCommentCounts;
    }

    public List<Statement> getAvailableItems() { return availableItems; }
    public List<Statement> getPendingItems() { return pendingItems; }
    public Map<Long, ConsensusResponse> getPendingConsensus() { return pendingConsensus; }
    public List<EditorDecision> getResolvedItems() { return resolvedItems; }
    public List<String> getAllTopics() { return allTopics; }
    public Map<Long, Long> getAvailableVoteCounts() { return availableVoteCounts; }
    public Map<Long, Long> getAvailableCommentCounts() { return availableCommentCounts; }
}
