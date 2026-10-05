package com.truecommunity.dto;

import com.truecommunity.model.CommentVoteType;

import java.time.LocalDateTime;

/** A comment enriched with its net vote score and the current user's vote. */
public class CommentView {

    private final Long commentId;
    private final String author;
    private final String content;
    private final String evidenceUrl;
    private final LocalDateTime createdAt;
    private final long netScore;
    private final CommentVoteType myVote;

    public CommentView(Long commentId, String author, String content, String evidenceUrl, LocalDateTime createdAt,
                       long netScore, CommentVoteType myVote) {
        this.commentId = commentId;
        this.author = author;
        this.content = content;
        this.evidenceUrl = evidenceUrl;
        this.createdAt = createdAt;
        this.netScore = netScore;
        this.myVote = myVote;
    }

    public Long getCommentId() {
        return commentId;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public long getNetScore() {
        return netScore;
    }

    public CommentVoteType getMyVote() {
        return myVote;
    }
}
