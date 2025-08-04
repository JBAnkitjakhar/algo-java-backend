//src/main/java/com/algoarena/algoarena/OAuth2LoginSuccessHandler.java

package com.algoarena.algoarena;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, 
            HttpServletResponse response, 
            Authentication authentication) throws IOException, ServletException {
        
        // Cast to OAuth2AuthenticationToken to get provider info
        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauth2Token.getPrincipal();
        String provider = oauth2Token.getAuthorizedClientRegistrationId(); // "google" or "github"
        
        try {
            // Process the user (save to database or update)
            User user = userService.processOAuth2User(oauth2User, provider);
            
            // Generate JWT tokens
            String accessToken = jwtUtil.generateAccessToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);
            
            // Log successful user processing
            System.out.println("✅ User processed successfully: " + user.toString());
            
            // Store user info in session (for fallback)
            request.getSession().setAttribute("currentUser", user);
            
            // Get frontend URL from environment or use default
            String frontendBaseUrl = System.getenv("FRONTEND_URL");
            if (frontendBaseUrl == null) {
                frontendBaseUrl = "http://localhost:3000";
            }
            
            // Redirect to Next.js frontend with tokens as URL parameters
            String frontendUrl = frontendBaseUrl + "/auth/callback" +
                "?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8) +
                "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8) +
                "&tokenType=Bearer" +
                "&expiresIn=86400";
            
            response.sendRedirect(frontendUrl);
            
        } catch (Exception e) {
            // Log error and redirect to error page
            System.err.println("❌ Error processing OAuth2 user: " + e.getMessage());
            e.printStackTrace();
            
            String frontendBaseUrl = System.getenv("FRONTEND_URL");
            if (frontendBaseUrl == null) {
                frontendBaseUrl = "http://localhost:3000";
            }
            
            response.sendRedirect(frontendBaseUrl + "?error=auth_failed");
        }
    }
}