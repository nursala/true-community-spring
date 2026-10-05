package com.truecommunity.dto;

import com.truecommunity.model.StatementStatus;
import com.truecommunity.model.Verdict;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Search and filter form shared by feed and statement browsing endpoints.
 */
public class StatementSearchForm {

    @Size(max = 200)
    private String keyword;

    @Size(max = 100)
    private String topic;

    private StatementStatus status;

    private String sort;

    // Multi-select filters used by the discovery feed sidebar.
    private List<String> topics = new ArrayList<>();

    private List<Verdict> verdicts = new ArrayList<>();

    public StatementSearchForm() {
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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
        this.sort = sort;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public List<Verdict> getVerdicts() {
        return verdicts;
    }

    public void setVerdicts(List<Verdict> verdicts) {
        this.verdicts = verdicts;
    }
}
