package com.truecommunity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * A user's verdict and optional explanation/evidence for a statement. The
 * unique constraint enforces at most one vote per user per statement.
 */
@Entity
@Table(
        name = "votes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_votes_user_statement",
                columnNames = {"user_id", "statement_id"}
        )
)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Verdict verdict;

    @Size(max = 1000)
    @Column(length = 1000)
    private String explanation;

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

    // Crowdsourced classification: the single topic this voter suggests for the statement.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_topic_id")
    private Topic suggestedTopic;

    protected Vote() {
    }

    /**
     * Creates a vote without an evidence URL.
     *
     * @param user voter
     * @param statement voted statement
     * @param verdict selected verdict
     * @param explanation optional explanation
     * @return the new vote entity
     */
    public static Vote create(User user, Statement statement, Verdict verdict, String explanation) {
        return create(user, statement, verdict, explanation, null);
    }

    /**
     * Creates a vote with optional evidence URL.
     *
     * @param user voter
     * @param statement voted statement
     * @param verdict selected verdict
     * @param explanation optional explanation
     * @param evidenceUrl optional evidence URL
     * @return the new vote entity
     */
    public static Vote create(User user, Statement statement, Verdict verdict, String explanation, String evidenceUrl) {
        Vote vote = new Vote();
        vote.user = user;
        vote.statement = statement;
        vote.verdict = verdict;
        vote.explanation = explanation;
        vote.evidenceUrl = evidenceUrl;
        return vote;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
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

    public Topic getSuggestedTopic() {
        return suggestedTopic;
    }

    public void setSuggestedTopic(Topic suggestedTopic) {
        this.suggestedTopic = suggestedTopic;
    }
}
