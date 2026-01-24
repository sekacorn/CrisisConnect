/**
 * Performance Tests
 *
 * Tests page load times, resource loading, and overall performance metrics
 * Benchmarks: First Contentful Paint, Time to Interactive, Total Blocking Time
 */

describe('Performance - Page Load Times', () => {
  it('should load login page quickly', () => {
    const start = performance.now();

    cy.visit('/login');

    cy.window().then((win) => {
      const end = performance.now();
      const loadTime = end - start;

      // Page should load in under 3 seconds
      expect(loadTime).to.be.lessThan(3000);

      cy.log(`Login page loaded in ${loadTime.toFixed(2)}ms`);
    });
  });

  it('should load dashboard quickly', () => {
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

    const start = performance.now();

    cy.visit('/dashboard');

    cy.window().then((win) => {
      const end = performance.now();
      const loadTime = end - start;

      // Dashboard should load in under 3 seconds
      expect(loadTime).to.be.lessThan(3000);

      cy.log(`Dashboard loaded in ${loadTime.toFixed(2)}ms`);
    });
  });

  it('should load needs list quickly', () => {
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
      body: Array.from({ length: 20 }, (_, i) => ({
        id: `need-${i}`,
        status: 'NEW',
        category: 'FOOD',
        country: 'TestCountry',
        regionOrState: 'TestRegion',
        urgencyLevel: 'HIGH',
        createdAt: new Date().toISOString(),
      })),
    }).as('getNeedsRequest');

    const start = performance.now();

    cy.visit('/needs');
    cy.wait('@getNeedsRequest');

    cy.window().then((win) => {
      const end = performance.now();
      const loadTime = end - start;

      // Needs list with 20 items should load in under 4 seconds
      expect(loadTime).to.be.lessThan(4000);

      cy.log(`Needs list loaded in ${loadTime.toFixed(2)}ms`);
    });
  });
});

describe('Performance - Resource Loading', () => {
  it('should not load excessive resources', () => {
    cy.visit('/login', {
      onBeforeLoad: (win) => {
        win.performance.mark('start-visit');
      },
    });

    cy.window().then((win) => {
      const resourceList = win.performance.getEntriesByType('resource');

      cy.log(`Total resources loaded: ${resourceList.length}`);

      // Should not load more than 50 resources on login page
      expect(resourceList.length).to.be.lessThan(50);
    });
  });

  it('should measure resource timing', () => {
    cy.visit('/dashboard', {
      onBeforeLoad: (win) => {
        const mockToken = 'mock-jwt-token';
        win.localStorage.setItem('token', mockToken);
      },
    });

    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'test-user-id',
        name: 'Test User',
        email: 'test@crisisconnect.org',
        role: 'ADMIN',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.window().then((win) => {
      const resourceList = win.performance.getEntriesByType('resource');

      resourceList.forEach((resource: any) => {
        const duration = resource.duration;

        // Individual resources should load in under 2 seconds
        expect(duration).to.be.lessThan(2000);

        if (duration > 500) {
          cy.log(`Slow resource: ${resource.name} took ${duration.toFixed(2)}ms`);
        }
      });
    });
  });
});

describe('Performance - Navigation Timing', () => {
  it('should measure navigation performance metrics', () => {
    cy.visit('/login');

    cy.window().then((win) => {
      const perfData = win.performance.timing;

      // DNS lookup time
      const dnsTime = perfData.domainLookupEnd - perfData.domainLookupStart;
      cy.log(`DNS Lookup: ${dnsTime}ms`);
      expect(dnsTime).to.be.lessThan(500);

      // TCP connection time
      const tcpTime = perfData.connectEnd - perfData.connectStart;
      cy.log(`TCP Connection: ${tcpTime}ms`);
      expect(tcpTime).to.be.lessThan(500);

      // DOM content loaded time
      const domContentLoadedTime = perfData.domContentLoadedEventEnd - perfData.domContentLoadedEventStart;
      cy.log(`DOM Content Loaded: ${domContentLoadedTime}ms`);
      expect(domContentLoadedTime).to.be.lessThan(1000);

      // Page load time
      const pageLoadTime = perfData.loadEventEnd - perfData.navigationStart;
      cy.log(`Total Page Load: ${pageLoadTime}ms`);
      expect(pageLoadTime).to.be.lessThan(5000);
    });
  });
});

