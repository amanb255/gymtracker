package com.gymtracker.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JwtAuthenticationFilter - per-request HTTP filter that extracts and validates
 * JWT tokens
 * 
 * How it works:
 * 1. For EVERY incoming HTTP request, this filter runs BEFORE the controller
 * 2. It looks for "Authorization: Bearer <token>" header
 * 3. If token exists, validates it using JwtTokenProvider
 * 4. If valid, extracts userId and creates Spring Security Authentication
 * object
 * 5. Sets Authentication into SecurityContext so controller knows user is
 * authenticated
 * 6. If token invalid/expired, lets request pass (controller can check
 * authentication)
 * 
 * Filter runs once per request (OncePerRequestFilter ensures this)
 * Does NOT validate every endpoint - that's handled by @PreAuthorize or
 * SecurityConfig
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Main filter logic - called once per HTTP request
     * 
     * @param request     The incoming HTTP request
     * @param response    The outgoing HTTP response
     * @param filterChain Chain of other filters/servlets to pass request to
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Step 1: Extract JWT token from "Authorization" header
            String token = extractTokenFromHeader(request);

            // Step 2: If token found and valid, create Authentication
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // Extract userId from token (this is what's in the token's "subject" field)
                String userId = jwtTokenProvider.extractUserId(token).toString();

                // Create Spring Authentication object with userId as principal
                // Empty ArrayList() = no authorities/roles (can add later)
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId,
                        null, new ArrayList<>());

                // Set this authentication into Spring Security context
                // Controllers can now access this via @AuthenticationPrincipal or
                // Authentication parameter
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // If token parsing fails, just log and continue
            // The request will either be allowed (public endpoint) or rejected by
            // SecurityConfig
            logger.debug("JWT token validation failed: " + e.getMessage());
        }

        // Continue filter chain and let request reach the controller
        filterChain.doFilter(request, response);
    }

    /**
     * Helper method to extract JWT token from HTTP Authorization header
     * 
     * Looking for header format: "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
     * 
     * @param request The HTTP request
     * @return JWT token string (without "Bearer "), or null if not found
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        // Get the Authorization header value
        String authHeader = request.getHeader(AUTH_HEADER);

        // If no header or doesn't start with "Bearer ", return null
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        // Extract token part after "Bearer " prefix
        return authHeader.substring(BEARER_PREFIX.length());
    }
}
