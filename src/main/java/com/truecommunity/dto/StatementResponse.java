package com.truecommunity.dto;

import com.truecommunity.model.StatementStatus;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JSON-safe public statement response used by statement API endpoints.
 */
public class StatementResponse {

    private Long id;

    private String text;

    private String speakerName;

    private String sourceUrl;

    private Set<String> topics = new HashSet<>();

    private StatementStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    public StatementResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSpeakerName() {
        return speakerName;
    }

    public void setSpeakerName(String speakerName) {
        this.speakerName = speakerName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Set<String> getTopics() {
        return topics;
    }

    public void setTopics(Set<String> topics) {
        this.topics = topics;
    }

    public StatementStatus getStatus() {
        return status;
    }

    public void setStatus(StatementStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
