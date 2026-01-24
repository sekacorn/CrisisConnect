describe('Needs Management', () => {
  beforeEach(() => {
    // Mock login
    const mockToken = 'mock-jwt-token';
    localStorage.setItem('token', mockToken);

    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'test-user-id',
        name: 'Test Worker',
        email: 'worker@crisisconnect.org',
        role: 'FIELD_WORKER',
        isActive: true,
      },
    }).as('getMeRequest');
  });

  it('should display needs list', () => {
    const mockNeeds = [
      {
        id: 'need-1',
        status: 'NEW',
        category: 'FOOD',
        country: 'TestCountry',
        regionOrState: 'TestRegion',
        urgencyLevel: 'HIGH',
        createdAt: new Date().toISOString(),
      },
    ];

    cy.intercept('GET', `${Cypress.env('apiUrl')}/needs`, {
      statusCode: 200,
      body: mockNeeds,
    }).as('getNeedsRequest');

    cy.visit('/needs');
    cy.wait('@getNeedsRequest');

    cy.contains('Assistance Needs').should('be.visible');
    cy.contains('FOOD').should('be.visible');
    cy.contains('HIGH').should('be.visible');
    cy.contains('TestCountry').should('be.visible');
  });

  it('should create a new need', () => {
    cy.intercept('POST', `${Cypress.env('apiUrl')}/needs`, {
      statusCode: 200,
      body: {
        id: 'new-need-id',
        category: 'FOOD',
        description: 'Test need',
        country: 'TestCountry',
        urgencyLevel: 'HIGH',
      },
    }).as('createNeedRequest');

    cy.visit('/needs/create');

    cy.get('select[name="category"]').select('FOOD', { force: true });
    cy.get('textarea[name="description"]').type('Need food assistance urgently', { force: true });
    cy.get('select[name="urgencyLevel"]').select('HIGH', { force: true });
    cy.get('input[name="country"]').type('TestCountry', { force: true });
    cy.get('input[name="regionOrState"]').type('TestRegion', { force: true });

    cy.get('button[type="submit"]').click({ force: true });

    cy.wait('@createNeedRequest');
    cy.url().should('include', '/needs');
  });

  it('should display privacy notice', () => {
    cy.intercept('GET', `${Cypress.env('apiUrl')}/needs`, {
      statusCode: 200,
      body: [],
    }).as('getNeedsRequest');

    cy.visit('/needs');

    cy.contains('Privacy Notice').should('be.visible');
    cy.contains('redacted information').should('be.visible');
  });
});
