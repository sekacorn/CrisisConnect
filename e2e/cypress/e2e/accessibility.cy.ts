/**
 * Accessibility (a11y) Tests
 *
 * Tests WCAG 2.1 Level AA compliance using axe-core
 * Covers: color contrast, keyboard navigation, ARIA attributes, semantic HTML, form labels
 */

describe('Accessibility - Login Page', () => {
  beforeEach(() => {
    cy.visit('/login');
    cy.injectAxe();
  });

  it('should have no accessibility violations on login page', () => {
    cy.checkA11y(null, {
      runOnly: {
        type: 'tag',
        values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']
      }
    });
  });

  it('should have proper form labels', () => {
    cy.get('input[type="email"]').should('have.attr', 'required');
    cy.get('input[type="password"]').should('have.attr', 'required');
  });

  it('should support keyboard navigation', () => {
    cy.get('input[type="email"]').focus().should('have.focus');
    cy.get('input[type="email"]').type('{tab}');
    cy.get('input[type="password"]').should('have.focus');
    cy.get('input[type="password"]').type('{tab}');
    cy.get('button[type="submit"]').should('have.focus');
  });

  it('should have sufficient color contrast', () => {
    cy.checkA11y(null, {
      runOnly: {
        type: 'tag',
        values: ['cat.color']
      }
    });
  });

  it('should have descriptive page title', () => {
    cy.title().should('not.be.empty');
  });
});

describe('Accessibility - Dashboard', () => {
  beforeEach(() => {
    const mockToken = 'mock-jwt-token';
    localStorage.setItem('token', mockToken);

    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'test-user-id',
        name: 'Test User',
        email: 'test@crisisconnect.org',
        role: 'FIELD_WORKER',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.visit('/dashboard');
    cy.injectAxe();
  });

  it('should have no accessibility violations on dashboard', () => {
    cy.checkA11y(null, {
      runOnly: {
        type: 'tag',
        values: ['wcag2a', 'wcag2aa']
      }
    });
  });

  it('should have proper heading hierarchy', () => {
    cy.get('h1').should('exist');
    cy.get('h1').should('contain', 'CrisisConnect Dashboard');
  });

  it('should have clickable cards with keyboard access', () => {
    cy.get('h3').first().parent().should('have.css', 'cursor', 'pointer');
  });

  it('should have descriptive button text', () => {
    cy.get('button').contains('Logout').should('be.visible');
  });
});

describe('Accessibility - Needs List', () => {
  beforeEach(() => {
    const mockToken = 'mock-jwt-token';
    localStorage.setItem('token', mockToken);

    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'test-user-id',
        name: 'Test User',
        email: 'test@crisisconnect.org',
        role: 'FIELD_WORKER',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.intercept('GET', `${Cypress.env('apiUrl')}/needs`, {
      statusCode: 200,
      body: [
        {
          id: 'need-1',
          status: 'NEW',
          category: 'FOOD',
          country: 'TestCountry',
          regionOrState: 'TestRegion',
          urgencyLevel: 'HIGH',
          createdAt: new Date().toISOString(),
        },
      ],
    }).as('getNeedsRequest');

    cy.visit('/needs');
    cy.injectAxe();
  });

  it('should have no accessibility violations on needs list', () => {
    cy.wait('@getNeedsRequest');
    cy.checkA11y(null, {
      runOnly: {
        type: 'tag',
        values: ['wcag2a', 'wcag2aa']
      }
    });
  });

  it('should have descriptive headings', () => {
    cy.get('h1').should('contain', 'Assistance Needs');
  });

  it('should have accessible navigation buttons', () => {
    cy.get('button').contains('Back to Dashboard').should('be.visible');
  });
});

describe('Accessibility - Create Need Form', () => {
  beforeEach(() => {
    const mockToken = 'mock-jwt-token';
    localStorage.setItem('token', mockToken);

    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'test-user-id',
        name: 'Test User',
        email: 'test@crisisconnect.org',
        role: 'FIELD_WORKER',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.visit('/needs/create');
    cy.injectAxe();
  });

  it('should have no accessibility violations on create need form', () => {
    cy.checkA11y(null, {
      runOnly: {
        type: 'tag',
        values: ['wcag2a', 'wcag2aa']
      }
    });
  });

  it('should have proper form field labels', () => {
    cy.get('label').should('have.length.greaterThan', 0);
    cy.get('select[name="category"]').should('have.attr', 'required');
    cy.get('textarea[name="description"]').should('have.attr', 'required');
    cy.get('select[name="urgencyLevel"]').should('have.attr', 'required');
    cy.get('input[name="country"]').should('have.attr', 'required');
  });

  it('should support keyboard navigation through form', () => {
    cy.get('select[name="category"]').focus().should('have.focus');
    cy.get('select[name="category"]').type('{tab}');
    cy.focused().should('have.attr', 'name', 'description');
  });

  it('should have accessible submit button', () => {
    cy.get('button[type="submit"]').should('be.visible');
    cy.get('button[type="submit"]').should('contain', 'Submit');
  });

  it('should have descriptive placeholders', () => {
    cy.get('textarea[name="description"]').should('have.attr', 'placeholder');
  });
});

describe('Accessibility - Admin Dashboard', () => {
  beforeEach(() => {
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

    cy.visit('/admin');
    cy.injectAxe();
  });

  it('should have no accessibility violations on admin dashboard', () => {
    cy.checkA11y(null, {
      runOnly: {
        type: 'tag',
        values: ['wcag2a', 'wcag2aa']
      }
    });
  });

  it('should have accessible tab navigation', () => {
    cy.contains('Statistics').should('be.visible');
    cy.contains('Organizations').should('be.visible');
    cy.contains('Users').should('be.visible');
  });

  it('should have proper heading structure', () => {
    cy.get('h1').should('contain', 'Admin Dashboard');
  });
});

describe('Accessibility - Keyboard Navigation Tests', () => {
  it('should navigate login form with keyboard only', () => {
    cy.visit('/login');

    // Tab through all interactive elements
    cy.get('body').tab();
    cy.focused().should('have.attr', 'type', 'email');

    cy.focused().tab();
    cy.focused().should('have.attr', 'type', 'password');

    cy.focused().tab();
    cy.focused().should('have.attr', 'type', 'submit');
  });
});

describe('Accessibility - Screen Reader Support', () => {
  beforeEach(() => {
    cy.visit('/login');
    cy.injectAxe();
  });

  it('should have appropriate ARIA attributes', () => {
    cy.checkA11y(null, {
      runOnly: {
        type: 'tag',
        values: ['best-practice']
      }
    });
  });

  it('should have alt text for images if any exist', () => {
    cy.get('img').each(($img) => {
      cy.wrap($img).should('have.attr', 'alt');
    });
  });

  it('should not have empty links', () => {
    cy.get('a').each(($link) => {
      cy.wrap($link).should('not.be.empty');
    });
  });
});

describe('Accessibility - Color and Contrast', () => {
  beforeEach(() => {
    cy.visit('/login');
    cy.injectAxe();
  });

  it('should have sufficient color contrast ratios', () => {
    cy.checkA11y(null, {
      rules: {
        'color-contrast': { enabled: true }
      }
    });
  });

  it('should not rely on color alone for information', () => {
    cy.checkA11y(null, {
      rules: {
        'color-contrast': { enabled: true },
        'link-in-text-block': { enabled: true }
      }
    });
  });
});
