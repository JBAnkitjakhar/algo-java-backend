//src/main/java/com/algoarena/algoarena/FrontendAuthController.java

package com.algoarena.algoarena;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/frontend")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://*.vercel.app"})
public class FrontendAuthController {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserService userService;
    
    /**
     * Initiate OAuth2 login for frontend
     * Redirects to Google OAuth2 with proper callback
     */
    @GetMapping("/auth/google")
    public void initiateGoogleLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }
    
    /**
     * Initiate GitHub OAuth2 login for frontend
     */
    @GetMapping("/auth/github")
    public void initiateGitHubLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/github");
    }
    
    /**
     * OAuth2 callback handler that returns JWT tokens for frontend
     * This will be called after successful OAuth2 authentication
     */
    @GetMapping("/auth/callback")
    public ResponseEntity<?> handleOAuth2Callback(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Get user from session (set by OAuth2LoginSuccessHandler)
            User currentUser = (User) request.getSession().getAttribute("currentUser");
            
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Authentication failed",
                    "message", "No user session found"
                ));
            }
            
            // Generate JWT tokens
            String accessToken = jwtUtil.generateAccessToken(currentUser);
            String refreshToken = jwtUtil.generateRefreshToken(currentUser);
            
            // Return tokens for frontend to store
            return ResponseEntity.ok(Map.of(
                "success", true,
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "tokenType", "Bearer",
                "expiresIn", 86400,
                "user", Map.of(
                    "id", currentUser.getId(),
                    "name", currentUser.getName(),
                    "email", currentUser.getEmail() != null ? currentUser.getEmail() : "",
                    "username", currentUser.getUsername(),
                    "provider", currentUser.getProvider(),
                    "avatarUrl", currentUser.getAvatarUrl() != null ? currentUser.getAvatarUrl() : ""
                )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Token generation failed",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Frontend-friendly user info endpoint
     */
    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUserForFrontend(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of(
                    "authenticated", false,
                    "message", "No valid token provided"
                ));
            }
            
            String token = authHeader.substring(7);
            
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of(
                    "authenticated", false,
                    "message", "Token is invalid or expired"
                ));
            }
            
            String providerId = jwtUtil.extractUsername(token);
            var userOpt = userService.findByProviderId(providerId);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of(
                    "authenticated", false,
                    "message", "User not found"
                ));
            }
            
            User user = userOpt.get();
            
            return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "user", Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail() != null ? user.getEmail() : "",
                    "username", user.getUsername(),
                    "provider", user.getProvider(),
                    "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                    "createdAt", user.getCreatedAt(),
                    "lastLoginAt", user.getLastLoginAt()
                )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "authenticated", false,
                "message", "Server error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Logout endpoint for frontend
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout() {
        // Since JWT is stateless, logout is handled on frontend by removing tokens
        // In a production app, you might want to blacklist the token
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Logged out successfully"
        ));
    }
}