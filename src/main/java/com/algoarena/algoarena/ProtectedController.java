//src/main/java/com/algoarena/algoarena/ProtectedController.java

package com.algoarena.algoarena;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/protected")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}) // Allow Next.js frontend
public class ProtectedController {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserService userService;
    
    /**
     * Get user profile - requires valid JWT token
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            // Validate JWT token and get user
            User user = validateTokenAndGetUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Unauthorized",
                    "message", "Invalid or expired token"
                ));
            }
            
            // Return user profile
            Map<String, Object> profile = Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "username", user.getUsername(),
                "provider", user.getProvider(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "createdAt", user.getCreatedAt(),
                "lastLoginAt", user.getLastLoginAt()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "user", profile
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Update user profile - requires valid JWT token
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> updates) {
        try {
            // Validate JWT token and get user
            User user = validateTokenAndGetUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Unauthorized",
                    "message", "Invalid or expired token"
                ));
            }
            
            // Extract update fields
            String newName = updates.get("name");
            String newUsername = updates.get("username");
            
            // Validate that at least one field is provided
            if ((newName == null || newName.trim().isEmpty()) && 
                (newUsername == null || newUsername.trim().isEmpty())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "No valid fields to update",
                    "message", "Provide 'name' or 'username' to update"
                ));
            }
            
            // Update user using UserService
            Optional<User> updatedUserOpt = userService.updateUserById(user.getId(), newName, newUsername);
            
            if (updatedUserOpt.isPresent()) {
                User updatedUser = updatedUserOpt.get();
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated successfully",
                    "user", Map.of(
                        "id", updatedUser.getId(),
                        "name", updatedUser.getName(),
                        "username", updatedUser.getUsername(),
                        "email", updatedUser.getEmail() != null ? updatedUser.getEmail() : "",
                        "lastLoginAt", updatedUser.getLastLoginAt()
                    )
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of(
                    "error", "User not found",
                    "message", "User could not be updated"
                ));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get user dashboard data - requires valid JWT token
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData(@RequestHeader("Authorization") String authHeader) {
        try {
            // Validate JWT token and get user
            User user = validateTokenAndGetUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Unauthorized",
                    "message", "Invalid or expired token"
                ));
            }
            
            // Prepare dashboard data
            Map<String, Object> dashboardData = new HashMap<>();
            dashboardData.put("welcomeMessage", "Welcome back, " + user.getName() + "!");
            dashboardData.put("userStats", Map.of(
                "totalLogins", "N/A", // You can track this later
                "accountAge", "Member since " + user.getCreatedAt().toLocalDate(),
                "provider", "Signed in with " + user.getProvider()
            ));
            dashboardData.put("quickActions", new String[]{
                "View Profile",
                "Update Settings", 
                "View Activity",
                "Logout"
            });
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", dashboardData,
                "user", Map.of(
                    "name", user.getName(),
                    "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
                )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Admin endpoint - get all users (requires valid JWT token)
     */
    @GetMapping("/admin/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        try {
            // Validate JWT token and get user
            User user = validateTokenAndGetUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Unauthorized",
                    "message", "Invalid or expired token"
                ));
            }
            
            // Get all users (in a real app, you'd check if user is admin)
            Iterable<User> allUsers = userService.getAllUsers();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "users", allUsers,
                "requestedBy", user.getName()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Helper method to validate JWT token and get user
     */
    private User validateTokenAndGetUser(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }
            
            String token = authHeader.substring(7);
            
            if (!jwtUtil.validateToken(token)) {
                return null;
            }
            
            String providerId = jwtUtil.extractUsername(token);
            Optional<User> userOpt = userService.findByProviderId(providerId);
            
            return userOpt.orElse(null);
            
        } catch (Exception e) {
            return null;
        }
    }
}