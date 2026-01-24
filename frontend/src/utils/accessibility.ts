/**
 * Accessibility Utilities for CrisisConnect
 * Section 508 and WCAG 2.1 Level AA Compliance
 */

/**
 * Screen Reader Only component styles
 * Visually hidden but accessible to screen readers
 */
export const srOnly: React.CSSProperties = {
  position: 'absolute',
  width: '1px',
  height: '1px',
  padding: '0',
  margin: '-1px',
  overflow: 'hidden',
  clip: 'rect(0, 0, 0, 0)',
  whiteSpace: 'nowrap',
  border: '0',
};

/**
 * Focus visible styles for keyboard navigation
 * WCAG 2.4.7 Focus Visible - 2px outline with high contrast
 */
export const focusVisibleStyles: React.CSSProperties = {
  outline: '2px solid #0066cc',
  outlineOffset: '2px',
};

/**
 * Announce message to screen readers
 * Creates a temporary live region for announcements
 */
export const announceToScreenReader = (message: string, priority: 'polite' | 'assertive' = 'polite') => {
  const announcement = document.createElement('div');
  announcement.setAttribute('role', 'status');
  announcement.setAttribute('aria-live', priority);
  announcement.setAttribute('aria-atomic', 'true');
  announcement.style.position = 'absolute';
  announcement.style.left = '-10000px';
  announcement.style.width = '1px';
  announcement.style.height = '1px';
  announcement.style.overflow = 'hidden';

  document.body.appendChild(announcement);

  // Delay to ensure screen reader picks it up
  setTimeout(() => {
    announcement.textContent = message;
  }, 100);

  // Remove after announcement
  setTimeout(() => {
    document.body.removeChild(announcement);
  }, 3000);
};

/**
 * Handle keyboard navigation for clickable elements
 * Allows Enter and Space to trigger click events
 */
export const handleKeyPress = (
  event: React.KeyboardEvent<HTMLElement>,
  callback: () => void
) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    callback();
  }
};

/**
 * Get ARIA label for status badge
 */
export const getStatusAriaLabel = (status: string): string => {
  const statusLabels: { [key: string]: string } = {
    'NEW': 'Status: New, not yet assigned',
    'ASSIGNED': 'Status: Assigned to an organization',
    'IN_PROGRESS': 'Status: In progress, being addressed',
    'RESOLVED': 'Status: Resolved, assistance provided',
    'REJECTED': 'Status: Rejected, cannot be fulfilled',
  };
  return statusLabels[status] || `Status: ${status}`;
};

/**
 * Get ARIA label for urgency level
 */
export const getUrgencyAriaLabel = (urgency: string): string => {
  const urgencyLabels: { [key: string]: string } = {
    'CRITICAL': 'Urgency: Critical, immediate attention required',
    'HIGH': 'Urgency: High priority',
    'MEDIUM': 'Urgency: Medium priority',
    'LOW': 'Urgency: Low priority',
  };
  return urgencyLabels[urgency] || `Urgency: ${urgency}`;
};

/**
 * Get color contrast compliant colors
 * WCAG 2.1 Level AA requires 4.5:1 for normal text, 3:1 for large text
 */
export const getAccessibleStatusColor = (status: string): { bg: string; text: string } => {
  const colors: { [key: string]: { bg: string; text: string } } = {
    'NEW': { bg: '#0056b3', text: '#ffffff' },
    'ASSIGNED': { bg: '#cc8800', text: '#000000' },
    'IN_PROGRESS': { bg: '#0066cc', text: '#ffffff' },
    'RESOLVED': { bg: '#1a7f37', text: '#ffffff' },
    'REJECTED': { bg: '#c41e3a', text: '#ffffff' },
  };
  return colors[status] || { bg: '#495057', text: '#ffffff' };
};

/**
 * Get color contrast compliant urgency colors
 */
export const getAccessibleUrgencyColor = (urgency: string): { bg: string; text: string } => {
  const colors: { [key: string]: { bg: string; text: string } } = {
    'CRITICAL': { bg: '#c41e3a', text: '#ffffff' },
    'HIGH': { bg: '#d96c00', text: '#ffffff' },
    'MEDIUM': { bg: '#cc8800', text: '#000000' },
    'LOW': { bg: '#1a7f37', text: '#ffffff' },
  };
  return colors[urgency] || { bg: '#495057', text: '#ffffff' };
};

/**
 * Trap focus within a modal or dialog
 * Ensures keyboard users can't tab out of modal
 */
export const trapFocus = (element: HTMLElement) => {
  const focusableElements = element.querySelectorAll<HTMLElement>(
    'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
  );

  const firstFocusable = focusableElements[0];
  const lastFocusable = focusableElements[focusableElements.length - 1];

  const handleTabKey = (e: KeyboardEvent) => {
    if (e.key !== 'Tab') return;

    if (e.shiftKey) {
      if (document.activeElement === firstFocusable) {
        e.preventDefault();
        lastFocusable?.focus();
      }
    } else {
      if (document.activeElement === lastFocusable) {
        e.preventDefault();
        firstFocusable?.focus();
      }
    }
  };

  element.addEventListener('keydown', handleTabKey);

  // Return cleanup function
  return () => {
    element.removeEventListener('keydown', handleTabKey);
  };
};

/**
 * Format date for screen readers
 * Provides more context than just the date
 */
export const formatDateForScreenReader = (date: string | Date): string => {
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  return dateObj.toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

/**
 * Create accessible error message ID
 * Links form field to error message via aria-describedby
 */
export const createErrorId = (fieldName: string): string => {
  return `${fieldName}-error`;
};

/**
 * Create accessible description ID
 * Links form field to help text via aria-describedby
 */
export const createDescriptionId = (fieldName: string): string => {
  return `${fieldName}-description`;
};
