package com.truecommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * MVC form for the editor add-statement modal. Values are also stored in the
 * session-scoped add-statement draft.
 */
public class EditorStatementForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 10_000)
    private String text;

    @NotNull
    private Long speakerId;

    @Size(max = 500)
    private String sourceUrl;

    // Optional at creation: a new statement starts unclassified and the editor
    // assigns its topics when publishing the final verdict.
    private Set<Long> topicIds = new LinkedHashSet<>();

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getSpeakerId() {
        return speakerId;
    }

    public void setSpeakerId(Long speakerId) {
        this.speakerId = speakerId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Set<Long> getTopicIds() {
        return topicIds;
    }

    public void setTopicIds(Set<Long> topicIds) {
        this.topicIds = topicIds;
    }
}