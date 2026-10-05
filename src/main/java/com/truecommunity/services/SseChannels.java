package com.truecommunity.services;

/**
 * Central definitions of the Server-Sent Events channel names used across the
 * app. Channels are internal routing keys shared by publishers and subscribers;
 * the string values must stay stable so both sides continue to match.
 */
public final class SseChannels {

    /** Public discovery feed and leaderboard refreshes. */
    public static final String PUBLIC_FEED = "public-feed";

    /** Editor workspace / desk updates. */
    public static final String EDITOR_DESK = "editor-desk";

    /** Editor-only speaker management page. */
    public static final String ADMIN_SPEAKERS = "admin-speakers";

    /** Editor-only user management page. */
    public static final String ADMIN_USERS = "admin-users";

    private static final String STATEMENT_PREFIX = "statement:";

    private static final String USER_PREFIX = "user:";

    private SseChannels() {
    }

    /**
     * Builds the per-statement (arena) channel name.
     *
     * @param statementId the statement id
     * @return the channel name {@code statement:<id>}
     */
    public static String statement(Long statementId) {
        return STATEMENT_PREFIX + statementId;
    }

    /**
     * Builds the per-user (dashboard) channel name.
     *
     * @param userId the user id
     * @return the channel name {@code user:<id>}
     */
    public static String user(Long userId) {
        return USER_PREFIX + userId;
    }
}
