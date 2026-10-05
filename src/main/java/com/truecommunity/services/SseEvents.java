package com.truecommunity.services;

/**
 * Central definitions of the Server-Sent Events event names. These names are
 * sent on the wire and matched by the browser's {@code addEventListener} calls,
 * so the string values must remain byte-identical to the client listeners.
 */
public final class SseEvents {

    public static final String VOTE_SUBMITTED = "VOTE_SUBMITTED";

    public static final String DECISION_PUBLISHED = "DECISION_PUBLISHED";

    public static final String DECISION_REVERTED = "DECISION_REVERTED";

    public static final String STATEMENT_CREATED = "STATEMENT_CREATED";

    public static final String STATEMENT_CLAIMED = "STATEMENT_CLAIMED";

    public static final String TASK_RELEASED = "TASK_RELEASED";

    public static final String COMMENT_ADDED = "COMMENT_ADDED";

    public static final String COMMENT_VOTE_UPDATED = "COMMENT_VOTE_UPDATED";

    public static final String DASHBOARD_UPDATED = "DASHBOARD_UPDATED";

    public static final String SPEAKER_CREATED = "SPEAKER_CREATED";

    public static final String SPEAKER_UPDATED = "SPEAKER_UPDATED";

    public static final String SPEAKER_DELETED = "SPEAKER_DELETED";

    public static final String USER_PROMOTED = "USER_PROMOTED";

    public static final String USER_DEMOTED = "USER_DEMOTED";

    private SseEvents() {
    }
}
