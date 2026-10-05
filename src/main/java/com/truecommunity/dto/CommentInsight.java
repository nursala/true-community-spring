package com.truecommunity.dto;

import java.time.LocalDateTime;

/** A statement comment enriched with its community net-vote score. */
public class CommentInsight {

    private final Long commentId;
    private final String author;
    private final String content;
    private final String evidenceUrl;
    private final LocalDateTime createdAt;
    private final Long netScore;

    public CommentInsight(Long commentId, String author, String content, String evidenceUrl, LocalDateTime createdAt, Long netScore) {
        this.commentId = commentId;
        this.author = author;
        this.content = content;
        this.evidenceUrl = evidenceUrl;
        this.createdAt = createdAt;
        this.netScore = netScore;
    }

    public Long getCommentId() { return commentId; }
    public String getAuthor() { return author; }
    public String getContent() { return content; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getNetScore() { return netScore; }
}
