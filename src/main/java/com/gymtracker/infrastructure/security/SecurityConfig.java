package com.gymtracker.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig - Master Spring Security configuration for the entire
 * application
 * 
 * This config:
 * 1. Defines which endpoints are PUBLIC vs. PROTECTED
 * 2. Registers JwtAuthenticationFilter into the request filter chain
 * 3. Disables session management (stateless JWT authentication)
 * 4. Configures CORS to allow requests from any origin
 * 5. Disables CSRF (not needed for stateless API)
 * 
 * Security Flow:
 * 1. Request comes in
 * 2. JwtAuthenticationFilter runs first - extracts/validates JWT token
 * 3. If no JWT token, request is anonymous
 * 4. SecurityFilterChain checks if endpoint requires authentication
 * 5. If protected and not authenticated -> 401 Unauthorized
 * 6. If public or authenticated -> request proceeds to controller
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Main security filter chain configuration
     * 
     * Defines authorization rules for HTTP endpoints
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Step 1: Disable CSRF (not needed for stateless REST API)
                // CSRF protects against cross-site request forgery via cookies - we use tokens
                .csrf(csrf -> csrf.disable())

                // Step 2: Disable sessions (stateless authentication via JWT)
                // SessionCreationPolicy.STATELESS means no JSESSIONID cookie, no server-side
                // sessions
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Step 3: Configure authorization rules for endpoints
                .authorizeHttpRequests(authz -> authz
                        // PUBLIC endpoints - no authentication required
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()

                        // PROTECTED endpoints - must be authenticated with valid JWT
                        .requestMatchers(HttpMethod.GET, "/api/users/profile").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/users/profile").authenticated()

                        // Workouts and exercises - all endpoints require authentication
                        .requestMatchers(HttpMethod.POST, "/api/workouts").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/workouts/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/workouts").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/workouts/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/workouts/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/workouts/**").authenticated()

                        // All other endpoints -> require authentication by default
                        .anyRequest().authenticated())

                // Step 4: Register JWT filter BEFORE the standard username/password filter
                // JwtAuthenticationFilter runs first on every request
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationManager bean - used by Spring Security for authentication
     * Required for some advanced authentication scenarios
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
