package com.truecommunity.dto;

import com.truecommunity.model.Verdict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Service/REST request object for publishing an editor decision.
 */
public class EditorDecisionRequest {

    @NotNull
    private Verdict finalVerdict;

    // Topics the editor assigns to the statement when publishing. Optional on the
    // REST contract; the MVC EditorDecisionForm enforces "at least one".
    private Set<Long> topicIds = new LinkedHashSet<>();

    @Size(max = 2000)
    private String notes;

    @Size(max = 500)
    private String finalEvidenceUrl;

    public EditorDecisionRequest() {
    }

    public Verdict getFinalVerdict() {
        return finalVerdict;
    }

    public void setFinalVerdict(Verdict finalVerdict) {
        this.finalVerdict = finalVerdict;
    }

    public Set<Long> getTopicIds() {
        return topicIds;
    }

    public void setTopicIds(Set<Long> topicIds) {
        this.topicIds = topicIds;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getFinalEvidenceUrl() {
        return finalEvidenceUrl;
    }

    public void setFinalEvidenceUrl(String finalEvidenceUrl) {
        this.finalEvidenceUrl = finalEvidenceUrl;
    }
}
