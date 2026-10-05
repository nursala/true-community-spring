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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Statement submitted for community voting and editor review. A statement can
 * move from {@code PENDING} to {@code UNDER_REVIEW} when claimed and to
 * {@code DECIDED} when the editor publishes a final decision.
 */
@Entity
@Table(name = "statements")
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @NotBlank
    @Column(name = "speaker_name", nullable = false)
    private String speakerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speaker_id")
    private Speaker speaker;

    @Column(name = "source_url")
    private String sourceUrl;


    @Column(name = "final_evidence_url", length = 500)
    private String finalEvidenceUrl;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "statement_topics",
            joinColumns = @JoinColumn(name = "statement_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    private Set<Topic> topics = new HashSet<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatementStatus status = StatementStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // Editor concurrency control: who has claimed this statement for review.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimed_by")
    private User claimedBy;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    protected Statement() {
    }

    /**
     * Creates a pending statement with a speaker snapshot and optional topic assignments.
     *
     * @param text statement text
     * @param speaker selected speaker
     * @param sourceUrl optional source URL
     * @param topics initial topics, if any
     * @return the new pending statement
     */
    public static Statement createPending(String text, Speaker speaker, String sourceUrl, Set<Topic> topics) {
        Statement statement = new Statement();
        statement.text = text;
        statement.setSpeaker(speaker);
        statement.sourceUrl = sourceUrl;
        statement.status = StatementStatus.PENDING;
        if (topics != null) {
            statement.topics.addAll(topics);
        }
        return statement;
    }

    public void addTopic(Topic topic) {
        topics.add(topic);
    }

    /**
     * Applies an editor edit to the mutable statement details in one place,
     * replacing text, speaker (keeping the speaker-name snapshot), source URL,
     * and the assigned topics. Status and workflow timestamps are untouched.
     *
     * @param text new statement text
     * @param speaker selected speaker
     * @param sourceUrl optional source URL, or {@code null}
     * @param topics replacement topic set, or {@code null} for none
     */
    public void editDetails(String text, Speaker speaker, String sourceUrl, Set<Topic> topics) {
        this.text = text;
        setSpeaker(speaker);
        this.sourceUrl = sourceUrl;
        this.topics.clear();
        if (topics != null) {
            this.topics.addAll(topics);
        }
    }

    /**
     * Marks the statement as decided and records the publish timestamp once.
     */
    public void markDecided() {
        status = StatementStatus.DECIDED;
        if (publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
    }

    /** Locks the statement to the given editor and moves it into review. */
    public void claim(User editor) {
        this.claimedBy = editor;
        this.claimedAt = LocalDateTime.now();
        this.status = StatementStatus.UNDER_REVIEW;
    }

    /** Returns the statement to the open pool: clears the claim and reverts to PENDING. */
    public void release() {
        this.claimedBy = null;
        this.claimedAt = null;
        this.status = StatementStatus.PENDING;
    }

    public boolean isClaimed() {
        return claimedBy != null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSpeakerName() {
        return speakerName;
    }

    public void setSpeakerName(String speakerName) {
        this.speakerName = speakerName;
    }

    public Speaker getSpeaker() {
        return speaker;
    }

    public void setSpeaker(Speaker speaker) {
        this.speaker = speaker;
        if (speaker != null) {
            this.speakerName = speaker.getName();
        }
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }


    public String getFinalEvidenceUrl() {
        return finalEvidenceUrl;
    }

    public void setFinalEvidenceUrl(String finalEvidenceUrl) {
        this.finalEvidenceUrl = finalEvidenceUrl;
    }

    public Set<Topic> getTopics() {
        return topics;
    }

    public void setTopics(Set<Topic> topics) {
        this.topics = topics;
    }

    public StatementStatus getStatus() {
        return status;
    }

    public void setStatus(StatementStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public User getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(User claimedBy) {
        this.claimedBy = claimedBy;
    }

    public LocalDateTime getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(LocalDateTime claimedAt) {
        this.claimedAt = claimedAt;
    }
}
