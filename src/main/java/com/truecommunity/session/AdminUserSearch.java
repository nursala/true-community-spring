package com.truecommunity.session;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;

/**
 * Session-scoped state: the admin users page search filter (username or email).
 * Lives for the duration of the HTTP session and is cleared on logout/expiry.
 * A blank query means "show all users".
 */
@Component
@SessionScope
public class AdminUserSearch implements Serializable {

    private static final long serialVersionUID = 1L;

    // Blank means "no filter".
    private String query = "";

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query.trim();
    }

    public void clear() {
        this.query = "";
    }

    public boolean isActive() {
        return !query.isBlank();
    }
}
