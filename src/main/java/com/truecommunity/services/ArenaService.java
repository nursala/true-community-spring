package com.truecommunity.services;

import com.truecommunity.dto.ArenaView;
import com.truecommunity.model.User;

/**
 * Assembles the voting arena read-model (consensus, leading verdict, comment
 * views and related statements), keeping that work out of the controller.
 */
public interface ArenaService {

    /** Builds the arena view-model. {@code currentUser} may be null for anonymous visitors. */
    ArenaView buildArenaView(Long statementId, User currentUser);
}