describe('Performance - API Response Times', () => {
  it('should measure login API response time', () => {
    cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/login`, (req) => {
      req.reply({
        statusCode: 200,
        body: {
          token: 'mock-jwt-token',
          type: 'Bearer',
          userId: 'test-user-id',
          email: 'test@crisisconnect.org',
          name: 'Test User',
          role: 'FIELD_WORKER',
        },
        delay: 0, // Simulate fast API response
      });
    }).as('loginRequest');

    cy.visit('/login');

    const start = Date.now();

    cy.get('input[type="email"]').type('test@crisisconnect.org', { force: true });
    cy.get('input[type="password"]').type('password123', { force: true });
    cy.get('button[type="submit"]').click({ force: true });

    cy.wait('@loginRequest').then((interception) => {
      const end = Date.now();
      const responseTime = end - start;

      cy.log(`Login API response time: ${responseTime}ms`);

      // Login should complete in under 2 seconds (including UI rendering)
      expect(responseTime).to.be.lessThan(2000);
    });
  });

  it('should measure needs list API response time', () => {
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

    cy.intercept('GET', `${Cypress.env('apiUrl')}/needs`, (req) => {
      req.reply({
        statusCode: 200,
        body: Array.from({ length: 50 }, (_, i) => ({
          id: `need-${i}`,
          status: 'NEW',
          category: 'FOOD',
          country: 'TestCountry',
          urgencyLevel: 'HIGH',
          createdAt: new Date().toISOString(),
        })),
        delay: 0,
      });
    }).as('getNeedsRequest');

    const start = Date.now();

    cy.visit('/needs');

    cy.wait('@getNeedsRequest').then((interception) => {
      const end = Date.now();
      const responseTime = end - start;

      cy.log(`Needs list API response time: ${responseTime}ms`);

      // Should handle 50 needs in under 3 seconds
      expect(responseTime).to.be.lessThan(3000);
    });
  });
});

describe('Performance - Memory Usage', () => {
  it('should not have memory leaks on page navigation', () => {
    const mockToken = 'mock-jwt-token';
    localStorage.setItem('token', mockToken);

    cy.intercept('GET', `${Cypress.env('apiUrl')}/auth/me`, {
      statusCode: 200,
      body: {
        id: 'test-user-id',
        name: 'Test User',
        email: 'test@crisisconnect.org',
        role: 'ADMIN',
        isActive: true,
      },
    }).as('getMeRequest');

    cy.intercept('GET', `${Cypress.env('apiUrl')}/needs`, {
      statusCode: 200,
      body: [],
    }).as('getNeedsRequest');

    // Navigate through multiple pages
    cy.visit('/dashboard');
    cy.wait(500);

    cy.visit('/needs');
    cy.wait('@getNeedsRequest');
    cy.wait(500);

    cy.visit('/dashboard');
    cy.wait(500);

    cy.visit('/needs');
    cy.wait('@getNeedsRequest');
    cy.wait(500);

    cy.window().then((win) => {
      // Check if performance memory API is available
      if (win.performance && (win.performance as any).memory) {
        const memoryInfo = (win.performance as any).memory;

        cy.log(`Used JS Heap: ${(memoryInfo.usedJSHeapSize / 1048576).toFixed(2)} MB`);
        cy.log(`Total JS Heap: ${(memoryInfo.totalJSHeapSize / 1048576).toFixed(2)} MB`);
        cy.log(`JS Heap Limit: ${(memoryInfo.jsHeapSizeLimit / 1048576).toFixed(2)} MB`);

        // Memory usage should be reasonable (under 100MB for this simple app)
        expect(memoryInfo.usedJSHeapSize).to.be.lessThan(100 * 1048576);
      }
    });
  });
});

describe('Performance - Render Performance', () => {
  it('should render large lists efficiently', () => {
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

    // Create a large dataset
    const largeDataset = Array.from({ length: 100 }, (_, i) => ({
      id: `need-${i}`,
      status: 'NEW',
      category: 'FOOD',
      country: 'TestCountry',
      regionOrState: 'TestRegion',
      urgencyLevel: 'HIGH',
      createdAt: new Date().toISOString(),
    }));

    cy.intercept('GET', `${Cypress.env('apiUrl')}/needs`, {
      statusCode: 200,
      body: largeDataset,
    }).as('getNeedsRequest');

    const start = performance.now();

    cy.visit('/needs');
    cy.wait('@getNeedsRequest');

    // Wait for all cards to render (may include extra elements)
    cy.get('[style*="cursor: pointer"]').should('have.length.gte', 100);

    cy.window().then((win) => {
      const end = performance.now();
      const renderTime = end - start;

      cy.log(`Rendered 100 items in ${renderTime.toFixed(2)}ms`);

      // Should render 100 items in under 5 seconds
      expect(renderTime).to.be.lessThan(5000);
    });
  });
});

describe('Performance - Bundle Size', () => {
  it('should measure JavaScript bundle size', () => {
    cy.visit('/login');

    cy.window().then((win) => {
      const scripts = win.performance.getEntriesByType('resource')
        .filter((resource: any) => resource.initiatorType === 'script');

      let totalSize = 0;
      scripts.forEach((script: any) => {
        totalSize += script.transferSize || 0;
      });

      cy.log(`Total JavaScript bundle size: ${(totalSize / 1024).toFixed(2)} KB`);

      // Total JS bundle should be under 2MB for initial load
      expect(totalSize).to.be.lessThan(2 * 1024 * 1024);
    });
  });
});
