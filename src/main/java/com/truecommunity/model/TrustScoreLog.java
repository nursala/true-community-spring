package com.truecommunity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Audit entry for a trust-score change caused by an editor decision. Logs are
 * tied to votes so decision reverts can undo the exact applied changes.
 */
@Entity
@Table(name = "trust_score_logs")
public class TrustScoreLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne
    @JoinColumn(name = "vote_id", unique = true)
    private Vote vote;

    @NotNull
    @Column(name = "change_amount", nullable = false)
    private Double changeAmount;

    @NotNull
    @Column(name = "new_score", nullable = false)
    private Double newScore;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected TrustScoreLog() {
    }

    /**
     * Creates a trust-score audit log for one vote adjustment.
     *
     * @param user the affected voter
     * @param vote the vote that caused the score change
     * @param changeAmount the applied score delta
     * @param newScore the resulting score
     * @param reason the stored reason
     * @return the new log entity
     */
    public static TrustScoreLog create(User user, Vote vote, Double changeAmount, Double newScore, String reason) {
        TrustScoreLog log = new TrustScoreLog();
        log.user = user;
        log.vote = vote;
        log.changeAmount = changeAmount;
        log.newScore = newScore;
        log.reason = reason;
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

    public Vote getVote() {
        return vote;
    }

    public void setVote(Vote vote) {
        this.vote = vote;
    }

    public Double getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(Double changeAmount) {
        this.changeAmount = changeAmount;
    }

    public Double getNewScore() {
        return newScore;
    }

    public void setNewScore(Double newScore) {
        this.newScore = newScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
