import React from 'react';

/**
 * Skip Navigation Link
 * WCAG 2.4.1 Bypass Blocks - Allows keyboard users to skip to main content
 */
interface SkipLinkProps {
  targetId: string;
  label?: string;
}

const SkipLink: React.FC<SkipLinkProps> = ({ targetId, label = 'Skip to main content' }) => {
  const handleClick = (e: React.MouseEvent<HTMLAnchorElement>) => {
    e.preventDefault();
    const target = document.getElementById(targetId);
    if (target) {
      target.focus();
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  return (
    <a
      href={`#${targetId}`}
      onClick={handleClick}
      style={styles.skipLink}
      className="skip-link"
    >
      {label}
    </a>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  skipLink: {
    position: 'absolute',
    left: '-9999px',
    top: '20px',
    zIndex: 9999,
    padding: '12px 20px',
    backgroundColor: '#0066cc',
    color: '#ffffff',
    textDecoration: 'none',
    fontWeight: 'bold',
    borderRadius: '4px',
    // Visible on focus
    outline: 'none',
  },
};

// Add global styles for skip link focus state
const globalStyles = `
  .skip-link:focus {
    left: 20px !important;
    outline: 3px solid #ffffff !important;
    outline-offset: 2px !important;
  }
`;

// Inject global styles
if (typeof document !== 'undefined') {
  const styleElement = document.createElement('style');
  styleElement.textContent = globalStyles;
  document.head.appendChild(styleElement);
}

export default SkipLink;
