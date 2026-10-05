package com.truecommunity.session;

import com.truecommunity.dto.EditorDecisionForm;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Session-scoped draft store for editor decision forms. Drafts are keyed by
 * statement id, store only form values, and disappear when the HTTP session is
 * destroyed.
 */
@Component
@SessionScope
public class EditorDecisionDrafts implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<Long, EditorDecisionForm> drafts = new LinkedHashMap<>();

    /**
     * Returns a defensive copy of the saved draft for the statement or a blank form.
     *
     * @param statementId the statement being edited
     * @return a detached form instance for rendering
     */
    public EditorDecisionForm getOrCreate(Long statementId) {
        EditorDecisionForm draft = drafts.get(statementId);
        return draft == null ? new EditorDecisionForm() : copyOf(draft);
    }

    /**
     * Saves a defensive copy of the form under the statement id.
     *
     * @param statementId the statement being edited
     * @param form the current form values
     */
    public void save(Long statementId, EditorDecisionForm form) {
        drafts.put(statementId, copyOf(form));
    }

    /**
     * Removes the draft for one statement after successful publication or manual clearing.
     *
     * @param statementId the statement whose draft should be cleared
     */
    public void clear(Long statementId) {
        drafts.remove(statementId);
    }

    private EditorDecisionForm copyOf(EditorDecisionForm source) {
        EditorDecisionForm copy = new EditorDecisionForm();
        copy.setFinalVerdict(source.getFinalVerdict());
        copy.setTopicIds(source.getTopicIds() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(source.getTopicIds()));
        copy.setNotes(source.getNotes());
        copy.setFinalEvidenceUrl(source.getFinalEvidenceUrl());
        return copy;
    }
}
