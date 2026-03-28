package com.gymtracker.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.domain.model.Gender;
import com.gymtracker.domain.model.User;
import com.gymtracker.domain.repository.UserRepository;
import com.gymtracker.presentation.dto.LoginRequest;
import com.gymtracker.presentation.dto.RegisterRequest;
import com.gymtracker.presentation.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for authentication endpoints
 * 
 * Tests the complete auth flow:
 * 1. Register new user with email/password
 * 2. Login with credentials
 * 3. Refresh expired access token
 * 4. Verify JWT tokens are valid
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegisterRequest validRegisterRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        validRegisterRequest = new RegisterRequest(
                "John Doe",
                "john@example.com",
                "SecurePassword123",
                LocalDate.of(1990, 5, 15),
                180,
                "MALE");

        loginRequest = new LoginRequest("john@example.com", "SecurePassword123");
    }

    /**
     * Test 1: Register a new user successfully
     * 
     * POST /api/auth/register
     * Expected: 201 Created with AuthResponse containing tokens
     */
    @Test
    void testRegisterSuccess() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.expiresIn").value(900)) // 15 minutes = 900 seconds
                .andReturn();

        // Extract the response
        String content = result.getResponse().getContentAsString();
        AuthResponse response = objectMapper.readValue(content, AuthResponse.class);

        // Verify user was created in database
        assert userRepository.findByEmail("john@example.com").isPresent();
        User savedUser = userRepository.findByEmail("john@example.com").get();
        assert savedUser.getName().equals("John Doe");
    }

    /**
     * Test 2: Register with duplicate email should fail
     * 
     * Expected: 400 Bad Request with "Email already exists" message
     */
    @Test
    void testRegisterDuplicateEmailFails() throws Exception {
        // Register first user
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isCreated());

        // Try to register with same email
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Email already exists")));
    }

    /**
     * Test 3: Register with invalid email format
     * 
     * Expected: 400 Bad Request with validation error
     */
    @Test
    void testRegisterInvalidEmailFails() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "John",
                "invalid-email", // No @ symbol
                "SecurePassword123",
                LocalDate.of(1990, 5, 15),
                180,
                "MALE");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Email should be valid")));
    }

    /**
     * Test 4: Register with password too short
     * 
     * Expected: 400 Bad Request with validation error (min 8 chars)
     */
    @Test
    void testRegisterPasswordTooShortFails() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "John",
                "john@example.com",
                "short", // Less than 8 characters
                LocalDate.of(1990, 5, 15),
                180,
                "MALE");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Password must be between 8 and 128 characters")));
    }

    /**
     * Test 5: Login with valid credentials
     * 
     * POST /api/auth/login
     * Expected: 200 OK with AuthResponse containing tokens
     */
    @Test
    void testLoginSuccess() throws Exception {
        // First register
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isCreated());

        // Then login
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    /**
     * Test 6: Login with wrong password
     * 
     * Expected: 400 Bad Request with "Invalid email or password" message
     */
    @Test
    void testLoginWrongPasswordFails() throws Exception {
        // Register user
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isCreated());

        // Try to login with wrong password
        LoginRequest wrongPassword = new LoginRequest("john@example.com", "WrongPassword123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    /**
     * Test 7: Login with non-existent email
     * 
     * Expected: 400 Bad Request with "Invalid email or password" message
     */
    @Test
    void testLoginNonExistentEmailFails() throws Exception {
        LoginRequest nonExistent = new LoginRequest("nonexistent@example.com", "Password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    /**
     * Test 8: Refresh token endpoint works
     * 
     * POST /api/auth/refresh
     * Expected: 200 OK with new access token and same refresh token
     */
    @Test
    void testRefreshTokenSuccess() throws Exception {
        // Register and login
        MvcResult loginResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String loginContent = loginResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(loginContent, AuthResponse.class);
        String refreshToken = authResponse.refreshToken();

        // Use refresh token to get new access token
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken)); // Same refresh token returned
    }
}
