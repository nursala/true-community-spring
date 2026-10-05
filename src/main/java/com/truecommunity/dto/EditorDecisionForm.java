package com.truecommunity.dto;

import com.truecommunity.model.Verdict;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * MVC form used by the editor workspace to publish a final decision. The same
 * fields are stored in the per-statement session draft.
 */
public class EditorDecisionForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "יש לבחור פסיקה סופית.")
    private Verdict finalVerdict;

    // The editor classifies the statement (at least one topic) when publishing the verdict.
    @NotEmpty(message = "יש לבחור לפחות נושא אחד.")
    private Set<Long> topicIds = new LinkedHashSet<>();

    @Size(max = 2000)
    private String notes;

    @Size(max = 500)
    private String finalEvidenceUrl;

    public EditorDecisionForm() {
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
