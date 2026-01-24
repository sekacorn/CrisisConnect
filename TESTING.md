# Testing Guide

This document provides comprehensive information about testing in CrisisConnect.

## Test Strategy

CrisisConnect uses a multi-layered testing approach:

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test interactions between components
3. **End-to-End Tests**: Test complete user workflows

## Backend Testing

### Technology Stack

- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework
- **Spring Boot Test**: Integration testing support
- **H2 Database**: In-memory database for tests

### Running Backend Tests

```bash
cd backend

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=JwtUtilTest

# Run tests with coverage
mvn clean test jacoco:report

# Skip tests
mvn package -DskipTests
```

### Test Structure

```
backend/src/test/java/org/crisisconnect/
├── security/
│   └── JwtUtilTest.java          # JWT token generation and validation
├── service/
│   ├── EncryptionServiceTest.java # PII encryption/decryption
│   └── NeedServiceTest.java       # Need CRUD and RBAC
└── controller/
    └── AuthControllerTest.java    # Authentication endpoints
```

### Writing Backend Tests

Example unit test:

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {

    @Mock
    private MyRepository myRepository;

    @InjectMocks
    private MyService myService;

    @Test
    void testMyMethod() {
        // Arrange
        when(myRepository.findById(any())).thenReturn(Optional.of(entity));

        // Act
        Result result = myService.myMethod(id);

        // Assert
        assertNotNull(result);
        verify(myRepository, times(1)).findById(any());
    }
}
```

### Test Coverage Goals

- **Lines**: > 80%
- **Branches**: > 70%
- **Methods**: > 80%

Focus areas:
- Security components (JWT, encryption)
- Privacy filtering
- RBAC enforcement
- Data validation

## Frontend Testing

### Technology Stack

- **Jest**: Testing framework
- **React Testing Library**: Component testing
- **Testing Library User Event**: User interaction simulation

### Running Frontend Tests

```bash
cd frontend

# Run all tests in watch mode
npm test

# Run all tests once
npm test -- --watchAll=false

# Run tests with coverage
npm test -- --coverage --watchAll=false

# Run specific test file
npm test -- Login.test.tsx

# Update snapshots
npm test -- -u
```

### Test Structure

```
frontend/src/
├── services/
│   └── api.test.ts               # API client
├── context/
│   └── AuthContext.test.tsx      # Authentication context
└── pages/
    └── Login.test.tsx             # Login page
```

### Writing Frontend Tests

Example component test:

```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import MyComponent from './MyComponent';

