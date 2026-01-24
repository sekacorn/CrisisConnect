# Project Structure

Complete overview of the CrisisConnect codebase organization.

## Root Directory

```
crisisconnect/
├── backend/                 # Spring Boot backend application
├── frontend/                # React TypeScript frontend
├── e2e/                     # End-to-end Cypress tests
├── docs/                    # Additional documentation
│   ├── PRIVACY.md          # Privacy policy
│   ├── DATA_RETENTION.md   # Data retention policy
│   └── PROJECT_STRUCTURE.md # This file
├── spec/                    # Technical specifications
│   ├── 00_product_vision.md
│   ├── 10_domain_model.md
│   ├── 20_roles_rbac.md
│   ├── 30_privacy_redaction.md
│   ├── 35_safeguarding_abuse_prevention.md
│   ├── 40_security_threat_model.md
│   └── 95_quality_assurance.md
├── .env.example             # Environment variables template
├── .gitignore              # Git ignore rules
├── docker-compose.yml      # Docker orchestration
├── start-all.sh/bat        # Start all services
├── start-backend.sh/bat    # Start backend only
├── start-frontend.sh/bat   # Start frontend only
├── start-demo.sh/bat       # Start demo mode
├── stop-all.sh/bat         # Stop all services
├── stop-all.sh/bat     # Stop backend only
├── stop-all.sh/bat    # Stop frontend only
├── stop-all.sh/bat        # Stop demo mode
├── test.sh/bat             # Test runner scripts
├── README.md               # Main documentation
├── CONTRIBUTING.md         # Contribution guidelines
├── SECURITY.md             # Security policies
├── TESTING.md              # Testing documentation
├── CHANGELOG.md            # Version history
└── LICENSE                 # MIT License
```

## Backend Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/org/crisisconnect/
│   │   │   ├── CrisisConnectApplication.java    # Main entry point
│   │   │   ├── controller/                      # REST API controllers
│   │   │   │   ├── AuthController.java          # Authentication endpoints
│   │   │   │   ├── NeedController.java          # Need management endpoints
│   │   │   │   └── OrganizationController.java  # Organization endpoints
│   │   │   ├── dto/                             # Data Transfer Objects
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── LoginResponse.java
│   │   │   │   ├── CreateNeedRequest.java
│   │   │   │   ├── UpdateNeedRequest.java
│   │   │   │   ├── RedactedNeedResponse.java
│   │   │   │   ├── FullNeedResponse.java
│   │   │   │   └── OrganizationResponse.java
│   │   │   ├── model/                           # Domain models
│   │   │   │   ├── entity/                      # JPA entities
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Organization.java
│   │   │   │   │   ├── ServiceArea.java
│   │   │   │   │   ├── Need.java
│   │   │   │   │   ├── SensitiveInfo.java
│   │   │   │   │   ├── NeedUpdate.java
│   │   │   │   │   └── AuditLog.java
│   │   │   │   └── enums/                       # Enumerations
│   │   │   │       ├── UserRole.java
│   │   │   │       ├── NeedStatus.java
│   │   │   │       ├── NeedCategory.java
│   │   │   │       ├── OrganizationType.java
│   │   │   │       ├── OrganizationStatus.java
│   │   │   │       └── UrgencyLevel.java
│   │   │   ├── repository/                      # Data access layer
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── OrganizationRepository.java
│   │   │   │   ├── ServiceAreaRepository.java
│   │   │   │   ├── NeedRepository.java
│   │   │   │   ├── NeedUpdateRepository.java
│   │   │   │   ├── SensitiveInfoRepository.java
│   │   │   │   └── AuditLogRepository.java
│   │   │   ├── security/                        # Security configuration
│   │   │   │   ├── JwtUtil.java                 # JWT token utilities
│   │   │   │   ├── JwtAuthenticationFilter.java # JWT filter
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   └── SecurityConfig.java          # Spring Security config
│   │   │   └── service/                         # Business logic
│   │   │       ├── AuthService.java             # Authentication service
│   │   │       ├── NeedService.java             # Need CRUD with RBAC
│   │   │       ├── EncryptionService.java       # PII encryption
│   │   │       ├── AuditService.java            # Audit logging
│   │   │       └── BootstrapService.java        # Initial setup
│   │   └── resources/
│   │       ├── application.yml                   # Application configuration
│   │       └── db/migration/                     # Flyway migrations
│   │           └── V1__initial_schema.sql
│   └── test/
│       ├── java/org/crisisconnect/
│       │   ├── controller/
│       │   │   └── AuthControllerTest.java
│       │   ├── security/
│       │   │   └── JwtUtilTest.java
│       │   └── service/
│       │       ├── EncryptionServiceTest.java
│       │       └── NeedServiceTest.java
│       └── resources/
│           └── application-test.yml              # Test configuration
├── Dockerfile                                    # Backend Docker image
└── pom.xml                                       # Maven configuration
```

## Frontend Structure

```
frontend/
├── public/
│   └── index.html                               # HTML template
├── src/
│   ├── context/                                 # React contexts
│   │   ├── AuthContext.tsx                      # Authentication context
│   │   └── AuthContext.test.tsx                 # Context tests
│   ├── pages/                                   # Page components
│   │   ├── Login.tsx                            # Login page
│   │   ├── Login.test.tsx
│   │   ├── Dashboard.tsx                        # Dashboard page
│   │   ├── NeedsList.tsx                        # Needs list page
│   │   └── CreateNeed.tsx                       # Create need form
│   ├── services/                                # API services
│   │   ├── api.ts                               # API client
│   │   └── api.test.ts
│   ├── types/                                   # TypeScript types
│   │   └── index.ts                             # Type definitions
│   ├── App.tsx                                  # Main app component
│   ├── index.tsx                                # Entry point
│   ├── index.css                                # Global styles
│   └── setupTests.ts                            # Test setup
├── .env.example                                 # Environment template
├── Dockerfile                                   # Frontend Docker image
├── nginx.conf                                   # Nginx configuration
├── package.json                                 # NPM dependencies
└── tsconfig.json                                # TypeScript config
```

## E2E Test Structure

```
e2e/
├── cypress/
│   ├── e2e/                                     # Test specs
│   │   ├── login.cy.ts                          # Login tests
│   │   └── needs.cy.ts                          # Needs management tests
│   ├── fixtures/                                # Test data
│   └── support/
│       └── e2e.ts                               # Custom commands
├── cypress.config.ts                            # Cypress configuration
├── package.json                                 # NPM dependencies
└── tsconfig.json                                # TypeScript config
```

## Key Components Explained

### Backend

#### Controllers
Handle HTTP requests and responses. Implement input validation and delegate to services.

#### Services
Contain business logic, RBAC enforcement, and privacy filtering. Core application logic resides here.

#### Repositories
Data access layer using Spring Data JPA. Simple CRUD operations and custom queries.

#### Security
JWT-based authentication, Spring Security configuration, and password encryption.

#### Models
- **Entities**: JPA-mapped database tables
- **DTOs**: API request/response objects
- **Enums**: Type-safe constants

### Frontend

#### Context
React context for global state management (authentication, user info).

#### Pages
Top-level route components representing full pages.

#### Services
API communication layer with automatic token injection.

#### Types
TypeScript interfaces and enums matching backend DTOs.

### Testing

#### Unit Tests
- **Backend**: JUnit + Mockito
- **Frontend**: Jest + React Testing Library

#### E2E Tests
Cypress tests for complete user workflows.

## Configuration Files

### Environment Configuration

```
.env                    # Local environment variables (not committed)
.env.example           # Template for environment variables
```

### Docker Configuration

```
docker-compose.yml     # Multi-container orchestration
backend/Dockerfile     # Backend image definition
frontend/Dockerfile    # Frontend image definition
frontend/nginx.conf    # Frontend web server config
```

### Build Configuration

```
backend/pom.xml        # Maven build configuration
frontend/package.json  # NPM build configuration
frontend/tsconfig.json # TypeScript compiler options
```

### Test Configuration

```
backend/src/test/resources/application-test.yml
frontend/src/setupTests.ts
e2e/cypress.config.ts
```

## Data Flow

### Authentication Flow

```
User Input (Login.tsx)
  → API Client (api.ts)
    → Auth Controller
      → Auth Service
        → User Repository
          → PostgreSQL
      ← JWT Token
    ← Login Response
  ← Store token in localStorage
