package com.truecommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * MVC form for creating and editing speaker profiles.
 */
public class SpeakerForm {

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 120)
    private String role;

    @Size(max = 1000)
    private String profileUrl;

    @Size(max = 1000)
    private String imageUrl;

    public SpeakerForm() {
    }

    public SpeakerForm(String name, String role, String profileUrl, String imageUrl) {
        this.name = name;
        this.role = role;
        this.profileUrl = profileUrl;
        this.imageUrl = imageUrl;
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
