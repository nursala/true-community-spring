package com.truecommunity.session;

import com.truecommunity.model.StatementStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;

/**
 * Session-scoped state: the admin statements page search/filter/sort selection.
 * Persists across requests for the duration of the HTTP session so filters
 * survive navigating away and back. The keyword doubles as an exact statement-id
 * lookup when it is a plain number. A blank keyword and null filters mean
 * "show all statements".
 */
@Component
@SessionScope
public class AdminStatementSearch implements Serializable {

    private static final long serialVersionUID = 1L;

    // Free-text keyword; also treated as an exact statement id when numeric.
    private String keyword = "";

    // Related-field filters.
    private Long speakerId;

    private String topic = "";

    private StatementStatus status;

    // Sort key: "new" (default), "old", "id", "status".
    private String sort = "new";

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword == null ? "" : keyword.trim();
    }

    public Long getSpeakerId() {
        return speakerId;
    }

    public void setSpeakerId(Long speakerId) {
        this.speakerId = speakerId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic == null ? "" : topic.trim();
    }

    public StatementStatus getStatus() {
        return status;
    }

    public void setStatus(StatementStatus status) {
        this.status = status;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort == null || sort.isBlank() ? "new" : sort.trim();
    }

    /**
     * Resets every filter and the keyword while restoring the default sort.
     */
    public void clear() {
        this.keyword = "";
        this.speakerId = null;
        this.topic = "";
        this.status = null;
        this.sort = "new";
    }

    /**
     * Reports whether any search/filter (not the sort) is currently applied,
     * used to decide whether to show the clear-filters control.
     *
     * @return {@code true} when a keyword or a related filter is active
     */
    public boolean isActive() {
        return !keyword.isBlank() || speakerId != null || !topic.isBlank() || status != null;
    }
}
