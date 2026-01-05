package com.binder.demo.user;

import java.util.UUID;

/**
 * Represents an authenticated user of the system.
 */
public class User {

    private UUID userId;
    private String email;
    private String fullName;
    private final ROLE role;

    public User(ROLE role) {
        this.role = role;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public ROLE getRole() { return role; }

    public void setRole(ROLE role) {
    }
}
