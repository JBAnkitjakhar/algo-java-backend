//src/main/java/com/algoarena/algoarena/User.java

package com.algoarena.algoarena;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String providerId; // Google ID or GitHub ID
    
    private String provider; // "google" or "github"
    
    private String name;
    
    private String email;
    
    private String username; // GitHub username or Google name
    
    private String avatarUrl;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime lastLoginAt;
    
    // Constructors
    public User() {}
    
    public User(String providerId, String provider, String name, String email, String username, String avatarUrl) {
        this.providerId = providerId;
        this.provider = provider;
        this.name = name;
        this.email = email;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    // Helper method to update last login
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", providerId='" + providerId + '\'' +
                ", provider='" + provider + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}