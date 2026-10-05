package com.truecommunity.repo;

import com.truecommunity.model.Statement;
import com.truecommunity.model.UserActivityLog;
import com.truecommunity.model.UserActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Persistence operations for activity logs shown on dashboards and editor task panels.
 */
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    List<UserActivityLog> findTop20ByOrderByCreatedAtDesc();

    List<UserActivityLog> findByStatementAndActionTypeInOrderByCreatedAtDesc(
            Statement statement, Collection<UserActivityType> actionTypes);

    boolean existsByStatementAndActionTypeAndTargetId(
            Statement statement, UserActivityType actionType, Long targetId);

    /**
     * Removes every activity-log row referencing a statement. Used by the
     * guarded admin statement delete so the not-null statement FK does not block
     * removal of a previously claimed/released statement.
     *
     * @param statementId the referenced statement id
     */
    @Modifying
    @Query("DELETE FROM UserActivityLog l WHERE l.statement.id = :statementId")
    void deleteByStatementId(@Param("statementId") Long statementId);
}
