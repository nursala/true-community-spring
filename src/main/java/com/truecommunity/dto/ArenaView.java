package com.truecommunity.dto;

import com.truecommunity.model.Statement;
import com.truecommunity.model.Topic;
import com.truecommunity.model.Verdict;

import java.util.List;

/** Read-model for the voting arena page, assembled by the service layer. */
public class ArenaView {

    private final ConsensusResponse consensus;
    private final Verdict leadingVerdict;
    private final int leadingPercent;
    private final List<CommentView> comments;
    private final List<Statement> related;
    private final List<Topic> voteTopics;
    private final boolean canViewDiscussion;

    public ArenaView(ConsensusResponse consensus, Verdict leadingVerdict, int leadingPercent,
                     List<CommentView> comments, List<Statement> related, List<Topic> voteTopics,
                     boolean canViewDiscussion) {
        this.consensus = consensus;
        this.leadingVerdict = leadingVerdict;
        this.leadingPercent = leadingPercent;
        this.comments = comments;
        this.related = related;
        this.voteTopics = voteTopics;
        this.canViewDiscussion = canViewDiscussion;
    }

    public ConsensusResponse getConsensus() { return consensus; }
    public Verdict getLeadingVerdict() { return leadingVerdict; }
    public int getLeadingPercent() { return leadingPercent; }
    public List<CommentView> getComments() { return comments; }
    public List<Statement> getRelated() { return related; }
    public List<Topic> getVoteTopics() { return voteTopics; }
    public boolean isCanViewDiscussion() { return canViewDiscussion; }
}
