package com.truecommunity.services;

import com.truecommunity.dto.ReviewItem;
import com.truecommunity.model.Statement;
import com.truecommunity.model.User;
import com.truecommunity.model.UserActivityLog;
import com.truecommunity.model.UserActivityType;

import java.util.List;
import java.util.Map;

/**
 * Records and reads user activity shown in dashboards and editor task panels.
 */
public interface UserActivityLogService {

    /**
     * Persists a single user activity entry.
     *
     * @param user the actor
     * @param actionType the activity type
     * @param statement the related statement
     * @param targetId the id of the specific target record, when applicable
     */
    void log(User user, UserActivityType actionType, Statement statement, Long targetId);

    List<UserActivityLog> getRecentActivity();

    /**
     * For each of the editor's tasks, the editor-only activity (claim/decide/release) of that
     * statement, keyed by statement id. Community actions (votes/comments/evidence) are excluded.
     */
    Map<Long, List<UserActivityLog>> getEditorTaskActivity(List<ReviewItem> tasks);
}
