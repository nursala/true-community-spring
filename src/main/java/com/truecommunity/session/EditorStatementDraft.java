package com.truecommunity.session;

import com.truecommunity.dto.EditorStatementForm;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.LinkedHashSet;

/**
 * Session-scoped draft for the editor "add statement" modal. It stores one
 * form per HTTP session and is cleared after successful statement creation,
 * logout, expiry, or application restart.
 */
@Component
@SessionScope
public class EditorStatementDraft implements Serializable {

    private static final long serialVersionUID = 1L;

    private EditorStatementForm draft;

    /**
     * Returns a defensive copy of the saved draft or a blank form when none exists.
     *
     * @return a detached form instance for rendering
     */
    public EditorStatementForm getOrCreate() {
        return draft == null ? new EditorStatementForm() : copyOf(draft);
    }

    /**
     * Saves a defensive copy of the current add-statement form.
     *
     * @param form the current modal form values
     */
    public void save(EditorStatementForm form) {
        draft = copyOf(form);
    }

    /**
     * Clears the current session's add-statement draft.
     */
    public void clear() {
        draft = null;
    }

    private EditorStatementForm copyOf(EditorStatementForm source) {
        EditorStatementForm copy = new EditorStatementForm();
        copy.setText(source.getText());
        copy.setSpeakerId(source.getSpeakerId());
        copy.setSourceUrl(source.getSourceUrl());
        copy.setTopicIds(source.getTopicIds() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(source.getTopicIds()));
        return copy;
    }
}
