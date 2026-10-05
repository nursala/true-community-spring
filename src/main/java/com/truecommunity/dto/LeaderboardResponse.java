package com.truecommunity.dto;

import java.util.List;

/** SPA payload for the leaderboard: the active view, the ranked entries, and the viewer. */
public class LeaderboardResponse {

    private final String sort;
    private final String scope;
    private final List<LeaderboardUserResponse> entries;
    // The authenticated, non-editor viewer (null when anonymous or an editor).
    private final LeaderboardUserResponse me;

    public LeaderboardResponse(String sort, String scope, List<LeaderboardUserResponse> entries,
                               LeaderboardUserResponse me) {
        this.sort = sort;
        this.scope = scope;
        this.entries = entries;
        this.me = me;
    }

    public String getSort() {
        return sort;
    }

    public String getScope() {
        return scope;
    }

    public List<LeaderboardUserResponse> getEntries() {
        return entries;
    }

    public LeaderboardUserResponse getMe() {
        return me;
    }
}
