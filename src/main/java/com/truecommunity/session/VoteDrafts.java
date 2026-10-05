package com.truecommunity.session;

import com.truecommunity.dto.VoteForm;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Session-scoped draft store for vote forms. Drafts are keyed by statement id,
 * contain only simple form values, and are cleared when the vote succeeds or
 * the HTTP session ends.
 */
@Component
@SessionScope
public class VoteDrafts implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<Long, VoteForm> drafts = new LinkedHashMap<>();

    /**
     * Returns a defensive copy of the saved draft for the statement or a blank form.
     *
     * @param statementId the statement being voted on
     * @return a detached vote form instance
     */
    public VoteForm getOrCreate(Long statementId) {
        VoteForm draft = drafts.get(statementId);
        return draft == null ? new VoteForm() : copyOf(draft);
    }

    /**
     * Saves a defensive copy of the vote form under the statement id.
     *
     * @param statementId the statement being voted on
     * @param form the current form values
     */
    public void save(Long statementId, VoteForm form) {
        drafts.put(statementId, copyOf(form));
    }

    /**
     * Removes the draft for one statement after successful voting or manual clearing.
     *
     * @param statementId the statement whose draft should be cleared
     */
    public void clear(Long statementId) {
        drafts.remove(statementId);
    }

    private VoteForm copyOf(VoteForm source) {
        VoteForm copy = new VoteForm();
        copy.setVerdict(source.getVerdict());
        copy.setSuggestedTopicId(source.getSuggestedTopicId());
        copy.setCommentText(source.getCommentText());
        copy.setEvidenceUrl(source.getEvidenceUrl());
        return copy;
    }
}
