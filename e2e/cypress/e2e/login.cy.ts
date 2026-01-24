describe('Login Flow', () => {
  beforeEach(() => {
    cy.visit('/login');
  });

  it('should display login page', () => {
    cy.contains('CrisisConnect').should('be.visible');
    cy.contains('Humanitarian Aid Coordination Platform').should('be.visible');
    cy.get('input[type="email"]').should('be.visible');
    cy.get('input[type="password"]').should('be.visible');
    cy.get('button[type="submit"]').should('be.visible');
  });

  it('should show validation errors for empty fields', () => {
    cy.get('button[type="submit"]').click();
    cy.get('input[type="email"]:invalid').should('exist');
    cy.get('input[type="password"]:invalid').should('exist');
  });

  it('should show error for invalid credentials', () => {
    cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/login`, {
      statusCode: 401,
      body: { message: 'Invalid credentials' },
    }).as('loginRequest');

    cy.get('input[type="email"]').type('invalid@example.com', { force: true });
    cy.get('input[type="password"]').type('wrongpassword', { force: true });
    cy.get('button[type="submit"]').click({ force: true });

    cy.wait('@loginRequest');
    cy.contains('Login failed. Please check your credentials.').should('be.visible');
  });

  it('should login successfully with valid credentials', () => {
    const mockResponse = {
      token: 'mock-jwt-token',
      type: 'Bearer',
      userId: 'test-user-id',
      email: 'admin@crisisconnect.org',
      name: 'Admin User',
      role: 'ADMIN',
    };

    cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/login`, {
      statusCode: 200,
      body: mockResponse,
    }).as('loginRequest');

    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'test-user-id',
        name: 'Admin User',
        email: 'admin@crisisconnect.org',
        role: 'ADMIN',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.get('input[type="email"]').type('admin@crisisconnect.org', { force: true });
    cy.get('input[type="password"]').type('Admin123!', { force: true });
    cy.get('button[type="submit"]').click({ force: true });

    cy.wait('@loginRequest');
    cy.url().should('include', '/dashboard');
  });
});
