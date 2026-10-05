package com.truecommunity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public speaker or source associated with statements. Statements reference
 * speakers through a nullable relationship while also retaining a speaker-name snapshot.
 */
@Entity
@Table(name = "speakers")
public class Speaker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Size(max = 120)
    @Column(length = 120)
    private String role;

    @Size(max = 1000)
    @Column(name = "profile_url", length = 1000)
    private String profileUrl;

    @Size(max = 1000)
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    protected Speaker() {
    }

    /**
     * Creates a speaker profile for statement attribution.
     *
     * @param name display name
     * @param role optional role or organization
     * @param profileUrl optional profile URL
     * @param imageUrl optional image URL
     * @return the new speaker entity
     */
    public static Speaker create(String name, String role, String profileUrl, String imageUrl) {
        Speaker speaker = new Speaker();
        speaker.name = name;
        speaker.role = role;
        speaker.profileUrl = profileUrl;
        speaker.imageUrl = imageUrl;
        return speaker;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
