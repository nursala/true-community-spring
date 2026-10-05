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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * One user's upvote or downvote on one comment. The database unique constraint
 * enforces a single reaction per user/comment pair.
 */
@Entity
@Table(
        name = "comment_votes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comment_vote",
                columnNames = {"comment_id", "user_id"}
        )
)
public class CommentVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 20)
    private CommentVoteType voteType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected CommentVote() {
    }

    /**
     * Creates a comment reaction for the supplied user and comment.
     *
     * @param comment the reacted-to comment
     * @param user the reacting user
     * @param voteType the upvote/downvote value
     * @return the new reaction entity
     */
    public static CommentVote create(Comment comment, User user, CommentVoteType voteType) {
        CommentVote commentVote = new CommentVote();
        commentVote.comment = comment;
        commentVote.user = user;
        commentVote.voteType = voteType;
        return commentVote;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public CommentVoteType getVoteType() {
        return voteType;
    }

    public void setVoteType(CommentVoteType voteType) {
        this.voteType = voteType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
