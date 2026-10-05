package com.truecommunity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Audit record for user actions tied to a statement, such as voting, commenting,
 * claiming, deciding, or releasing.
 */
@Entity
@Table(name = "user_activity_log")
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private UserActivityType actionType;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "statement_id", nullable = false)
    private Statement statement;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected UserActivityLog() {
    }

    /**
     * Creates an activity log entry.
     *
     * @param user the actor
     * @param actionType action category
     * @param statement related statement
     * @param targetId optional id of the specific target record
     * @return the new log entity
     */
    public static UserActivityLog create(User user, UserActivityType actionType, Statement statement, Long targetId) {
        UserActivityLog log = new UserActivityLog();
        log.user = user;
        log.actionType = actionType;
        log.statement = statement;
        log.targetId = targetId;
        return log;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserActivityType getActionType() {
        return actionType;
    }

    public void setActionType(UserActivityType actionType) {
        this.actionType = actionType;
    }

    public Statement getStatement() {
        return statement;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
