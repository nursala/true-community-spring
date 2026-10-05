package com.truecommunity.services;

import com.truecommunity.dto.LeaderboardResponse;
import com.truecommunity.model.User;

public interface LeaderboardService {

    /**
     * Builds the enriched, ranked leaderboard for the SPA.
     *
     * @param sort        ranking metric: "trust" (default), "votes", or "accuracy"
     * @param scope       "top" (default, first 10) or "all"
     * @param currentUser the authenticated viewer, or null when anonymous
     */
    LeaderboardResponse build(String sort, String scope, User currentUser);
}
