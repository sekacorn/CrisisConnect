import React from 'react';

interface RestrictedInfoProps {
  label: string;
  reason?: string;
}

/**
 * RestrictedInfo component displays a placeholder for restricted/redacted information
 *
 * Used when displaying RedactedNeedResponse where PII fields are not available
 * Shows user-friendly message explaining why information is restricted
 */
const RestrictedInfo: React.FC<RestrictedInfoProps> = ({ label, reason }) => {
  return (
    <div style={{
      padding: '12px',
      backgroundColor: '#f5f5f5',
      borderLeft: '4px solid #999',
      borderRadius: '4px',
      marginBottom: '8px'
    }}>
      <div style={{ fontWeight: 'bold', color: '#666', marginBottom: '4px' }}>
        {label}
      </div>
      <div style={{ color: '#999', fontSize: '14px', fontStyle: 'italic' }}>
        {reason || 'Information restricted - Authorization required'}
      </div>
    </div>
  );
};

export default RestrictedInfo;
