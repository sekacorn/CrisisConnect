describe('Admin Dashboard', () => {
  beforeEach(() => {
    // Mock login as admin
    const mockToken = 'mock-jwt-token';
    localStorage.setItem('token', mockToken);

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
  });

  it('should access admin dashboard', () => {
    cy.visit('/admin');

    cy.contains('Admin Dashboard').should('be.visible');
    cy.contains('CrisisConnect Administration Panel').should('be.visible');
  });

  it('should display admin navigation tabs', () => {
    cy.visit('/admin');

    cy.contains('Statistics').should('be.visible');
    cy.contains('Organizations').should('be.visible');
    cy.contains('Users').should('be.visible');
    cy.contains('Audit Logs').should('be.visible');
    cy.contains('Suspicious Activity').should('be.visible');
  });

  it('should switch between tabs', () => {
    cy.visit('/admin');

    // Click Organizations tab
    cy.contains('Organizations').click({ force: true });
    cy.wait(500);

    // Click Users tab
    cy.contains('Users').click({ force: true });
    cy.wait(500);

    // Click Audit Logs tab
    cy.contains('Audit Logs').click({ force: true });
    cy.wait(500);

    // Click Suspicious Activity tab
    cy.contains('Suspicious Activity').click({ force: true });
    cy.wait(500);

    // Click back to Statistics
    cy.contains('Statistics').click({ force: true });
  });

  it('should navigate back to main dashboard', () => {
    cy.visit('/admin');

    cy.contains('Main Dashboard').click({ force: true });
    cy.url().should('include', '/dashboard');
  });

  it('should logout from admin dashboard', () => {
    cy.visit('/admin');

    cy.contains('Logout').click({ force: true });
    cy.url().should('include', '/login');
  });
});

describe('Admin Access Control', () => {
  it('should redirect non-admin users away from admin panel', () => {
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

    cy.visit('/admin');

    // Should redirect to dashboard
    cy.url().should('include', '/dashboard');
    cy.url().should('not.include', '/admin');
  });
});
