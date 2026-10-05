package com.truecommunity.services;

import com.truecommunity.dto.ReviewItem;
import com.truecommunity.model.Statement;
import com.truecommunity.model.User;
import com.truecommunity.model.UserActivityLog;
import com.truecommunity.model.UserActivityType;
import com.truecommunity.repo.StatementRepository;
import com.truecommunity.repo.UserActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transactional activity-log implementation. It stores user actions and builds
 * the editor-task activity map from editor-only workflow events.
 */
@Service
@Transactional
public class UserActivityLogServiceImpl implements UserActivityLogService {

    // Only editor-driven actions are surfaced in the per-task activity panel.
    private static final List<UserActivityType> EDITOR_ACTIONS =
            List.of(UserActivityType.CLAIMED, UserActivityType.DECIDED, UserActivityType.RELEASED);

    private final UserActivityLogRepository userActivityLogRepository;
    private final StatementRepository statementRepository;

    public UserActivityLogServiceImpl(UserActivityLogRepository userActivityLogRepository,
                                      StatementRepository statementRepository) {
        this.userActivityLogRepository = userActivityLogRepository;
        this.statementRepository = statementRepository;
    }

    /**
     * Writes one activity record in the caller's transaction.
     *
     * @param user the actor
     * @param actionType the activity type
     * @param statement the related statement
     * @param targetId the id of the specific target record, when applicable
     */
    @Override
    public void log(User user, UserActivityType actionType, Statement statement, Long targetId) {
        userActivityLogRepository.save(UserActivityLog.create(user, actionType, statement, targetId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserActivityLog> getRecentActivity() {
        return userActivityLogRepository.findTop20ByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<UserActivityLog>> getEditorTaskActivity(List<ReviewItem> tasks) {
        Map<Long, List<UserActivityLog>> activityByStatement = new LinkedHashMap<>();
        for (ReviewItem task : tasks) {
            List<UserActivityLog> logs = statementRepository.findById(task.getId())
                    .map(statement -> userActivityLogRepository
                            .findByStatementAndActionTypeInOrderByCreatedAtDesc(statement, EDITOR_ACTIONS))
                    .orElseGet(List::of);
            activityByStatement.put(task.getId(), logs);
        }
        return activityByStatement;
    }
}
