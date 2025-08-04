//src/main/java/com/algoarena/algoarena/JwtAuthenticationFilter.java

package com.algoarena.algoarena;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        final String requestTokenHeader = request.getHeader("Authorization");

        String providerId = null;
        String jwtToken = null;

        // Check if Authorization header exists and starts with "Bearer "
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                providerId = jwtUtil.extractUsername(jwtToken);
            } catch (Exception e) {
                logger.warn("Unable to get JWT Token or JWT Token has expired");
            }
        }

        // If we have a token and no authentication is set yet
        if (providerId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Validate token
            if (jwtUtil.validateToken(jwtToken)) {
                
                // Get user from database to ensure user still exists
                Optional<User> userOpt = userService.findByProviderId(providerId);
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            user, // Principal (the user object)
                            null, // Credentials (we don't need password for JWT)
                            List.of(new SimpleGrantedAuthority("ROLE_USER")) // Authorities
                        );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    logger.info("✅ JWT Authentication successful for user: " + user.getName());
                } else {
                    logger.warn("❌ User not found in database for providerId: " + providerId);
                }
            } else {
                logger.warn("❌ JWT Token validation failed");
            }
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip JWT filter for these paths
        return path.startsWith("/oauth2") || 
               path.startsWith("/login") || 
               path.equals("/") || 
               path.equals("/public") || 
               path.equals("/error") ||
               path.startsWith("/api/auth") ||
               path.startsWith("/api/users") ||
               path.startsWith("/api/health");
    }
}