describe('MyComponent', () => {
  it('should render correctly', () => {
    render(<MyComponent />);
    expect(screen.getByText('Hello')).toBeInTheDocument();
  });

  it('should handle click', () => {
    const handleClick = jest.fn();
    render(<MyComponent onClick={handleClick} />);

    fireEvent.click(screen.getByRole('button'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });
});
```

### Mocking API Calls

```typescript
import { apiClient } from './api';

jest.mock('./api');
const mockedApi = apiClient as jest.Mocked<typeof apiClient>;

test('fetches data', async () => {
  mockedApi.getData.mockResolvedValue({ data: 'test' });

  // Test component that uses apiClient
});
```

## End-to-End Testing

### Technology Stack

- **Cypress**: E2E testing framework

### Running E2E Tests

```bash
cd e2e

# Install dependencies (first time)
npm install

# Run tests headless
npm test

# Open Cypress Test Runner
npm run test:open
```

### Test Structure

```
e2e/cypress/
├── e2e/
│   ├── login.cy.ts               # Login workflows
│   └── needs.cy.ts               # Need management
└── support/
    └── e2e.ts                    # Custom commands
```

### Writing E2E Tests

Example E2E test:

```typescript
describe('Feature Flow', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('should complete workflow', () => {
    // Arrange - Set up test data
    cy.intercept('GET', '/api/data', { fixture: 'data.json' });

    // Act - Perform actions
    cy.get('button').click();
    cy.get('input').type('test value');
    cy.get('form').submit();

    // Assert - Verify results
    cy.url().should('include', '/success');
    cy.contains('Success message').should('be.visible');
  });
});
```

### Custom Cypress Commands

```typescript
// In cypress/support/e2e.ts
Cypress.Commands.add('login', (email: string, password: string) => {
  cy.visit('/login');
  cy.get('input[type="email"]').type(email);
  cy.get('input[type="password"]').type(password);
  cy.get('button[type="submit"]').click();
});

// Use in tests
cy.login('user@example.com', 'password');
```

## Test Data

### Backend Test Data

Test data is created in `@BeforeEach` methods:

```java
@BeforeEach
void setUp() {
    testUser = new User();
    testUser.setEmail("test@example.com");
    testUser.setRole(UserRole.ADMIN);
    // ... more setup
}
```

### Frontend Test Data

Mock data in test files or separate fixtures:

```typescript
const mockUser = {
  id: 'test-id',
  name: 'Test User',
  email: 'test@example.com',
  role: 'ADMIN',
};
```

### E2E Test Data

Cypress fixtures in `e2e/cypress/fixtures/`:

```json
{
  "user": {
    "email": "test@example.com",
    "name": "Test User"
  }
}
```

## Continuous Integration

### GitHub Actions Example

```yaml
name: Tests

on: [push, pull_request]

jobs:
  backend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: cd backend && mvn test

  frontend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - run: cd frontend && npm install && npm test

  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: cypress-io/github-action@v5
        with:
          working-directory: e2e
```

## Best Practices

### General

1. **Test Naming**: Use descriptive names that explain what is being tested
2. **Arrange-Act-Assert**: Structure tests clearly
3. **One Assertion**: Test one thing at a time
4. **Independent Tests**: Tests should not depend on each other
5. **Fast Tests**: Keep tests quick to encourage frequent running

### Backend

1. **Mock External Dependencies**: Use Mockito to mock repositories
2. **Test Security**: Always test authentication and authorization
3. **Test Privacy**: Verify data filtering and encryption
4. **Test Edge Cases**: Invalid inputs, null values, etc.

### Frontend

1. **Test User Behavior**: Test what users see and do
2. **Avoid Implementation Details**: Don't test internal state
3. **Accessible Queries**: Use `getByRole`, `getByLabelText`
4. **User Events**: Prefer `userEvent` over `fireEvent`

### E2E

1. **Test Critical Paths**: Focus on most important user journeys
2. **Use Data Attributes**: Add `data-testid` for reliable selectors
3. **Wait Properly**: Use Cypress automatic waiting
4. **Isolate Tests**: Each test should set up its own state

## Debugging Tests

### Backend

```bash
# Run with debug logging
mvn test -Dlogging.level.org.crisisconnect=DEBUG

# Run in debug mode (attach debugger on port 5005)
mvn test -Dmaven.surefire.debug
```

### Frontend

```bash
# Run tests with debugging
node --inspect-brk node_modules/.bin/jest --runInBand

# Use Chrome DevTools for debugging
```

### E2E

```bash
# Open Cypress with browser DevTools
npm run test:open

# Use cy.debug() in tests
cy.get('button').debug().click();

# Use cy.pause() to pause execution
cy.pause();
```

## Common Issues

### Backend

**Issue**: Tests fail with database errors
**Solution**: Check `application-test.yml` configuration

**Issue**: Mocks not working
**Solution**: Verify `@Mock` and `@InjectMocks` annotations

### Frontend

**Issue**: "Unable to find element"
**Solution**: Use `findBy*` queries for async elements

**Issue**: "Cannot read property of undefined"
**Solution**: Check if component dependencies are mocked

### E2E

**Issue**: Tests timeout
**Solution**: Increase timeout or fix application performance

**Issue**: Flaky tests
**Solution**: Add proper waits, avoid fixed delays

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [React Testing Library](https://testing-library.com/docs/react-testing-library/intro/)
- [Cypress Documentation](https://docs.cypress.io/)
- [Testing Best Practices](https://kentcdodds.com/blog/common-mistakes-with-react-testing-library)
