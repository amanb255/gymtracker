# GymTracker Authentication & Authorization System Design

**Version**: 1.0  
**Date**: March 15, 2026  
**Status**: Design Phase (Ready for Implementation)

---

## 📋 Table of Contents

1. [System Overview](#system-overview)
2. [Entity Models](#entity-models)
3. [New Classes & Components](#new-classes--components)
4. [API Endpoints](#api-endpoints)
5. [Authentication Flows](#authentication-flows)
6. [Database Schema](#database-schema)
7. [Security Considerations](#security-considerations)
8. [Implementation Sequence](#implementation-sequence)

---

## 🎯 System Overview

### Architecture

- **Authentication Method**: JWT (JSON Web Token) with OAuth2-inspired flow
- **Password Security**: bcrypt hashing (Spring Security's PasswordEncoder)
- **Token Strategy**: Dual-token (Access Token + Refresh Token)
- **Authorization**: Role-based (currently just authenticated users; extend later with USER, ADMIN roles)

### Key Decisions

- **Stateless authentication**: Access tokens validated via signature, no session storage
- **Refresh tokens**: Stateless (no DB storage for now; can optimize later)
- **Email as unique identifier**: Users register/login with email
- **No external OAuth providers initially**: Just email/password. Can add Google/GitHub login later.

---

## 🏗️ Entity Models

### Updated User Entity

**Location**: `com.gymtracker.domain.model.User.java`

**Changes**:

1. Add `email` field (unique, indexed)
2. Add `hashedPassword` field (never store plain text)
3. Add `createdAt` timestamp
4. Add `updatedAt` timestamp (optional, for audit)
5. Remove password setters, add secure methods instead

**Fields**:

```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class User {

    // Existing fields
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer heightCm;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    // NEW fields
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String hashedPassword;  // bcrypt format: $2a$10$...

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime updatedAt;

    // Existing relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Workout> workouts;
}
```

**Validation Rules**:

- Email: must be valid format, not null, unique
- HashedPassword: must be non-empty, bcrypt format
- Other fields: same as before

**Methods**:

```java
// Constructor: register flow
public User(String name, LocalDate dateOfBirth, Integer heightCm,
            Gender gender, String email, String hashedPassword)

// Getters (no setters for password!)
public String getEmail()
public LocalDateTime getCreatedAt()
public LocalDateTime getUpdatedAt()

// Domain methods
public void updateName(String newName)
public void updateHeight(Integer newHeightCm)
public void updateGender(Gender newGender)
public void updateEmail(String newEmail)  // for future use
public void setUpdatedAt(LocalDateTime timestamp)  // internal use only
```

---

## 🛠️ New Classes & Components

### 1. DTOs (Data Transfer Objects)

**Location**: `com.gymtracker.presentation.dto`

#### RegisterRequest

```java
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    private String password;

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @NotNull(message = "Date of birth is required")
    @PastOrPresent(message = "Date of birth cannot be in future")
    private LocalDate dateOfBirth;

    @NotNull(message = "Height is required")
    @Positive(message = "Height must be positive")
    private Integer heightCm;

    @NotNull(message = "Gender is required")
    private Gender gender;
}
```

#### LoginRequest

```java
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
```

#### AuthResponse

```java
public class AuthResponse {
    private String accessToken;      // Valid for 15 minutes
    private String refreshToken;     // Valid for 30 days
    private UUID userId;
    private String email;
    private String name;
    private Long expiresIn;          // seconds until access token expires

    // Getters/Setters
}
```

#### RefreshTokenRequest

```java
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
```

### 2. JWT Token Provider

**Location**: `com.gymtracker.infrastructure.security.JwtTokenProvider.java`

**Responsibilities**:

- Generate Access Token
- Generate Refresh Token
- Validate token signature
- Extract user ID from token
- Check token expiry

```java
@Component
public class JwtTokenProvider {

    // Configuration (from application.properties)
    @Value("${jwt.secret:your-secret-key-min-32-chars-for-production}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiry:900}")  // 15 minutes in seconds
    private long accessTokenExpiry;

    @Value("${jwt.refresh-token-expiry:2592000}")  // 30 days in seconds
    private long refreshTokenExpiry;

    // Methods
    public String generateAccessToken(UUID userId, String email)
    public String generateRefreshToken(UUID userId, String email)
    public UUID getUserIdFromToken(String token)
    public String getEmailFromToken(String token)
    public boolean validateToken(String token)
    public long getExpiryTime(String token)
}
```

**Implementation Details**:

- Algorithm: HS512 (HMAC-SHA512)
- Payload includes: `sub` (userId), `email`, `iat`, `exp`
- Secret key: minimum 32 characters (production requirement)

### 3. Password Encoder

**Location**: `com.gymtracker.infrastructure.security.PasswordEncoder.java`

**Responsibilities**:

- Hash plain passwords using bcrypt
- Verify password against hash

```java
@Component
public class PasswordEncoderComponent {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // strength 12 iterations
    }

    public String encode(String rawPassword)      // returns bcrypt hash
    public boolean matches(String raw, String hash) // compares password
}
```

### 4. AuthService

**Location**: `com.gymtracker.application.AuthService.java`

**Responsibilities**:

- Handle user registration
- Handle user login
- Handle token refresh
- Validate tokens
- Check email uniqueness

```java
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;

    // Methods
    public User register(RegisterRequest request)  // throws if email exists
    public AuthResponse login(LoginRequest request) // throws if invalid
    public AuthResponse refreshToken(String refreshToken)
    public boolean validateToken(String token)
    public UUID extractUserIdFromToken(String token)
}
```

### 5. Authentication Filter

**Location**: `com.gymtracker.infrastructure.security.JwtAuthenticationFilter.java`

**Responsibilities**:

- Intercept HTTP requests
- Extract JWT from `Authorization` header
- Validate JWT
- Set `SecurityContext` with authenticated user

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain)

    // Extracts token from: Authorization: Bearer <token>
    // Validates and sets security context
}
```

### 6. Security Configuration

**Location**: `com.gymtracker.infrastructure.config.SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)

    // Configures:
    // - Which endpoints are public (register, login, refresh)
    // - Which endpoints require authentication
    // - JWT filter chain
    // - CORS settings
}
```

---

## 🌐 API Endpoints

### Authentication Endpoints

#### 1. Register

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

Response (201 Created):
{
    "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john@example.com",
    "name": "John Doe",
    "expiresIn": 900
}

Error (400 Bad Request):
{
    "error": "Email already registered"
}

Error (400 Bad Request):
{
    "error": "Password must be at least 8 characters"
}
```

#### 2. Login

```
POST /api/auth/login
Content-Type: application/json

Request:
{
    "email": "john@example.com",
    "password": "SecurePass123!"
}

Response (200 OK):
{
    "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john@example.com",
    "name": "John Doe",
    "expiresIn": 900
}

Error (401 Unauthorized):
{
    "error": "Invalid email or password"
}
```

#### 3. Refresh Token

```
POST /api/auth/refresh
Content-Type: application/json

Request:
{
    "refreshToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9..."
}

Response (200 OK):
{
    "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 900
}

Error (401 Unauthorized):
{
    "error": "Invalid or expired refresh token"
}
```

### Protected Endpoints (Examples)

#### Get User Profile

```
GET /api/users/me
Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...

Response (200 OK):
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john@example.com",
    "name": "John Doe",
    "dateOfBirth": "1990-05-15",
    "heightCm": 180,
    "gender": "MALE",
    "createdAt": "2026-03-15T10:00:00Z"
}

Error (401 Unauthorized):
{
    "error": "Invalid or missing authorization token"
}
```

#### Get User Workouts

```
GET /api/workouts
Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...

Response (200 OK):
[
    {
        "id": "...",
        "date": "2026-03-15",
        "title": "Chest Day",
        "exercises": [...]
    }
]

Return ONLY current user's workouts (enforced by authentication filter)
```

---

## 🔄 Authentication Flows

### Flow 1: User Registration

```
┌─────────────┐
│   iOS App   │
└──────┬──────┘
       │ POST /api/auth/register
       │ {email, password, name, dob, height, gender}
       ▼
┌──────────────────────────────────────┐
│      AuthController.register()       │
└──────┬───────────────────────────────┘
       │ Forward request
       ▼
┌──────────────────────────────────────┐
│      AuthService.register()          │
└──────┬───────────────────────────────┘
       │
       ├─ Check if email exists
       │  (SELECT * FROM users WHERE email = ?)
       │
       ├─ If exists: throw EmailAlreadyRegisteredException
       │
       ├─ If not: Hash password using bcrypt
       │  password: "SecurePass123"
       │  → hash: "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxW..."
       │
       ├─ Create new User entity
       │  user = new User(name, dob, height, gender, email, hashedPassword)
       │
       ├─ Save to database
       │
       ├─ Generate Access Token (15 min expiry)
       │  payload: {sub: userId, email, iat, exp}
       │  → "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9..."
       │
       ├─ Generate Refresh Token (30 day expiry)
       │  payload: {sub: userId, email, iat, exp}
       │  → "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9..."
       │
       └─ Return AuthResponse with both tokens
       │
       ▼
┌──────────────────────────────────────┐
│      iOS App                         │
│  store accessToken (memory)          │
│  store refreshToken (Keychain)       │
└──────────────────────────────────────┘
```

### Flow 2: User Login

```
┌─────────────┐
│   iOS App   │
└──────┬──────┘
       │ POST /api/auth/login
       │ {email, password}
       ▼
┌──────────────────────────────────────┐
│      AuthController.login()          │
└──────┬───────────────────────────────┘
       │ Forward request
       ▼
┌──────────────────────────────────────┐
│      AuthService.login()             │
└──────┬───────────────────────────────┘
       │
       ├─ Find user by email
       │  (SELECT * FROM users WHERE email = ?)
       │
       ├─ If not found: throw InvalidCredentialsException
       │
       ├─ If found: Compare passwords using bcrypt
       │  Input: "SecurePass123"
       │  Stored: "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxW..."
       │  bcrypt.matches(input, stored) → true/false
       │
       ├─ If not match: throw InvalidCredentialsException
       │
       ├─ If match: Generate tokens (same as registration)
       │
       └─ Return AuthResponse
       │
       ▼
┌──────────────────────────────────────┐
│      iOS App                         │
│  store accessToken (memory)          │
│  store refreshToken (Keychain)       │
└──────────────────────────────────────┘
```

### Flow 3: Authenticated API Request

```
┌─────────────┐
│   iOS App   │
└──────┬──────┘
       │ GET /api/workouts
       │ Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...
       ▼
┌──────────────────────────────────────┐
│      JwtAuthenticationFilter         │
│      (OncePerRequestFilter)          │
└──────┬───────────────────────────────┘
       │
       ├─ Extract token from header
       │  (Authorization: Bearer <token>)
       │
       ├─ Validate token signature using secret key
       │  (Proves token wasn't tampered with)
       │
       ├─ Check token expiry
       │  (iat + exp compared to current time)
       │
       ├─ Extract user ID from payload
       │  (payload.sub → UUID)
       │
       ├─ If invalid/expired: return 401 Unauthorized
       │
       ├─ If valid: Set SecurityContext
       │  SecurityContextHolder.setContext(
       │    new SecurityContext(userId)
       │  )
       │
       └─ Continue filter chain
       │
       ▼
┌──────────────────────────────────────┐
│      WorkoutController.getWorkouts() │
└──────┬───────────────────────────────┘
       │
       ├─ Get current user ID from SecurityContext
       │
       ├─ Call WorkoutService.getWorkoutsForUser(userId)
       │
       └─ Return only this user's workouts
       │
       ▼
┌──────────────────────────────────────┐
│      iOS App                         │
│ Receives workouts data in response   │
└──────────────────────────────────────┘
```

### Flow 4: Token Refresh

```
┌─────────────┐
│   iOS App   │
│ (Access Token expired after 15 min)
└──────┬──────┘
       │ POST /api/auth/refresh
       │ {refreshToken: "eyJhbGc..."}
       ▼
┌──────────────────────────────────────┐
│      AuthController.refreshToken()   │
└──────┬───────────────────────────────┘
       │ Forward request
       ▼
┌──────────────────────────────────────┐
│      AuthService.refreshToken()      │
└──────┬───────────────────────────────┘
       │
       ├─ Validate refresh token signature
       │
       ├─ If invalid: throw UnauthorizedException
       │
       ├─ If expired: throw UnauthorizedException
       │
       ├─ Extract user ID from token
       │
       ├─ Generate NEW access token (15 min)
       │
       └─ Return new access token
       │
       ▼
┌──────────────────────────────────────┐
│      iOS App                         │
│  store NEW accessToken               │
│  continue using app                  │
└──────────────────────────────────────┘
```

---

## 📊 Database Schema

### Users Table

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    hashed_password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    height_cm INTEGER NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT check_height_positive CHECK (height_cm > 0),
    CONSTRAINT check_email_not_empty CHECK (email != '')
);

-- Indexes for performance
CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_created_at ON users(created_at);
```

### Example Row

```
id                                    | email              | hashed_password                  | name      | height_cm | date_of_birth | gender | created_at          | updated_at
550e8400-e29b-41d4-a716-446655440000 | john@example.com   | $2a$12$N9qo8u...                | John Doe  | 180       | 1990-05-15    | MALE   | 2026-03-15 10:00:00 | NULL
```

---

## 🔐 Security Considerations

### 1. Password Security

- **Hashing Algorithm**: bcrypt with strength 12 iterations
- **Never store plain text**: All passwords hashed before storage
- **No password recovery**: Only password reset via email link (future feature)
- **Minimum length**: 8 characters (enforced at DTO level)

### 2. Token Security

- **Secret Key**: Minimum 32 characters, environment variable (production requirement)
- **Signature Algorithm**: HS512 (HMAC-SHA512)
- **Access Token TTL**: 15 minutes (short-lived, limited exposure)
- **Refresh Token TTL**: 30 days (long-lived, but refresh is secure)
- **Token Storage**:
  - Access Token: In-memory (cleared when app closes)
  - Refresh Token: iOS Keychain (secure enclave)

### 3. API Security

- **HTTPS only**: Enforce in production (environment-based)
- **CORS**: Allow only trusted domains (configure for iOS app domain)
- **Email Validation**: Validate format before processing
- **Rate Limiting**: Add later (prevent brute force login attempts)

### 4. Data Privacy

- **Email Uniqueness**: Prevent duplicate registrations
- **Workouts Isolation**: Users can only see their own workouts
- **No user enumeration**: Same error message for "email not found" and "wrong password"

### 5. Environment-Specific Config

**application.properties** (development):

```properties
jwt.secret=dev-secret-key-min-32-characters-long-for-security
jwt.access-token-expiry=900
jwt.refresh-token-expiry=2592000
cors.allowed-origins=http://localhost:8080
```

**application-prod.properties** (production):

```properties
jwt.secret=${JWT_SECRET}  # Injected from environment variable
jwt.access-token-expiry=900
jwt.refresh-token-expiry=2592000
cors.allowed-origins=${ALLOWED_ORIGINS}
```

---

## 📋 Implementation Sequence

### Phase 1: Model & Infrastructure (Days 1-2)

**Order of Implementation**:

1. **Update User Entity**
   - Add `email`, `hashedPassword`, `createdAt`, `updatedAt` fields
   - Add validation methods
   - Migration: Add columns to DB

2. **Create PasswordEncoder Component**
   - Integrate bcrypt
   - Test password hashing/verification

3. **Create JwtTokenProvider**
   - Implement token generation
   - Implement token validation
   - Test token extraction

4. **Create DTOs**
   - RegisterRequest, LoginRequest, AuthResponse, RefreshTokenRequest
   - Validation annotations

5. **Update UserRepository**
   - Add `findByEmail(String email)` method
   - Add `existsByEmail(String email)` method

### Phase 2: Services & Security (Days 3-4)

6. **Create AuthService**
   - register() method
   - login() method
   - refreshToken() method
   - validateToken() method

7. **Create JwtAuthenticationFilter**
   - Extract token from headers
   - Validate and set SecurityContext

8. **Create SecurityConfig**
   - Configure Spring Security
   - Enable JWT filter
   - Define public vs protected endpoints

### Phase 3: Controllers & Testing (Days 5-6)

9. **Create AuthController**
   - POST /api/auth/register
   - POST /api/auth/login
   - POST /api/auth/refresh
   - Error handling

10. **Create UserController**
    - GET /api/users/me (get authenticated user profile)
    - PUT /api/users/me (update profile)
    - Error handling with authenticated user context

11. **Update WorkoutController**
    - Enforce user isolation (users only see their own workouts)
    - Add authentication checks

12. **Integration Testing**
    - Test registration flow
    - Test login flow
    - Test token refresh
    - Test protected endpoints

### Phase 4: Documentation & Deployment Prep (Days 7)

13. **API Documentation**
    - Swagger/OpenAPI integration
    - Endpoint documentation

14. **Error Handling**
    - Standardized error responses
    - Proper HTTP status codes

15. **Logging**
    - Auth event logging
    - Security audit trail

---

## 🧪 Testing Strategy

### Unit Tests

- PasswordEncoder: hash/verify
- JwtTokenProvider: generate/validate tokens
- AuthService: register/login logic

### Integration Tests

- Register endpoint: valid/invalid inputs
- Login endpoint: valid/invalid credentials
- Token refresh: valid/expired tokens
- Protected endpoints: with/without auth header

### Security Tests

- Password hashing: verify bcrypt
- Token expiry: test TTL
- Email uniqueness: prevent duplicates
- Endpoint access: unauthorized requests blocked

---

## ✅ Acceptance Criteria

When implementation is complete:

- [ ] User can register with email/password
- [ ] User receives access + refresh tokens
- [ ] User can login with email/password
- [ ] Tokens are stored securely (Keychain on iOS)
- [ ] Protected endpoints return 401 without valid token
- [ ] Users can only access their own workouts
- [ ] Access token expires after 15 minutes
- [ ] User can refresh token using refresh token
- [ ] Passwords are bcrypt hashed in database
- [ ] Email validation prevents duplicates
- [ ] All endpoints have proper error handling
- [ ] Tests pass (unit + integration)

---

## 📝 Notes for Implementation

### Gotchas to Avoid

1. Don't expose user IDs in error messages (security)
2. Don't use `@Transactional` on AuthService if not needed (can cause issues with token generation)
3. Ensure JWT secret is NOT hardcoded in production
4. Always hash passwords before saving, never retrieve plain passwords
5. Validate tokens on every protected request

### Future Enhancements

1. Email verification on registration
2. Password reset via email
3. Logout with token blacklist
4. OAuth2 with Google/GitHub
5. Two-factor authentication
6. Account lockout after failed attempts
7. Session management for multiple devices

---

**Document Version**: 1.0  
**Last Updated**: March 15, 2026  
**Next Step**: Begin Phase 1 Implementation
