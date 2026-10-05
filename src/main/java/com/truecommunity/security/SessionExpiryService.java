package com.truecommunity.security;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Expires the active HTTP sessions of a given user so their next request is
 * forced to re-authenticate and pick up fresh authorities. Used after a role
 * change (promote/demote) so the effect is immediate instead of waiting for the
 * user to log out and back in.
 */
@Service
public class SessionExpiryService {

    private final SessionRegistry sessionRegistry;

    public SessionExpiryService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * Marks all active sessions for the supplied email as expired. The user is
     * redirected through Spring Security's expired-session handling on the next request.
     *
     * @param email the account email stored in the security principal
     */
    public void expireUserSessions(String email) {
        if (email == null) {
            return;
        }
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UserDetails userDetails && email.equals(userDetails.getUsername())) {
                for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                    session.expireNow();
                }
            }
        }
    }
}
