//src/main/java/com/algoarena/algoarena/AuthController.java

package com.algoarena.algoarena;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}) // Allow Next.js frontend
public class AuthController {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserService userService;
    
    /**
     * Generate JWT tokens after OAuth2 login
     * This endpoint is called after successful OAuth2 authentication
     */
    @PostMapping("/generate-token")
    public ResponseEntity<?> generateToken(HttpServletRequest request) {
        try {
            // Get user from session (set by OAuth2LoginSuccessHandler)
            User currentUser = (User) request.getSession().getAttribute("currentUser");
            
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "No authenticated user found",
                    "message", "Please login first"
                ));
            }
            
            // Generate JWT tokens
            String accessToken = jwtUtil.generateAccessToken(currentUser);
            String refreshToken = jwtUtil.generateRefreshToken(currentUser);
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", 86400); // 24 hours in seconds
            response.put("user", Map.of(
                "id", currentUser.getId(),
                "name", currentUser.getName(),
                "email", currentUser.getEmail() != null ? currentUser.getEmail() : "",
                "username", currentUser.getUsername(),
                "provider", currentUser.getProvider(),
                "avatarUrl", currentUser.getAvatarUrl() != null ? currentUser.getAvatarUrl() : ""
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Token generation failed",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Test endpoint - Generate JWT for a specific user by email
     * Only for testing purposes
     */
    @PostMapping("/generate-token-test")
    public ResponseEntity<?> generateTokenTest(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            
            if (email == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Email required",
                    "message", "Please provide email in request body"
                ));
            }
            
            Optional<User> userOpt = userService.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "User not found",
                    "message", "User with email " + email + " not found"
                ));
            }
            
            User user = userOpt.get();
            
            // Generate JWT tokens
            String accessToken = jwtUtil.generateAccessToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", 86400);
            response.put("user", Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "username", user.getUsername(),
                "provider", user.getProvider(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Token generation failed",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            
            if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid refresh token",
                    "message", "Please login again"
                ));
            }
            
            // Extract user ID from refresh token
            String userId = jwtUtil.extractUserId(refreshToken);
            Optional<User> userOpt = userService.findByProviderId(userId);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "User not found",
                    "message", "Please login again"
                ));
            }
            
            User user = userOpt.get();
            
            // Generate new access token
            String newAccessToken = jwtUtil.generateAccessToken(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", 86400);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Token refresh failed",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Validate JWT token
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            
            if (token == null) {
                return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "No token provided"));
            }
            
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            boolean isValid = jwtUtil.validateToken(token);
            
            if (isValid) {
                String userId = jwtUtil.extractUserId(token);
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "userId", userId,
                    "message", "Token is valid"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "message", "Token is invalid or expired"
                ));
            }
            
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "message", "Token validation failed: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get current user info from JWT token
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing or invalid Authorization header",
                    "message", "Please provide a valid Bearer token"
                ));
            }
            
            String token = authHeader.substring(7);
            
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid token",
                    "message", "Token is invalid or expired"
                ));
            }
            
            String providerId = jwtUtil.extractUsername(token);
            Optional<User> userOpt = userService.findByProviderId(providerId);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "User not found",
                    "message", "User associated with token not found"
                ));
            }
            
            User user = userOpt.get();
            
            Map<String, Object> userInfo = Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "username", user.getUsername(),
                "provider", user.getProvider(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "createdAt", user.getCreatedAt(),
                "lastLoginAt", user.getLastLoginAt()
            );
            
            return ResponseEntity.ok(userInfo);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get user info",
                "message", e.getMessage()
            ));
        }
    }
}