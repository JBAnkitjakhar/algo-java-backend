//src/main/java/com/algoarena/algoarena/UserService.java

package com.algoarena.algoarena;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Process OAuth2 user login - either create new user or update existing one
     */
    public User processOAuth2User(OAuth2User oauth2User, String provider) {
        String providerId = extractProviderId(oauth2User, provider);
        
        // Check if user already exists
        Optional<User> existingUser = userRepository.findByProviderId(providerId);
        
        if (existingUser.isPresent()) {
            // User exists - update last login time
            User user = existingUser.get();
            user.updateLastLogin();
            return userRepository.save(user);
        } else {
            // New user - create and save
            User newUser = createUserFromOAuth2(oauth2User, provider, providerId);
            return userRepository.save(newUser);
        }
    }
    
    /**
     * Extract provider ID based on OAuth2 provider
     */
    private String extractProviderId(OAuth2User oauth2User, String provider) {
        if ("google".equals(provider)) {
            // Google uses 'sub' field for unique ID
            return oauth2User.getAttribute("sub");
        } else if ("github".equals(provider)) {
            // GitHub uses 'id' field for unique ID
            Integer id = oauth2User.getAttribute("id");
            return id != null ? id.toString() : null;
        }
        return null;
    }
    
    /**
     * Create new User object from OAuth2 data
     */
    private User createUserFromOAuth2(OAuth2User oauth2User, String provider, String providerId) {
        String name = extractName(oauth2User, provider);
        String email = extractEmail(oauth2User, provider);
        String username = extractUsername(oauth2User, provider);
        String avatarUrl = extractAvatarUrl(oauth2User, provider);
        
        return new User(providerId, provider, name, email, username, avatarUrl);
    }
    
    /**
     * Extract name from OAuth2 user data
     */
    private String extractName(OAuth2User oauth2User, String provider) {
        if ("google".equals(provider)) {
            return oauth2User.getAttribute("name");
        } else if ("github".equals(provider)) {
            // GitHub might have 'name' or fall back to 'login'
            String name = oauth2User.getAttribute("name");
            return name != null ? name : oauth2User.getAttribute("login");
        }
        return null;
    }
    
    /**
     * Extract email from OAuth2 user data
     */
    private String extractEmail(OAuth2User oauth2User, String provider) {
        return oauth2User.getAttribute("email");
    }
    
    /**
     * Extract username from OAuth2 user data
     */
    private String extractUsername(OAuth2User oauth2User, String provider) {
        if ("google".equals(provider)) {
            // Google doesn't have username, use name or email
            String name = oauth2User.getAttribute("name");
            return name != null ? name : oauth2User.getAttribute("email");
        } else if ("github".equals(provider)) {
            return oauth2User.getAttribute("login");
        }
        return null;
    }
    
    /**
     * Extract avatar URL from OAuth2 user data
     */
    private String extractAvatarUrl(OAuth2User oauth2User, String provider) {
        if ("google".equals(provider)) {
            return oauth2User.getAttribute("picture");
        } else if ("github".equals(provider)) {
            return oauth2User.getAttribute("avatar_url");
        }
        return null;
    }
    
    /**
     * Find user by provider ID
     */
    public Optional<User> findByProviderId(String providerId) {
        return userRepository.findByProviderId(providerId);
    }
    
    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Get all users (for admin purposes)
     */
    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Update user profile information
     */
    public User updateUser(User user) {
        user.updateLastLogin(); // Update the last modified time
        return userRepository.save(user);
    }
    
    /**
     * Update specific user fields by ID
     */
    public Optional<User> updateUserById(String userId, String name, String username) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            if (name != null && !name.trim().isEmpty()) {
                user.setName(name.trim());
            }
            
            if (username != null && !username.trim().isEmpty()) {
                user.setUsername(username.trim());
            }
            
            user.updateLastLogin(); // Update last modified time
            User savedUser = userRepository.save(user);
            return Optional.of(savedUser);
        }
        
        return Optional.empty();
    }
    
    /**
     * Find user by ID
     */
    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }
    
    /**
     * Delete user by ID
     */
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }
}