← Update Auth Context
```

### Need Creation Flow

```
User Input (CreateNeed.tsx)
  → API Client
    → Need Controller (validate JWT)
      → Need Service (check RBAC)
        → Encryption Service (encrypt PII)
        → Need Repository
        → Sensitive Info Repository
        → Audit Service (log action)
      ← Full Need Response
    ← API Response
  ← Navigate to needs list
```

### Privacy Filtering Flow

```
GET /needs/{id}
  → Need Controller
    → Need Service
      → Check user permissions
        → User has full access?
          → YES: Return FullNeedResponse + decrypt PII
          → NO: Return RedactedNeedResponse
      → Audit Service (log access level)
    ← Response (Full or Redacted)
```

## File Naming Conventions

### Backend (Java)
- **Classes**: PascalCase (e.g., `UserService.java`)
- **Packages**: lowercase (e.g., `org.crisisconnect.service`)
- **Tests**: Class name + `Test` (e.g., `UserServiceTest.java`)

### Frontend (TypeScript)
- **Components**: PascalCase (e.g., `Login.tsx`)
- **Services**: camelCase (e.g., `api.ts`)
- **Types**: PascalCase (e.g., `UserRole`)
- **Tests**: File name + `.test` (e.g., `Login.test.tsx`)

### E2E (Cypress)
- **Tests**: Feature name + `.cy.ts` (e.g., `login.cy.ts`)

## Build Artifacts

### Generated Directories

```
backend/target/        # Compiled backend code
frontend/build/        # Production build
frontend/node_modules/ # Frontend dependencies
e2e/node_modules/      # E2E test dependencies
```

### Runtime Files

```
backend.pid            # Backend process ID
frontend.pid           # Frontend process ID
demo.pid               # Demo mode process ID
backend.log            # Backend application logs
frontend.log           # Frontend application logs
demo.log               # Demo mode application logs
```

## Port Assignments

| Service | Port | Purpose |
|---------|------|---------|
| Frontend Dev Server | 3000 | React development server |
| Backend API | 8080 | Spring Boot application |
| PostgreSQL | 5432 | Database server |
| Frontend Production | 80 | Nginx serving static files |

## Database Schema

See migration file: `backend/src/main/resources/db/migration/V1__initial_schema.sql`

Tables:
- `users` - System users
- `organizations` - NGOs and agencies
- `service_areas` - Geographic coverage
- `service_area_categories` - Category mappings
- `needs` - Assistance requests
- `sensitive_info` - Encrypted PII
- `need_updates` - Status change history
- `audit_logs` - Security audit trail

## Security Layers

1. **Network**: HTTPS/TLS encryption
2. **Authentication**: JWT tokens
3. **Authorization**: Role-based access control
4. **Data**: AES-256 encryption for PII
5. **Audit**: Comprehensive logging
6. **Privacy**: Redacted responses by default

---

This structure supports security, scalability, and maintainability while protecting vulnerable populations.
