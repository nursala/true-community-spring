package com.truecommunity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Controlled classification topic used by statement assignments and voter
 * topic suggestions.
 */
@Entity
@Table(
        name = "topics",
        uniqueConstraints = @UniqueConstraint(name = "uk_topics_name", columnNames = "name")
)
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    protected Topic() {
    }

    /**
     * Creates a topic with the supplied unique name.
     *
     * @param name topic display name
     * @return the new topic entity
     */
    public static Topic create(String name) {
        Topic topic = new Topic();
        topic.name = name;
        return topic;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
