package com.truecommunity.dto;

/**
 * Vote-derived count for one suggested topic.
 */
public class TopicConsensusResponse {

    private String topicName;

    private long voteCount;

    public TopicConsensusResponse() {
    }

    public TopicConsensusResponse(String topicName, long voteCount) {
        this.topicName = topicName;
        this.voteCount = voteCount;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public long getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(long voteCount) {
        this.voteCount = voteCount;
    }
}
