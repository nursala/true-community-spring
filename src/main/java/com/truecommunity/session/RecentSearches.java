package com.truecommunity.session;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Session-scoped state: the current user's most recent keyword searches.
 * Lives for the duration of the HTTP session and is cleared on logout/expiry.
 * Most-recent-first, capped, with case-insensitive de-duplication.
 */
@Component
@SessionScope
public class RecentSearches implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MAX_ITEMS = 5;
    private final LinkedList<String> items = new LinkedList<>();

    /**
     * Adds a non-blank search term, moves duplicates to the front, and caps the list.
     *
     * @param term the search keyword
     */
    public void add(String term) {
        if (term == null || term.isBlank()) {
            return;
        }
        String trimmed = term.trim();
        items.removeIf(existing -> existing.equalsIgnoreCase(trimmed));
        items.addFirst(trimmed);
        while (items.size() > MAX_ITEMS) {
            items.removeLast();
        }
    }

    /**
     * Returns an immutable snapshot of recent search terms.
     *
     * @return most-recent-first search terms
     */
    public List<String> getItems() {
        return List.copyOf(items);
    }
}
