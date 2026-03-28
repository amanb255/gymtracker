package com.gymtracker.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.domain.model.Gender;
import com.gymtracker.domain.model.User;
import com.gymtracker.domain.repository.UserRepository;
import com.gymtracker.presentation.dto.AuthResponse;
import com.gymtracker.presentation.dto.RegisterRequest;
import com.gymtracker.presentation.dto.UserUpdateRequest;
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
 * Integration tests for user profile endpoints
 * 
 * Tests user profile management:
 * 1. Get authenticated user's profile
 * 2. Update user profile (name, height, gender)
 * 3. Verify authentication is required
 * 4. Verify ownership of profile data
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
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String accessToken;
    private String userId;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        // Create and login user to get token
        RegisterRequest registerRequest = new RegisterRequest(
                "John Doe",
                "john@example.com",
                "SecurePassword123",
                LocalDate.of(1990, 5, 15),
                180,
                "MALE");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(content, AuthResponse.class);
        accessToken = authResponse.accessToken();
        userId = authResponse.userId().toString();
    }

    /**
     * Test 1: Get user profile without authentication should fail
     * 
     * GET /api/users/profile (no JWT token)
     * Expected: 403 Forbidden
     */
    @Test
    void testGetProfileWithoutAuthenticationFails() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isForbidden());
    }

    /**
     * Test 2: Get authenticated user's profile
     * 
     * GET /api/users/profile (with JWT token)
     * Expected: 200 OK with user data
     */
    @Test
    void testGetProfileWithAuthenticationSuccess() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.heightCm").value(180))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    /**
     * Test 3: Update user name successfully
     * 
     * PATCH /api/users/profile with only name field
     * Expected: 200 OK with updated profile
     */
    @Test
    void testUpdateUserNameSuccess() throws Exception {
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "Jane Doe", // New name
                null, // Keep height
                null // Keep gender
        );

        mockMvc.perform(patch("/api/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com")) // Unchanged
                .andExpect(jsonPath("$.heightCm").value(180)); // Unchanged
    }

    /**
     * Test 4: Update user height successfully
     * 
     * PATCH /api/users/profile with only height field
     * Expected: 200 OK with updated profile
     */
    @Test
    void testUpdateUserHeightSuccess() throws Exception {
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                null, // Keep name
                175, // New height
                null // Keep gender
        );

        mockMvc.perform(patch("/api/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe")) // Unchanged
                .andExpect(jsonPath("$.heightCm").value(175)); // Updated
    }

    /**
     * Test 5: Update user gender successfully
     * 
     * PATCH /api/users/profile with only gender field
     * Expected: 200 OK with updated profile
     */
    @Test
    void testUpdateUserGenderSuccess() throws Exception {
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                null, // Keep name
                null, // Keep height
                "FEMALE" // New gender
        );

        mockMvc.perform(patch("/api/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gender").value("FEMALE"));
    }

    /**
     * Test 6: Update multiple fields at once
     * 
     * Expected: 200 OK with all fields updated
     */
    @Test
    void testUpdateMultipleFieldsSuccess() throws Exception {
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "Jane Smith",
                165,
                "FEMALE");

        mockMvc.perform(patch("/api/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Smith"))
                .andExpect(jsonPath("$.heightCm").value(165))
                .andExpect(jsonPath("$.gender").value("FEMALE"));
    }

    /**
     * Test 7: Update with invalid height (negative) should fail
     * 
     * Expected: 400 Bad Request with validation error
     */
    @Test
    void testUpdateInvalidHeightFails() throws Exception {
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                null,
                -180, // Negative height
                null);

        mockMvc.perform(patch("/api/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Height must be positive")));
    }

    /**
     * Test 8: Update profile without authentication should fail
     * 
     * Expected: 403 Forbidden
     */
    @Test
    void testUpdateProfileWithoutAuthenticationFails() throws Exception {
        UserUpdateRequest updateRequest = new UserUpdateRequest("New Name", null, null);

        mockMvc.perform(patch("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    /**
     * Test 9: Empty update (all null) should not change anything
     * 
     * Expected: 200 OK with unchanged profile
     */
    @Test
    void testEmptyUpdateDoesNothing() throws Exception {
        UserUpdateRequest updateRequest = new UserUpdateRequest(null, null, null);

        mockMvc.perform(patch("/api/users/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe")) // Original value
                .andExpect(jsonPath("$.heightCm").value(180)); // Original value
    }
}
