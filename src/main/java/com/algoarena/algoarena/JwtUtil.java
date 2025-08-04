//src/main/java/com/algoarena/algoarena/JwtUtil.java

package com.algoarena.algoarena;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    
    // JWT secret key (we'll add this to application.properties)
    @Value("${jwt.secret:mySecretKeyForAlgoArenaAppThatIsLongEnoughForHS256Algorithm}")
    private String jwtSecret;
    
    // JWT expiration time (24 hours)
    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationMs;
    
    // Refresh token expiration (7 days)
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;
    
    /**
     * Generate JWT access token for user
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("provider", user.getProvider());
        claims.put("username", user.getUsername());
        
        return createToken(claims, user.getProviderId(), jwtExpirationMs);
    }
    
    /**
     * Generate JWT refresh token for user
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("tokenType", "refresh");
        
        return createToken(claims, user.getProviderId(), refreshExpirationMs);
    }
    
    /**
     * Create JWT token with claims and expiration
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
    
    /**
     * Extract username (providerId) from JWT token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extract user ID from JWT token
     */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }
    
    /**
     * Extract expiration date from JWT token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extract specific claim from JWT token
     */
    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Extract all claims from JWT token
     */
    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Check if JWT token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Validate JWT token
     */
    public Boolean validateToken(String token, String providerId) {
        try {
            final String username = extractUsername(token);
            return (username.equals(providerId) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate JWT token without user validation
     */
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}