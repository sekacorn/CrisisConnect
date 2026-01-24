describe('Dashboard Navigation', () => {
  beforeEach(() => {
    // Mock login as different user roles
    const mockToken = 'mock-jwt-token';
    localStorage.setItem('token', mockToken);
  });

  it('should display dashboard for admin user', () => {
    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'admin-id',
        name: 'Admin User',
        email: 'admin@crisisconnect.org',
        role: 'ADMIN',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.visit('/dashboard');

    cy.contains('CrisisConnect Dashboard').should('be.visible');
    cy.contains('Admin User').should('be.visible');
    cy.contains('(ADMIN)').should('be.visible');
    cy.contains('Admin Panel').should('be.visible');
  });

  it('should display dashboard for field worker', () => {
    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'worker-id',
        name: 'Field Worker',
        email: 'worker@crisisconnect.org',
        role: 'FIELD_WORKER',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.visit('/dashboard');

    cy.contains('CrisisConnect Dashboard').should('be.visible');
    cy.contains('Field Worker').should('be.visible');
    cy.contains('(FIELD_WORKER)').should('be.visible');
    cy.contains('Create Need').should('be.visible');
  });

  it('should navigate to needs list', () => {
    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'user-id',
        name: 'Test User',
        email: 'user@crisisconnect.org',
        role: 'FIELD_WORKER',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.intercept('GET', `${Cypress.env('apiUrl')}/needs`, {
      statusCode: 200,
      body: [],
    }).as('getNeedsRequest');

    cy.visit('/dashboard');
    cy.contains('View Needs').click({ force: true });

    cy.url().should('include', '/needs');
  });

  it('should navigate to create need', () => {
    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'worker-id',
        name: 'Field Worker',
        email: 'worker@crisisconnect.org',
        role: 'FIELD_WORKER',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.visit('/dashboard');
    cy.contains('Create Need').click({ force: true });

    cy.url().should('include', '/needs/create');
  });

  it('should logout successfully', () => {
    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'user-id',
        name: 'Test User',
        email: 'user@crisisconnect.org',
        role: 'FIELD_WORKER',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.visit('/dashboard');
    cy.contains('Logout').click({ force: true });

    cy.url().should('include', '/login');
  });

  it('should display security information', () => {
    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'user-id',
        name: 'Test User',
        email: 'user@crisisconnect.org',
        role: 'ADMIN',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.visit('/dashboard');

    cy.contains('Security & Privacy').should('be.visible');
    cy.contains('All sensitive data is encrypted at rest').should('be.visible');
    cy.contains('GDPR, CCPA, and HIPAA compliant').should('be.visible');
  });

  it('should not display admin panel for non-admin users', () => {
    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'worker-id',
        name: 'Field Worker',
        email: 'worker@crisisconnect.org',
        role: 'FIELD_WORKER',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.visit('/dashboard');

    cy.contains('Admin Panel').should('not.exist');
  });
});
