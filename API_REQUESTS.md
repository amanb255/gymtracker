# GymTracker API Requests

## Authentication Endpoints

### 1. Register User

```
POST /api/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123",
  "dateOfBirth": "1990-05-15",
  "heightCm": 180,
  "gender": "MALE"
}
```

### 2. Login

```
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123"
}
```

### 3. Refresh Token

```
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI..."
}
```

---

## User Endpoints

### 4. Get User Profile

```
GET /api/users/profile
Authorization: Bearer {accessToken}
```

### 5. Update User Profile

```
PATCH /api/users/profile
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Jane Doe",
  "heightCm": 175,
  "gender": "FEMALE"
}
```

---

## Workout Endpoints

### 6. Create Workout

```
POST /api/workouts
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "date": "2026-03-28",
  "title": "Chest Day",
  "notes": "Morning session",
  "exercises": [
    {
      "name": "Bench Press",
      "category": "CHEST",
      "sets": [
        { "reps": 8, "weight": 70 },
        { "reps": 8, "weight": 70 },
        { "reps": 6, "weight": 80 }
      ]
    },
    {
      "name": "Incline Dumbbell Press",
      "category": "CHEST",
      "sets": [
        { "reps": 10, "weight": 30 },
        { "reps": 10, "weight": 30 }
      ]
    }
  ]
}
```

### 7. Get All Workouts

```
GET /api/workouts
Authorization: Bearer {accessToken}
```

### 8. Get Specific Workout

```
GET /api/workouts/{workoutId}
Authorization: Bearer {accessToken}
```

### 9. Update Workout

```
PATCH /api/workouts/{workoutId}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "title": "Updated Chest Day",
  "notes": "Added more volume"
}
```

### 10. Delete Workout

```
DELETE /api/workouts/{workoutId}
Authorization: Bearer {accessToken}
```

---

## Exercise Endpoints

### 11. Add Exercise to Workout

```
POST /api/workouts/{workoutId}/exercises
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Cable Flyes",
  "category": "CHEST",
  "sets": [
    { "reps": 12, "weight": 25 },
    { "reps": 12, "weight": 25 }
  ]
}
```

### 12. Update Exercise

```
PATCH /api/workouts/{workoutId}/exercises/{exerciseId}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Barbell Bench Press",
  "category": "CHEST"
}
```

### 13. Delete Exercise

```
DELETE /api/workouts/{workoutId}/exercises/{exerciseId}
Authorization: Bearer {accessToken}
```

---

## Set Entry Endpoints

### 14. Add Set Entry to Exercise

```
POST /api/workouts/{workoutId}/exercises/{exerciseId}/sets
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "reps": 8,
  "weight": 80
}
```

### 15. Update Set Entry

```
PATCH /api/workouts/{workoutId}/exercises/{exerciseId}/sets/{setId}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "reps": 10,
  "weight": 75.5
}
```

### 16. Delete Set Entry

```
DELETE /api/workouts/{workoutId}/exercises/{exerciseId}/sets/{setId}
Authorization: Bearer {accessToken}
```

---

## Notes

- Replace `{accessToken}` with token from login/register response
- Replace `{workoutId}`, `{exerciseId}`, `{setId}` with actual UUIDs
- All timestamps are in ISO 8601 format (YYYY-MM-DD for dates)
- Weight values can be integers or decimals (kg)
- Exercise categories: CHEST, BACK, LEGS, SHOULDERS, ARMS, ABS, CARDIO
- Gender values: MALE, FEMALE
- All endpoints except /auth/\* require JWT authentication
- Workouts must contain at least 1 exercise
- Exercises must contain at least 1 set entry
