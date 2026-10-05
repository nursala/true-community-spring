package com.truecommunity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * User discussion comment attached to a statement. Comments belong to one user
 * and one statement and may optionally carry an evidence URL copied from a vote.
 */
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String content;

    @Size(max = 500)
    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "statement_id", nullable = false)
    private Statement statement;

    protected Comment() {
    }

    /**
     * Creates a comment without evidence.
     *
     * @param user the author
     * @param statement the discussed statement
     * @param content the comment body
     * @return the new comment entity
     */
    public static Comment create(User user, Statement statement, String content) {
        return create(user, statement, content, null);
    }

    /**
     * Creates a comment with an optional evidence URL.
     *
     * @param user the author
     * @param statement the discussed statement
     * @param content the comment body
     * @param evidenceUrl optional evidence URL
     * @return the new comment entity
     */
    public static Comment create(User user, Statement statement, String content, String evidenceUrl) {
        Comment comment = new Comment();
        comment.user = user;
        comment.statement = statement;
        comment.content = content;
        comment.evidenceUrl = evidenceUrl;
        return comment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Statement getStatement() {
        return statement;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }
}
