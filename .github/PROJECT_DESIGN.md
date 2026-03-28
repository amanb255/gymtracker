# GymTracker - Complete System Design Document

**Version**: 1.0  
**Date**: March 15, 2026  
**Status**: Design Complete - Ready for Implementation  
**Target**: Production-Ready iOS App + Backend

---

## 📋 Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture & Technology Stack](#architecture--technology-stack)
3. [Package Structure](#package-structure)
4. [Database Schema](#database-schema)
5. [Entity Model & Relationships](#entity-model--relationships)
6. [API Contract](#api-contract)
7. [Data Flow](#data-flow)
8. [Security Model](#security-model)
9. [Implementation Roadmap](#implementation-roadmap)
10. [Deployment Strategy](#deployment-strategy)

---

## 🎯 Project Overview

### Vision

Build a production-grade fitness tracking backend that powers an iOS app, allowing users to:

- Register/login securely
- Log workout sessions
- Track exercises and sets
- View workout history
- Extend with AI recommendations, nutrition tracking, and social features later

### Principles

- **Clean Architecture**: Layered design (Presentation → Application → Domain → Infrastructure)
- **Domain-Driven Design**: Business logic in entities, not services
- **API-First**: RESTful JSON API, mobile-first design
- **Security-First**: JWT auth, bcrypt passwords, user isolation
- **Scalable**: Designed for single user initially, multi-user ready
- **Testable**: Unit tests, integration tests, security tests

### Success Criteria

- User can register, login, create workouts on iPhone
- All data encrypted in transit (HTTPS)
- All passwords hashed (bcrypt)
- Each user isolated (can't see others' workouts)
- API responses sub-500ms (single user)
- Zero downtime deployments

---

## 🏗️ Architecture & Technology Stack

### Backend Architecture: Layered (3 + 1 Infrastructure)

```
┌─────────────────────────────────────────────┐
│     PRESENTATION LAYER                      │
│  (REST Controllers, DTOs, HTTP Handlers)   │
│  Package: com.gymtracker.presentation      │
└────────────────┬────────────────────────────┘
                 │ HTTP Request/Response
                 ▼
┌─────────────────────────────────────────────┐
│     APPLICATION LAYER (Services)            │
│  (Business Logic Orchestration)             │
│  Package: com.gymtracker.application       │
└────────────────┬────────────────────────────┘
                 │ Call domain/repo
                 ▼
┌─────────────────────────────────────────────┐
│     DOMAIN LAYER (Models)                   │
│  (Pure Business Rules & Entities)           │
│  Package: com.gymtracker.domain            │
│    - domain.model: Entities, Enums         │
│    - domain.repository: Interfaces         │
└────────────────┬────────────────────────────┘
                 │ Interface
                 ▼
┌─────────────────────────────────────────────┐
│     INFRASTRUCTURE LAYER                    │
│  (DB, Security, External Services)          │
│  Package: com.gymtracker.infrastructure    │
│    - security: JWT, Password, Filters      │
│    - persistence: JPA Repositories         │
│    - config: Spring Configuration          │
└─────────────────────────────────────────────┘
```

### Technology Stack

| Layer          | Technology       | Version               | Purpose                |
| -------------- | ---------------- | --------------------- | ---------------------- |
| **Backend**    | Spring Boot      | 4.0.2                 | Framework              |
| **ORM**        | JPA/Hibernate    | 7.2.1                 | Database mapping       |
| **Database**   | PostgreSQL       | 14 (dev), Neon (prod) | Data persistence       |
| **Security**   | Spring Security  | 7.0.3                 | Auth/authz framework   |
| **Auth**       | JWT              | -                     | Stateless tokens       |
| **Hashing**    | bcrypt           | (Spring default)      | Password security      |
| **Testing**    | JUnit5 + Mockito | -                     | Unit/Integration tests |
| **Build**      | Maven            | 3.8+                  | Build tool             |
| **Java**       | Java             | 18+                   | Language               |
| **Deployment** | Render           | -                     | PaaS hosting           |

---

## 📁 Package Structure

```
src/main/java/com/gymtracker/
├── GymtrackerApplication.java
│
├── presentation/                    # REST APIs, Controllers
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   └── WorkoutController.java
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── AuthResponse.java
│   │   ├── RefreshTokenRequest.java
│   │   ├── UserResponse.java
│   │   ├── WorkoutRequest.java
│   │   ├── WorkoutResponse.java
│   │   ├── ExerciseRequest.java
│   │   └── ExerciseResponse.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       └── ApiError.java
│
├── application/                     # Service Layer (Orchestration)
│   ├── AuthService.java
│   ├── UserService.java
│   └── WorkoutService.java
│
├── domain/                          # Business Logic (Core)
│   ├── model/
│   │   ├── User.java
│   │   ├── Workout.java
│   │   ├── Exercise.java
│   │   ├── SetEntry.java
│   │   ├── Gender.java
│   │   └── ExerciseCategory.java
│   └── repository/
│       ├── UserRepository.java
│       └── WorkoutRepository.java
│
└── infrastructure/                  # Technical Implementations
    ├── security/
    │   ├── JwtTokenProvider.java
    │   ├── JwtAuthenticationFilter.java
    │   ├── PasswordEncoderComponent.java
    │   └── SecurityConfig.java
    ├── persistence/
    │   └── (Spring Data JPA implementations)
    └── config/
        └── ApplicationConfig.java

src/main/resources/
├── application.properties            # Dev config
├── application-prod.properties       # Prod config
└── application-test.properties       # Test config
```

---

## 🗄️ Database Schema

### Complete ERD

```
┌─────────────────────────────┐
│         USERS               │
├─────────────────────────────┤
│ id (UUID) [PK]              │
│ email (VARCHAR) [UNIQUE]    │
│ hashed_password (VARCHAR)   │
│ name (VARCHAR)              │
│ height_cm (INT)             │
│ date_of_birth (DATE)        │
│ gender (VARCHAR)            │
│ created_at (TIMESTAMP)      │
│ updated_at (TIMESTAMP)      │
└─────┬───────────────────────┘
      │ 1:N (cascade delete)
      │
      ▼
┌─────────────────────────────┐
│       WORKOUTS              │
├─────────────────────────────┤
│ id (UUID) [PK]              │
│ user_id (UUID) [FK]         │
│ date (DATE)                 │
│ title (VARCHAR)             │
│ notes (TEXT)                │
│ created_at (TIMESTAMP)      │
│ updated_at (TIMESTAMP)      │
└─────┬───────────────────────┘
      │ 1:N (cascade delete)
      │
      ▼
┌─────────────────────────────┐
│      EXERCISES              │
├─────────────────────────────┤
│ id (UUID) [PK]              │
│ workout_id (UUID) [FK]      │
│ name (VARCHAR)              │
│ category (VARCHAR)          │
│ created_at (TIMESTAMP)      │
└─────┬───────────────────────┘
      │ 1:N (cascade delete)
      │
      ▼
┌─────────────────────────────┐
│      SET_ENTRIES            │
├─────────────────────────────┤
│ id (UUID) [PK]              │
│ exercise_id (UUID) [FK]     │
│ reps (INT)                  │
│ weight (DOUBLE)             │
│ created_at (TIMESTAMP)      │
└─────────────────────────────┘
```

### SQL Schema (DDL)

```sql
-- Users table with authentication fields
CREATE TABLE users (
    id UUID PRIMARY KEY NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    hashed_password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    height_cm INTEGER NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT check_height_positive CHECK (height_cm > 0),
    CONSTRAINT check_email_not_empty CHECK (email != '')
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Workouts table (belongs to user)
CREATE TABLE workouts (
    id UUID PRIMARY KEY NOT NULL,
    user_id UUID NOT NULL,
    date DATE NOT NULL,
    title VARCHAR(255) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT fk_workouts_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT check_title_not_empty CHECK (title != '')
);

CREATE INDEX idx_workouts_user_id ON workouts(user_id);
CREATE INDEX idx_workouts_date ON workouts(date);

-- Exercises table (belongs to workout)
CREATE TABLE exercises (
    id UUID PRIMARY KEY NOT NULL,
    workout_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_exercises_workout FOREIGN KEY (workout_id)
        REFERENCES workouts(id) ON DELETE CASCADE,
    CONSTRAINT check_exercise_name_not_empty CHECK (name != '')
);

CREATE INDEX idx_exercises_workout_id ON exercises(workout_id);

-- Set entries table (belongs to exercise)
CREATE TABLE set_entries (
    id UUID PRIMARY KEY NOT NULL,
    exercise_id UUID NOT NULL,
    reps INTEGER NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_set_entries_exercise FOREIGN KEY (exercise_id)
        REFERENCES exercises(id) ON DELETE CASCADE,
    CONSTRAINT check_reps_positive CHECK (reps > 0),
    CONSTRAINT check_weight_non_negative CHECK (weight >= 0)
);

CREATE INDEX idx_set_entries_exercise_id ON set_entries(exercise_id);
```

---

## 🏛️ Entity Model & Relationships

### User Entity (Core)

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String hashedPassword;  // bcrypt format

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer heightCm;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Workout> workouts;
}
```

**Invariants**:

- Email unique, non-empty, valid format
- Password hashed (never plain text)
- Name non-empty
- Height > 0
- DateOfBirth not in future
- Gender non-null
- Has many workouts (can be empty)

---

### Workout Aggregate Root

```java
@Entity
@Table(name = "workouts")
public class Workout {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Exercise> exercises;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime updatedAt;
}
```

**Invariants**:

- Belongs to one user
- Has date
- Has non-empty title
- Must have ≥1 exercise
- Unique exercise names per workout
- Exercises deleted when workout deleted

---

### Exercise (Owned Entity)

```java
@Entity
@Table(name = "exercises")
public class Exercise {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseCategory category;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SetEntry> sets;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

**Invariants**:

- Belongs to one workout
- Non-empty name
- Category from enum
- Must have ≥1 set entry
- Sets deleted when exercise deleted

---

### SetEntry (Leaf Entity)

```java
@Entity
@Table(name = "set_entries")
public class SetEntry {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(nullable = false)
    private int reps;

    @Column(nullable = false)
    private double weight;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

**Invariants**:

- Belongs to one exercise
- Reps > 0
- Weight ≥ 0

---

## 🌐 API Contract

### Authentication Endpoints

#### Register

```
POST /api/auth/register
Content-Type: application/json

Request:
{
    "email": "john@example.com",
    "password": "SecurePass123!",
    "name": "John Doe",
    "dateOfBirth": "1990-05-15",
    "heightCm": 180,
    "gender": "MALE"
}

Response (201):
{
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john@example.com",
    "name": "John Doe",
    "expiresIn": 900
}

Error (400): {"error": "Email already registered"}
Error (422): {"error": "Invalid request", "details": {...}}
```

#### Login

```
POST /api/auth/login
Content-Type: application/json

Request:
{
    "email": "john@example.com",
    "password": "SecurePass123!"
}

Response (200):
{
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john@example.com",
    "name": "John Doe",
    "expiresIn": 900
}

Error (401): {"error": "Invalid email or password"}
```

#### Refresh Token

```
POST /api/auth/refresh
Content-Type: application/json

Request:
{
    "refreshToken": "eyJhbGc..."
}

Response (200):
{
    "accessToken": "eyJhbGc...",
    "expiresIn": 900
}

Error (401): {"error": "Invalid or expired refresh token"}
```

### User Endpoints

#### Get Profile

```
GET /api/users/me
Authorization: Bearer <access_token>

Response (200):
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john@example.com",
    "name": "John Doe",
    "dateOfBirth": "1990-05-15",
    "heightCm": 180,
    "gender": "MALE",
    "createdAt": "2026-03-15T10:00:00Z"
}

Error (401): {"error": "Unauthorized"}
```

#### Update Profile

```
PUT /api/users/me
Authorization: Bearer <access_token>
Content-Type: application/json

Request:
{
    "name": "John Smith",
    "heightCm": 182,
    "gender": "MALE"
}

Response (200):
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john@example.com",
    "name": "John Smith",
    "dateOfBirth": "1990-05-15",
    "heightCm": 182,
    "gender": "MALE",
    "updatedAt": "2026-03-15T11:00:00Z"
}

Error (401): {"error": "Unauthorized"}
Error (422): {"error": "Invalid input"}
```

### Workout Endpoints

#### Create Workout

```
POST /api/workouts
Authorization: Bearer <access_token>
Content-Type: application/json

Request:
{
    "date": "2026-03-15",
    "title": "Chest Day",
    "notes": "Good pump today",
    "exercises": [
        {
            "name": "Bench Press",
            "category": "CHEST",
            "sets": [
                {"reps": 10, "weight": 100},
                {"reps": 8, "weight": 110}
            ]
        }
    ]
}

Response (201):
{
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "date": "2026-03-15",
    "title": "Chest Day",
    "notes": "Good pump today",
    "exercises": [
        {
            "id": "770e8400-e29b-41d4-a716-446655440002",
            "name": "Bench Press",
            "category": "CHEST",
            "sets": [
                {"id": "...", "reps": 10, "weight": 100.0},
                {"id": "...", "reps": 8, "weight": 110.0}
            ]
        }
    ],
    "createdAt": "2026-03-15T10:00:00Z"
}

Error (401): {"error": "Unauthorized"}
Error (422): {"error": "Workout must contain at least one exercise"}
```

#### Get User Workouts

```
GET /api/workouts?page=0&size=20&sortBy=date&direction=DESC
Authorization: Bearer <access_token>

Response (200):
{
    "content": [
        {
            "id": "660e8400-e29b-41d4-a716-446655440001",
            "date": "2026-03-15",
            "title": "Chest Day",
            "exercises": [...]
        }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
}

Error (401): {"error": "Unauthorized"}
```

#### Get Workout by ID

```
GET /api/workouts/{id}
Authorization: Bearer <access_token>

Response (200):
{...workout details...}

Error (401): {"error": "Unauthorized"}
Error (404): {"error": "Workout not found"}
Error (403): {"error": "Not authorized to view this workout"}
```

#### Update Workout

```
PUT /api/workouts/{id}
Authorization: Bearer <access_token>
Content-Type: application/json

Request:
{
    "title": "Updated Chest Day",
    "notes": "Added another set"
}

Response (200):
{...updated workout...}

Error (401): {"error": "Unauthorized"}
Error (403): {"error": "Not authorized to update this workout"}
```

#### Delete Workout

```
DELETE /api/workouts/{id}
Authorization: Bearer <access_token>

Response (204): No Content

Error (401): {"error": "Unauthorized"}
Error (403): {"error": "Not authorized to delete this workout"}
Error (404): {"error": "Workout not found"}
```

---

## 🔄 Data Flow

### Request Flow (Authenticated)

```
1. iOS App sends HTTP Request
   GET /api/workouts
   Authorization: Bearer eyJhbGc...

2. Spring receives request
   ↓ JwtAuthenticationFilter intercepts

3. Extract token from Authorization header

4. Validate JWT signature using secret key
   (Proves token wasn't tampered with)

5. Check token expiry
   (Is current time < token.exp ?)

6. Extract user ID from token payload
   (UUID from payload.sub)

7. Set SecurityContext with authenticated user
   (Spring Security context available throughout request)

8. Forward to WorkoutController.getWorkouts()

9. Service calls: WorkoutService.getUserWorkouts(userId)

10. Repository query: WorkoutRepository.findByUserId(userId)
    (Only fetch workouts for authenticated user)

11. Return list of workouts

12. Serialize to JSON

13. Return HTTP 200 + JSON response

14. iOS app stores response, displays to user
```

### Registration Flow (Silent)

```
1. iOS: User enters email, password, profile
   → POST /api/auth/register

2. AuthController.register(RegisterRequest req)

3. AuthService.register(RegisterRequest)
   ├─ Check if email exists
   │  (Prevent duplicates)
   │
   ├─ Hash password using bcrypt
   │  "SecurePass123" → "$2a$12$N9qo8u..."
   │
   ├─ Create User entity
   │  (Constructor validates all invariants)
   │
   ├─ Save to database
   │  INSERT INTO users (id, email, hashed_password, ...)
   │
   ├─ Generate Access Token (15 min)
   │  Payload: {sub: userId, email, iat, exp}
   │  Signed with secret key
   │
   ├─ Generate Refresh Token (30 days)
   │  Payload: {sub: userId, email, iat, exp}
   │  Signed with secret key
   │
   └─ Return AuthResponse with tokens

4. AuthController returns HTTP 201 + AuthResponse

5. iOS app:
   ├─ Stores Access Token in memory
   ├─ Stores Refresh Token in Keychain
   └─ Navigates to home screen
```

---

## 🔐 Security Model

### Authentication (How you PROVE who you are)

1. **Registration**: Hash password → Store hash
2. **Login**: Compare input hash with stored hash
3. **Token**: Server issues JWT with user ID
4. **Request**: Client sends JWT with each request
5. **Validation**: Server validates JWT signature

### Authorization (What you're ALLOWED to do)

1. **Public Endpoints**: `/api/auth/*` - no token needed
2. **Protected Endpoints**: `/api/workouts`, `/api/users/*` - token required
3. **User Isolation**: Can only access own workouts
4. **Error Handling**: Same error for "not found" and "not authorized"

### Password Security

- **Algorithm**: bcrypt (industry standard)
- **Iterations**: 12 (slows down brute force)
- **Never in logs**: Hash only
- **Never retrievable**: Hash one-way

### Token Security

- **Type**: JWT (JSON Web Token)
- **Signature**: HS512 (HMAC-SHA512)
- **Secret**: Environment variable (production)
- **Access TTL**: 15 minutes (limited exposure)
- **Refresh TTL**: 30 days (long use, secure storage)

### Data Security

- **In Transit**: HTTPS only (enforced in production)
- **At Rest**: PostgreSQL on Neon (managed encryption)
- **User Isolation**: User can't query others' workouts via SQL
- **No SQL Injection**: ORM (JPA) + parameterized queries

---

## 🚀 Implementation Roadmap

### Phase 1: Authentication Foundation (Days 1-3)

**Iteration 1.1**: Model Updates

- [ ] Add email, hashedPassword, createdAt, updatedAt to User
- [ ] Create DTOs (RegisterRequest, LoginRequest, AuthResponse, etc.)
- [ ] Migrate database schema

**Iteration 1.2**: Security Components

- [ ] Create PasswordEncoderComponent (bcrypt)
- [ ] Create JwtTokenProvider (generate/validate tokens)
- [ ] Test token generation and validation

**Iteration 1.3**: Services

- [ ] Create AuthService (register, login, refresh, validate)
- [ ] Update UserRepository (findByEmail, existsByEmail)
- [ ] Write unit tests

### Phase 2: REST API & Filters (Days 4-5)

**Iteration 2.1**: Security Infrastructure

- [ ] Create JwtAuthenticationFilter
- [ ] Create SecurityConfig
- [ ] Create GlobalExceptionHandler

**Iteration 2.2**: Controllers

- [ ] Create AuthController (/auth/register, /login, /refresh)
- [ ] Create UserController (/users/me, PUT /users/me)
- [ ] Integration tests

**Iteration 2.3**: Protection

- [ ] Protect WorkoutController endpoints
- [ ] Enforce user isolation
- [ ] Test with multiple users

### Phase 3: Validation & Error Handling (Days 6-7)

**Iteration 3.1**: Input Validation

- [ ] Add @Valid annotations
- [ ] Implement constraint validators
- [ ] Test invalid inputs

**Iteration 3.2**: Error Handling

- [ ] Standardize error responses
- [ ] HTTP status codes correct
- [ ] Security: no info leakage

**Iteration 3.3**: Logging & Monitoring

- [ ] Log auth events
- [ ] Log security events
- [ ] Add request/response logging

### Phase 4: Production Readiness (Days 8)

**Iteration 4.1**: Configuration

- [ ] Environment-based config
- [ ] Secrets management
- [ ] Database migration scripts

**Iteration 4.2**: Deployment

- [ ] Render deployment
- [ ] Neon database setup
- [ ] CI/CD pipeline

**Iteration 4.3**: Documentation

- [ ] API documentation (Swagger)
- [ ] Deployment guide
- [ ] Troubleshooting guide

---

## 🌍 Deployment Strategy

### Development Environment

**Database**: Local PostgreSQL  
**Server**: `localhost:8080`  
**Config**: `application.properties`

### Production Environment

**Database**: Neon (Managed PostgreSQL)  
**Server**: Render (PaaS)  
**Config**: Environment variables  
**SSL**: Auto-managed via Render  
**CDN**: Cloudflare (optional, later)

### Environment Variables (Production)

```bash
JWT_SECRET=your-secret-key-min-32-chars
JWT_ACCESS_TOKEN_EXPIRY=900
JWT_REFRESH_TOKEN_EXPIRY=2592000
DATABASE_URL=postgresql://user:pass@neon-server/gymtracker
CORS_ALLOWED_ORIGINS=https://gymtracker.example.com
SPRING_PROFILE=prod
```

### Deployment Flow

```
1. Developer pushes to main branch

2. CI/CD pipeline runs
   ├─ Compile code
   ├─ Run tests
   ├─ Build Docker image
   └─ Push to registry

3. Render deploys
   ├─ Pull image
   ├─ Run migrations
   ├─ Start application
   └─ Health check

4. Neon database receives connections

5. DNS points to Render Dyno

6. HTTPS terminates at edge

7. Request reaches Spring Boot

8. Response goes back through CDN

9. iOS app receives response
```

---

## ✅ Acceptance Criteria (All Phases)

- [ ] User can register with email/password (Phase 1)
- [ ] User receives access + refresh tokens (Phase 1)
- [ ] Tokens are stored securely (Phase 1)
- [ ] User can login (Phase 1)
- [ ] Access token expires after 15 min (Phase 1)
- [ ] User can refresh token (Phase 1)
- [ ] Protected endpoints return 401 without token (Phase 2)
- [ ] Users can only see their own workouts (Phase 2)
- [ ] Workouts can be created/read/updated/deleted (Phase 2)
- [ ] Passwords are bcrypt hashed in database (Phase 1)
- [ ] Email validation prevents duplicates (Phase 1)
- [ ] All endpoints have proper error handling (Phase 3)
- [ ] Tests pass (unit + integration) (Phases 1-3)
- [ ] App deploys to production (Phase 4)
- [ ] iOS app can connect to production API (Phase 4)

---

## 🎓 Key Concepts

### Layers Recap

- **Presentation**: HTTP interface, DTOs, error handling
- **Application**: Business orchestration, transactions
- **Domain**: Business rules, entity invariants, repositories (interfaces)
- **Infrastructure**: Implementations, security, database

### Why This Design?

- **Testability**: Mock repositories, test services independently
- **Maintainability**: Changes to one layer don't break others
- **Security**: Sensitive data validation in domain, not controllers
- **Scalability**: Easy to add caching, queuing, etc.

### Production Checklist

- [ ] Secrets in environment, not code
- [ ] Passwords hashed (bcrypt 12+)
- [ ] Tokens signed and TTL-limited
- [ ] HTTPS enforced
- [ ] User isolation enforced
- [ ] Rate limiting (future)
- [ ] Monitoring/logging
- [ ] Backups enabled

---

**Document Version**: 1.0  
**Status**: Ready for Implementation  
**Next**: Begin Phase 1 Day 1
