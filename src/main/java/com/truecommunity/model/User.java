package com.truecommunity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Application account used for authentication, authorization, voting, editor
 * ownership, and trust-score ranking.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Size(min = 6)
    @Column(nullable = false)
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @NotNull
    @Column(name = "trust_score", nullable = false)
    private Double trustScore = 1.0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Protected accounts (e.g. the seeded primary editor) can never be demoted.
    @Column(name = "seeded_admin", nullable = false, columnDefinition = "boolean default false")
    private boolean seededAdmin = false;

    protected User() {
    }

    /**
     * Creates a regular user with an already encoded password.
     *
     * @param username public username
     * @param email login email
     * @param encodedPassword encoded password
     * @return the new regular user
     */
    public static User createRegularUser(String username, String email, String encodedPassword) {
        User user = createBaseUser(username, email, encodedPassword);
        user.role = Role.USER;
        return user;
    }

    private static User createBaseUser(String username, String email, String encodedPassword) {
        User user = new User();
        user.username = username;
        user.email = email;
        user.password = encodedPassword;
        return user;
    }

    /**
     * Grants editor authority and sets the editor baseline trust score.
     */
    public void promoteToEditor() {
        this.role = Role.EDITOR;
        this.trustScore = 5.0;
    }

    /**
     * Removes editor authority while keeping the current trust score.
     */
    public void demoteToUser() {
        this.role = Role.USER;
    }

    /**
     * Marks a seeded primary editor as protected from demotion.
     */
    public void markAsSeededAdmin() {
        this.seededAdmin = true;
    }

    public boolean isSeededAdmin() {
        return seededAdmin;
    }

    /**
     * Adjusts trust score while keeping it inside the supported {@code 0.0..5.0} range.
     *
     * @param delta the score change to apply
     */
    public void adjustTrustScore(double delta) {
        double currentScore = trustScore == null ? 0.0 : trustScore;
        trustScore = Math.max(0.0, Math.min(5.0, currentScore + delta));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Double getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(Double trustScore) {
        this.trustScore = trustScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
