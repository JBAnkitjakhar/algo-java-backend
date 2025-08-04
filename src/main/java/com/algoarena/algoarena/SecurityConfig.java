//src/main/java/com/algoarena/algoarena/SecurityConfig.java

package com.algoarena.algoarena;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS with our configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // Disable CSRF for API endpoints (needed for JWT)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            )
            // Configure session management - stateless for JWT
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Allow sessions for OAuth2, but JWT endpoints are stateless
            )
            // Add JWT filter before the default authentication filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Configure URL-based authorization
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/public", "/error").permitAll()
                .requestMatchers("/api/users", "/api/auth/**", "/api/frontend/**").permitAll()  // Allow public access to auth APIs
                .requestMatchers("/oauth2/**", "/login/**").permitAll()  // Allow OAuth2 endpoints
                .requestMatchers("/api/protected/**").authenticated()  // Require authentication for protected APIs
                .anyRequest().authenticated()  // All other URLs require authentication
            )
            // Configure OAuth2 login with custom success handler
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth2/authorization/google")  // Default login redirect
                .successHandler(oauth2LoginSuccessHandler)  // Use our custom success handler
                .failureUrl("/login?error=true")            // Where to go if login fails
            )
            // Configure logout
            .logout(logout -> logout
                .logoutSuccessUrl("/")                      // Where to go after logout
                .invalidateHttpSession(true)
                .clearAuthentication(true)
            );

        return http.build();
    }
}