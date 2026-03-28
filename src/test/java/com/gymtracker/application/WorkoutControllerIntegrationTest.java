package com.gymtracker.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.domain.repository.UserRepository;
import com.gymtracker.domain.repository.WorkoutRepository;
import com.gymtracker.presentation.dto.*;
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
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for workout endpoints
 * 
 * Tests complete workout lifecycle:
 * 1. Create workouts with exercises and sets
 * 2. Read workouts (list and get by ID)
 * 3. Update workouts, exercises, sets
 * 4. Delete workouts, exercises, sets
 * 5. Verify business rules (min 1 exercise, min 1 set)
 * 6. Verify ownership and authorization
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
class WorkoutControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkoutRepository workoutRepository;

    private String accessToken;
    private String userId;
    private WorkoutCreateRequest validWorkoutRequest;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        workoutRepository.deleteAll();

        // Register and login user
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

        // Create valid workout request with exercises and sets
        validWorkoutRequest = new WorkoutCreateRequest(
                LocalDate.now(),
                "Chest Day",
                "Morning session",
                Arrays.asList(
                        new ExerciseCreateRequest(
                                "Bench Press",
                                "CHEST",
                                Arrays.asList(
                                        new SetEntryRequest(8, 80.0),
                                        new SetEntryRequest(8, 80.0),
                                        new SetEntryRequest(8, 80.0)))));
    }

    /**
     * Test 1: Create workout without authentication should fail
     * 
     * POST /api/workouts (no JWT)
     * Expected: 403 Forbidden
     */
    @Test
    void testCreateWorkoutWithoutAuthenticationFails() throws Exception {
        mockMvc.perform(post("/api/workouts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWorkoutRequest)))
                .andExpect(status().isForbidden());
    }

    /**
     * Test 2: Create valid workout successfully
     * 
     * POST /api/workouts
     * Expected: 201 Created with full workout data including all nested
     * exercises/sets
     */
    @Test
    void testCreateWorkoutSuccess() throws Exception {
        mockMvc.perform(post("/api/workouts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWorkoutRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.title").value("Chest Day"))
                .andExpect(jsonPath("$.notes").value("Morning session"))
                .andExpect(jsonPath("$.exercises").isArray())
                .andExpect(jsonPath("$.exercises[0].name").value("Bench Press"))
                .andExpect(jsonPath("$.exercises[0].category").value("CHEST"))
                .andExpect(jsonPath("$.exercises[0].sets").isArray())
                .andExpect(jsonPath("$.exercises[0].sets.length()").value(3));
    }

    /**
     * Test 3: Create workout without exercises should fail
     * 
     * Expected: 400 Bad Request with validation error
     */
    @Test
    void testCreateWorkoutWithoutExercisesFails() throws Exception {
        WorkoutCreateRequest invalidRequest = new WorkoutCreateRequest(
                LocalDate.now(),
                "Invalid Workout",
                "No exercises",
                Arrays.asList() // Empty exercises list
        );

        mockMvc.perform(post("/api/workouts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("at least one exercise")));
    }

    /**
     * Test 4: Create exercise without sets should fail
     * 
     * Expected: 400 Bad Request with validation error
     */
    @Test
    void testCreateExerciseWithoutSetsFails() throws Exception {
        WorkoutCreateRequest invalidRequest = new WorkoutCreateRequest(
                LocalDate.now(),
                "Invalid Workout",
                "Exercise with no sets",
                Arrays.asList(
                        new ExerciseCreateRequest(
                                "Bench Press",
                                "CHEST",
                                Arrays.asList() // Empty sets list
                        )));

        mockMvc.perform(post("/api/workouts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("at least one set")));
    }

    /**
     * Test 5: Get all workouts for authenticated user
     * 
     * GET /api/workouts
     * Expected: 200 OK with list of workouts
     */
    @Test
    void testGetAllWorkoutsSuccess() throws Exception {
        // First create a workout
        mockMvc.perform(post("/api/workouts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWorkoutRequest)))
                .andExpect(status().isCreated());

        // Then get all workouts
        mockMvc.perform(get("/api/workouts")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Chest Day"))
                .andExpect(jsonPath("$[0].exercises").isArray());
    }

    /**
     * Test 6: Get specific workout by ID
     * 
     * GET /api/workouts/{id}
     * Expected: 200 OK with workout details
     */
    @Test
    void testGetWorkoutByIdSuccess() throws Exception {
        // Create workout
        MvcResult createResult = mockMvc.perform(post("/api/workouts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWorkoutRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createContent = createResult.getResponse().getContentAsString();
        WorkoutResponse workoutResponse = objectMapper.readValue(createContent, WorkoutResponse.class);
        String workoutId = workoutResponse.id().toString();

        // Get specific workout
        mockMvc.perform(get("/api/workouts/" + workoutId)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workoutId))
                .andExpect(jsonPath("$.title").value("Chest Day"))
                .andExpect(jsonPath("$.exercises[0].name").value("Bench Press"));
    }

    /**
     * Test 7: Update workout title
     * 
     * PATCH /api/workouts/{id}
     * Expected: 200 OK with updated workout
     */
    @Test
    void testUpdateWorkoutTitleSuccess() throws Exception {
        // Create workout
        MvcResult createResult = mockMvc.perform(post("/api/workouts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWorkoutRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createContent = createResult.getResponse().getContentAsString();
        WorkoutResponse workoutResponse = objectMapper.readValue(createContent, WorkoutResponse.class);
        String workoutId = workoutResponse.id().toString();

        // Update title
        WorkoutUpdateRequest updateRequest = new WorkoutUpdateRequest(
                "Updated Chest Day",
                "Afternoon session");

        mockMvc.perform(patch("/api/workouts/" + workoutId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Chest Day"))
                .andExpect(jsonPath("$.notes").value("Afternoon session"));
    }

    /**
     * Test 8: Delete workout
     * 
     * DELETE /api/workouts/{id}
     * Expected: 204 No Content
     */
    @Test
    void testDeleteWorkoutSuccess() throws Exception {
        // Create workout
        MvcResult createResult = mockMvc.perform(post("/api/workouts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWorkoutRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createContent = createResult.getResponse().getContentAsString();
        WorkoutResponse workoutResponse = objectMapper.readValue(createContent, WorkoutResponse.class);
        String workoutId = workoutResponse.id().toString();

        // Delete workout
        mockMvc.perform(delete("/api/workouts/" + workoutId)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // Verify it's deleted by trying to get it
        mockMvc.perform(get("/api/workouts/" + workoutId)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test 9: Add exercise to existing workout
     * 
     * POST /api/workouts/{id}/exercises
     * Expected: 201 Created with updated workout
     */
    @Test
    void testAddExerciseToWorkoutSuccess() throws Exception {
        // Create workout
        MvcResult createResult = mockMvc.perform(post("/api/workouts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWorkoutRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createContent = createResult.getResponse().getContentAsString();
        WorkoutResponse workoutResponse = objectMapper.readValue(createContent, WorkoutResponse.class);
        String workoutId = workoutResponse.id().toString();

        // Add new exercise
        ExerciseCreateRequest newExercise = new ExerciseCreateRequest(
                "Incline Press",
                "CHEST",
                Arrays.asList(
                        new SetEntryRequest(10, 70.0),
                        new SetEntryRequest(10, 70.0)));

        mockMvc.perform(post("/api/workouts/" + workoutId + "/exercises")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newExercise)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises.length()").value(2))
                .andExpect(jsonPath("$.exercises[1].name").value("Incline Press"));
    }

    /**
     * Test 10: Update exercise name
     * 
     * PATCH /api/workouts/{id}/exercises/{exId}
     * Expected: 200 OK with updated exercise
     */
    @Test
    void testUpdateExerciseSuccess() throws Exception {
        // Create workout
        MvcResult createResult = mockMvc.perform(post("/api/workouts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWorkoutRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createContent = createResult.getResponse().getContentAsString();
        WorkoutResponse workoutResponse = objectMapper.readValue(createContent, WorkoutResponse.class);
        String workoutId = workoutResponse.id().toString();
        String exerciseId = workoutResponse.exercises().get(0).id().toString();

        // Update exercise
        ExerciseUpdateRequest updateRequest = new ExerciseUpdateRequest(
                "Barbell Bench Press",
                "CHEST");

        mockMvc.perform(patch("/api/workouts/" + workoutId + "/exercises/" + exerciseId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].name").value("Barbell Bench Press"));
    }

    /**
     * Test 11: Add set to exercise
     * 
     * POST /api/workouts/{id}/exercises/{exId}/sets
     * Expected: 201 Created with updated workout
     */
    @Test
    void testAddSetToExerciseSuccess() throws Exception {
        // Create workout
        MvcResult createResult = mockMvc.perform(post("/api/workouts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWorkoutRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createContent = createResult.getResponse().getContentAsString();
        WorkoutResponse workoutResponse = objectMapper.readValue(createContent, WorkoutResponse.class);
        String workoutId = workoutResponse.id().toString();
        String exerciseId = workoutResponse.exercises().get(0).id().toString();

        // Add new set
        SetEntryRequest newSet = new SetEntryRequest(10, 100.0);

        mockMvc.perform(post("/api/workouts/" + workoutId + "/exercises/" + exerciseId + "/sets")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newSet)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises[0].sets.length()").value(4)) // 3 original + 1 new
                .andExpect(jsonPath("$.exercises[0].sets[3].reps").value(10))
                .andExpect(jsonPath("$.exercises[0].sets[3].weight").value(100.0));
    }

    /**
     * Test 12: Update set entry
     * 
     * PATCH /api/workouts/{id}/exercises/{exId}/sets/{setId}
     * Expected: 200 OK with updated set
     */
    @Test
    void testUpdateSetEntrySuccess() throws Exception {
        // Create workout
        MvcResult createResult = mockMvc.perform(post("/api/workouts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWorkoutRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createContent = createResult.getResponse().getContentAsString();
        WorkoutResponse workoutResponse = objectMapper.readValue(createContent, WorkoutResponse.class);
        String workoutId = workoutResponse.id().toString();
        String exerciseId = workoutResponse.exercises().get(0).id().toString();
        String setId = workoutResponse.exercises().get(0).sets().get(0).id().toString();

        // Update set
        SetEntryRequest updateRequest = new SetEntryRequest(12, 85.0);

        mockMvc.perform(patch("/api/workouts/" + workoutId + "/exercises/" + exerciseId + "/sets/" + setId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets[0].reps").value(12))
                .andExpect(jsonPath("$.exercises[0].sets[0].weight").value(85.0));
    }

    /**
     * Test 13: Verify workout ownership - user can't access others' workouts
     * 
     * Expected: 400 Bad Request (pretends resource doesn't exist for security)
     */
    @Test
    void testWorkoutOwnershipEnforcedForMultipleUsers() throws Exception {
        // Create workout with first user
        MvcResult createResult = mockMvc.perform(post("/api/workouts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWorkoutRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createContent = createResult.getResponse().getContentAsString();
        WorkoutResponse workoutResponse = objectMapper.readValue(createContent, WorkoutResponse.class);
        String workoutId = workoutResponse.id().toString();

        // Create second user
        RegisterRequest secondUserRequest = new RegisterRequest(
                "Jane Doe",
                "jane@example.com",
                "SecurePassword123",
                LocalDate.of(1992, 3, 20),
                165,
                "FEMALE");

        MvcResult secondUserResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondUserRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String secondUserContent = secondUserResult.getResponse().getContentAsString();
        AuthResponse secondUserAuth = objectMapper.readValue(secondUserContent, AuthResponse.class);
        String secondUserToken = secondUserAuth.accessToken();

        // Try to access first user's workout with second user's token
        mockMvc.perform(get("/api/workouts/" + workoutId)
                .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Workout not found")));
    }
}
