package com.truecommunity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Evidence link associated with a vote. The vote relationship is unique so
 * each vote can have at most one persisted evidence record.
 */
@Entity
@Table(
        name = "evidence",
        uniqueConstraints = @UniqueConstraint(name = "uk_evidence_vote", columnNames = "vote_id")
)
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String url;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "vote_id", nullable = false, unique = true)
    private Vote vote;

    protected Evidence() {
    }

    /**
     * Creates evidence for a vote.
     *
     * @param vote the vote that supplied the evidence
     * @param url the evidence URL
     * @param description the evidence description
     * @return the new evidence entity
     */
    public static Evidence create(Vote vote, String url, String description) {
        Evidence evidence = new Evidence();
        evidence.vote = vote;
        evidence.url = url;
        evidence.description = description;
        return evidence;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Vote getVote() {
        return vote;
    }

    public void setVote(Vote vote) {
        this.vote = vote;
    }
}
