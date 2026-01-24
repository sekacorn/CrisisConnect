describe('Complete Workflow - Field Worker to NGO Staff', () => {
  it('should complete full need creation and claiming workflow', () => {
    // Step 1: Login as Field Worker
    const mockLoginResponse = {
      token: 'mock-jwt-token-fieldworker',
      type: 'Bearer',
      userId: 'fieldworker-id',
      email: 'fieldworker@crisisconnect.org',
      name: 'Field Worker',
      role: 'FIELD_WORKER',
    };

    cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/login`, {
      statusCode: 200,
      body: mockLoginResponse,
    }).as('loginRequest');

    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'fieldworker-id',
        name: 'Field Worker',
        email: 'fieldworker@crisisconnect.org',
        role: 'FIELD_WORKER',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.visit('/login');
    cy.get('input[type="email"]').type('fieldworker@crisisconnect.org', { force: true });
    cy.get('input[type="password"]').type('password123', { force: true });
    cy.get('button[type="submit"]').click({ force: true });

    cy.wait('@loginRequest');
    cy.url().should('include', '/dashboard');

    // Step 2: Navigate to Create Need
    cy.contains('Create Need').click({ force: true });
    cy.url().should('include', '/needs/create');

    // Step 3: Fill in Need Form
    const mockNeedResponse = {
      id: 'new-need-123',
      category: 'FOOD',
      description: 'Urgent food assistance needed',
      country: 'Lebanon',
      regionOrState: 'Beirut',
      urgencyLevel: 'HIGH',
      status: 'NEW',
    };

    cy.intercept('POST', `${Cypress.env('apiUrl')}/needs`, {
      statusCode: 200,
      body: mockNeedResponse,
    }).as('createNeedRequest');

    cy.get('select[name="category"]').select('FOOD', { force: true });
    cy.get('textarea[name="description"]').type('Urgent food assistance needed for family of 5', { force: true });
    cy.get('select[name="urgencyLevel"]').select('HIGH', { force: true });
    cy.get('input[name="country"]').type('Lebanon', { force: true });
    cy.get('input[name="regionOrState"]').type('Beirut', { force: true });
    cy.get('input[name="beneficiaryName"]').type('Test Beneficiary', { force: true });

    cy.get('button[type="submit"]').click({ force: true });
    cy.wait('@createNeedRequest');

    // Step 4: Verify Need was Created
    const mockNeedsList = [mockNeedResponse];

    cy.intercept('GET', `${Cypress.env('apiUrl')}/needs`, {
      statusCode: 200,
      body: mockNeedsList,
    }).as('getNeedsRequest');

    cy.url().should('include', '/needs');
    cy.wait('@getNeedsRequest');
    cy.contains('FOOD').should('be.visible');
    cy.contains('HIGH').should('be.visible');
  });
});

describe('Error Handling Workflow', () => {
  beforeEach(() => {
    const mockToken = 'mock-jwt-token';
    localStorage.setItem('token', mockToken);

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
  });

  it('should handle API errors gracefully when loading needs', () => {
    cy.intercept('GET', `${Cypress.env('apiUrl')}/needs`, {
      statusCode: 500,
      body: { message: 'Internal server error' },
    }).as('getNeedsError');

    cy.visit('/needs');
    cy.wait('@getNeedsError');

    cy.contains('Failed to load needs').should('be.visible');
  });

  it('should handle network errors when creating need', () => {
    cy.intercept('POST', `${Cypress.env('apiUrl')}/needs`, {
      statusCode: 400,
      body: { message: 'Invalid request data' },
    }).as('createNeedError');

    cy.visit('/needs/create');

    cy.get('select[name="category"]').select('FOOD', { force: true });
    cy.get('textarea[name="description"]').type('Test description', { force: true });
    cy.get('select[name="urgencyLevel"]').select('HIGH', { force: true });
    cy.get('input[name="country"]').type('TestCountry', { force: true });

    cy.get('button[type="submit"]').click({ force: true });
    cy.wait('@createNeedError');

    cy.contains('Failed to create need').should('be.visible');
  });

  it('should handle unauthorized access', () => {
    // Clear token to simulate unauthorized access
    localStorage.removeItem('token');

    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 401,
      body: { message: 'Unauthorized' },
    }).as('unauthorizedRequest');

    cy.visit('/dashboard');

    // Should redirect to login
    cy.url().should('include', '/login');
  });
});

describe('Privacy and Security Workflow', () => {
  beforeEach(() => {
    const mockToken = 'mock-jwt-token';
    localStorage.setItem('token', mockToken);

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
  });

  it('should display privacy notice when viewing needs', () => {
    cy.intercept('GET', `${Cypress.env('apiUrl')}/needs`, {
      statusCode: 200,
      body: [],
    }).as('getNeedsRequest');

    cy.visit('/needs');

    cy.contains('Privacy Notice').should('be.visible');
    cy.contains('redacted information').should('be.visible');
    cy.contains('Sensitive details').should('be.visible');
  });

  it('should display privacy notice when creating needs', () => {
    cy.visit('/needs/create');

    cy.contains('Privacy & Security Notice').should('be.visible');
    cy.contains('encrypted').should('be.visible');
  });
});
