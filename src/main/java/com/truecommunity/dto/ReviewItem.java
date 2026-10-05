package com.truecommunity.dto;

import com.truecommunity.model.Speaker;
import com.truecommunity.model.StatementStatus;
import com.truecommunity.model.Verdict;

import java.util.Map;
import java.util.Set;

/** A statement awaiting an editor decision, with its current consensus snapshot. */
public class ReviewItem {

    private final Long id;
    private final String text;
    private final Speaker speaker;
    private final String speakerName;
    private final String sourceUrl;
    private final StatementStatus status;
    private final Set<String> topics;
    private final long totalVotes;
    private final Verdict leadingVerdict;
    private final Map<Verdict, Integer> percentByVerdict;
    private final Map<Verdict, Long> countByVerdict;
    private final ConsensusResponse consensus;

    public ReviewItem(Long id, String text, Speaker speaker, String speakerName, String sourceUrl,
                      StatementStatus status, Set<String> topics, long totalVotes, Verdict leadingVerdict,
                      Map<Verdict, Integer> percentByVerdict, Map<Verdict, Long> countByVerdict,
                      ConsensusResponse consensus) {
        this.id = id;
        this.text = text;
        this.speaker = speaker;
        this.speakerName = speakerName;
        this.sourceUrl = sourceUrl;
        this.status = status;
        this.topics = topics;
        this.totalVotes = totalVotes;
        this.leadingVerdict = leadingVerdict;
        this.percentByVerdict = percentByVerdict;
        this.countByVerdict = countByVerdict;
        this.consensus = consensus;
    }

    public Long getId() { return id; }
    public String getText() { return text; }
    public Speaker getSpeaker() { return speaker; }
    public String getSpeakerName() { return speakerName; }
    public String getSourceUrl() { return sourceUrl; }
    public StatementStatus getStatus() { return status; }
    public Set<String> getTopics() { return topics; }
    public long getTotalVotes() { return totalVotes; }
    public Verdict getLeadingVerdict() { return leadingVerdict; }
    public Map<Verdict, Integer> getPercentByVerdict() { return percentByVerdict; }
    public Map<Verdict, Long> getCountByVerdict() { return countByVerdict; }
    public ConsensusResponse getConsensus() { return consensus; }
    public int getLeadingPercent() {
        if (leadingVerdict == null) {
            return 0;
        }
        Integer percentage = percentByVerdict.get(leadingVerdict);
        return percentage == null ? 0 : percentage;
    }
}