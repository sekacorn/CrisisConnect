import React, { useState } from 'react';
import { apiClient } from '../services/api';
import { canClaimNeeds } from '../types';

interface ClaimNeedButtonProps {
  needId: string;
  userRole: string;
  isAlreadyClaimed: boolean;
  onClaimSuccess?: () => void;
  onClaimError?: (error: string) => void;
}

/**
 * ClaimNeedButton component allows NGO staff to claim needs
 *
 * Features:
 * - Only visible to NGO_STAFF and ADMIN roles
 * - Disabled if need is already claimed
 * - Handles 403 (outside service area), 404 (not found), 429 (rate limited)
 * - Shows loading state during claim request
 * - Calls onClaimSuccess callback on successful claim
 */
const ClaimNeedButton: React.FC<ClaimNeedButtonProps> = ({
  needId,
  userRole,
  isAlreadyClaimed,
  onClaimSuccess,
  onClaimError,
}) => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Only show button if user can claim needs
  if (!canClaimNeeds(userRole)) {
    return null;
  }

  const handleClaim = async () => {
    setIsLoading(true);
    setError(null);

    try {
      await apiClient.claimNeed(needId);

      // Success - notify parent component
      if (onClaimSuccess) {
        onClaimSuccess();
      }
    } catch (err: any) {
      let errorMessage = 'Failed to claim need. Please try again.';

      // Handle specific error cases
      if (err.response?.status === 403) {
        errorMessage = 'Cannot claim this need - it may be outside your service area or your organization is not verified.';
      } else if (err.response?.status === 404) {
        errorMessage = 'Need not found or you do not have permission to claim it.';
      } else if (err.response?.status === 429) {
        errorMessage = err.message || 'Too many requests. Please try again later.';
      } else if (err.response?.data?.message) {
        errorMessage = err.response.data.message;
      }

      setError(errorMessage);

      if (onClaimError) {
        onClaimError(errorMessage);
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div style={{ marginTop: '16px' }}>
      <button
        onClick={handleClaim}
        disabled={isAlreadyClaimed || isLoading}
        style={{
          padding: '12px 24px',
          backgroundColor: isAlreadyClaimed ? '#ccc' : '#007bff',
          color: 'white',
          border: 'none',
          borderRadius: '4px',
          fontSize: '16px',
          fontWeight: 'bold',
          cursor: isAlreadyClaimed || isLoading ? 'not-allowed' : 'pointer',
          opacity: isAlreadyClaimed || isLoading ? 0.6 : 1,
        }}
      >
        {isLoading ? 'Claiming...' : isAlreadyClaimed ? 'Already Claimed' : 'Claim This Need'}
      </button>

      {error && (
        <div style={{
          marginTop: '12px',
          padding: '12px',
          backgroundColor: '#f8d7da',
          color: '#721c24',
          border: '1px solid #f5c6cb',
          borderRadius: '4px',
        }}>
          {error}
        </div>
      )}
    </div>
  );
};

export default ClaimNeedButton;
