// Cypress support file for E2E tests
import 'cypress-axe';

// Hide webpack dev server overlay if it appears
Cypress.on('uncaught:exception', (err, runnable) => {
  // Prevent Cypress from failing on uncaught exceptions from app
  return false;
});

// Remove webpack overlay before each test
beforeEach(() => {
  cy.on('window:before:load', (win) => {
    // Remove webpack dev server overlay if present
    win.addEventListener('DOMContentLoaded', () => {
      const overlay = win.document.getElementById('webpack-dev-server-client-overlay');
      if (overlay) {
        overlay.remove();
      }
    });
  });
});

// Custom commands
Cypress.Commands.add('login', (email: string, password: string) => {
  cy.visit('/login');
  cy.get('input[type="email"]').type(email, { force: true });
  cy.get('input[type="password"]').type(password, { force: true });
  cy.get('button[type="submit"]').click({ force: true });
});

// Accessibility testing command
Cypress.Commands.add('checkA11y', (context?: string | Node, options?: any) => {
  cy.injectAxe();
  cy.checkA11y(context, options);
});

declare global {
  namespace Cypress {
    interface Chainable {
      login(email: string, password: string): Chainable<void>;
      checkA11y(context?: string | Node, options?: any): Chainable<void>;
    }
  }
}

export {};
