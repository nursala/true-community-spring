package com.truecommunity.session;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Session-scoped state: statements the current user has recently viewed.
 * Lives for the duration of the HTTP session and is cleared on logout/expiry.
 */
@Component
@SessionScope
public class RecentlyViewedStatements implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MAX_ITEMS = 5;
    private final LinkedList<ViewedStatement> items = new LinkedList<>();

    /**
     * Adds or moves a statement to the front of the per-session recent list.
     *
     * @param id the viewed statement id
     * @param text the statement text shown in the recent list
     */
    public void add(Long id, String text) {
        items.removeIf(v -> v.getId().equals(id));
        items.addFirst(new ViewedStatement(id, text));
        while (items.size() > MAX_ITEMS) {
            items.removeLast();
        }
    }

    /**
     * Returns an immutable snapshot of the session's recent statements.
     *
     * @return most-recent-first viewed statements
     */
    public List<ViewedStatement> getItems() {
        return List.copyOf(items);
    }

    /**
     * Lightweight serializable recent-statement value stored in the HTTP session.
     */
    public static class ViewedStatement implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Long id;
        private final String text;

        public ViewedStatement(Long id, String text) {
            this.id = id;
            this.text = text;
        }

        public Long getId() {
            return id;
        }

        public String getText() {
            return text;
        }
    }
}
