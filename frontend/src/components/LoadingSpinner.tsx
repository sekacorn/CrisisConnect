import React from 'react';

/**
 * Accessible Loading Spinner
 * Provides screen reader announcement and visible loading indicator
 */
interface LoadingSpinnerProps {
  message?: string;
  size?: 'small' | 'medium' | 'large';
}

const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  message = 'Loading',
  size = 'medium',
}) => {
  const sizeMap = {
    small: { width: '20px', height: '20px', borderWidth: '2px' },
    medium: { width: '40px', height: '40px', borderWidth: '4px' },
    large: { width: '60px', height: '60px', borderWidth: '6px' },
  };

  const dimensions = sizeMap[size];

  return (
    <div
      role="status"
      aria-live="polite"
      aria-label={message}
      style={styles.container}
    >
      <div
        style={{
          ...styles.spinner,
          ...dimensions,
        }}
        aria-hidden="true"
      />
      <span style={styles.srOnly}>{message}...</span>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '20px',
  },
  spinner: {
    border: '4px solid #f3f3f3',
    borderTop: '4px solid #0066cc',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  srOnly: {
    position: 'absolute',
    width: '1px',
    height: '1px',
    padding: '0',
    margin: '-1px',
    overflow: 'hidden',
    clip: 'rect(0, 0, 0, 0)',
    whiteSpace: 'nowrap',
    border: '0',
  },
};

// Add keyframe animation for spinner
const spinnerAnimation = `
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
`;

if (typeof document !== 'undefined') {
  const styleElement = document.createElement('style');
  styleElement.textContent = spinnerAnimation;
  document.head.appendChild(styleElement);
}

export default LoadingSpinner;
