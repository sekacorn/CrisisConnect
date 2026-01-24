import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiClient } from '../services/api';
import { RedactedNeedResponse, FullNeedResponse, isFullNeedResponse } from '../types';
import RestrictedInfo from './RestrictedInfo';
import ClaimNeedButton from './ClaimNeedButton';

interface NeedDetailProps {
  userRole: string;
}

/**
 * NeedDetail component displays need information with privacy filtering
 *
 * Features:
 * - Displays RedactedNeedResponse with restricted info placeholders by default
 * - Displays FullNeedResponse with all details if user is authorized
 * - Handles rate limiting (429) with user-friendly error messages
 * - Handles authorization denial (404) - could mean not found OR access denied
 * - Shows ClaimNeedButton for NGO staff if need is not yet claimed
 * - Never caches sensitive PII in browser storage
 */
const NeedDetail: React.FC<NeedDetailProps> = ({ userRole }) => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [need, setNeed] = useState<RedactedNeedResponse | FullNeedResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [rateLimitError, setRateLimitError] = useState(false);

  useEffect(() => {
    loadNeed();
  }, [id]);

  const loadNeed = async () => {
    if (!id) return;

    setLoading(true);
    setError(null);
    setRateLimitError(false);

    try {
      const response = await apiClient.getNeedById(id);
      setNeed(response);
    } catch (err: any) {
      if (err.response?.status === 429) {
        setRateLimitError(true);
        setError(err.message || 'You have viewed too many needs recently. Please try again later.');
      } else if (err.response?.status === 404) {
        setError('Need not found or you do not have permission to view it.');
      } else {
        setError('Failed to load need details. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleClaimSuccess = () => {
    // Reload need to get updated information
    loadNeed();
  };

  if (loading) {
    return <div style={{ padding: '20px', textAlign: 'center' }}>Loading...</div>;
  }

  if (error) {
    return (
      <div style={{ padding: '20px' }}>
        <div style={{
          padding: '16px',
          backgroundColor: rateLimitError ? '#fff3cd' : '#f8d7da',
          color: rateLimitError ? '#856404' : '#721c24',
          border: `1px solid ${rateLimitError ? '#ffeaa7' : '#f5c6cb'}`,
          borderRadius: '4px',
          marginBottom: '16px'
        }}>
          {error}
        </div>
        <button
          onClick={() => navigate('/needs')}
          style={{
            padding: '10px 20px',
            backgroundColor: '#6c757d',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Back to Needs List
        </button>
      </div>
    );
  }

  if (!need) {
    return <div style={{ padding: '20px' }}>Need not found</div>;
  }

  const isFull = isFullNeedResponse(need);

  return (
    <div style={{ padding: '20px', maxWidth: '800px', margin: '0 auto' }}>
      <h1>Need Details</h1>

      {/* Always visible fields (from RedactedNeedResponse) */}
      <div style={{ marginBottom: '24px' }}>
        <div style={{ marginBottom: '12px' }}>
          <strong>Status:</strong> <span style={{
            padding: '4px 8px',
            backgroundColor: getStatusColor(need.status),
            color: 'white',
            borderRadius: '4px',
            fontSize: '14px'
          }}>{need.status}</span>
        </div>

        <div style={{ marginBottom: '12px' }}>
          <strong>Category:</strong> {need.category}
        </div>

        <div style={{ marginBottom: '12px' }}>
          <strong>Urgency:</strong> <span style={{
            padding: '4px 8px',
            backgroundColor: getUrgencyColor(need.urgencyLevel),
            color: 'white',
            borderRadius: '4px',
            fontSize: '14px'
          }}>{need.urgencyLevel}</span>
        </div>

        <div style={{ marginBottom: '12px' }}>
          <strong>Location:</strong> {need.regionOrState ? `${need.regionOrState}, ${need.country}` : need.country}
        </div>

        <div style={{ marginBottom: '12px' }}>
          <strong>Created:</strong> {new Date(need.createdAt).toLocaleString()}
        </div>

        {need.generalizedVulnerabilityFlags && (
          <div style={{ marginBottom: '12px' }}>
            <strong>Vulnerability Flags:</strong> {need.generalizedVulnerabilityFlags}
          </div>
        )}
      </div>

      <hr style={{ margin: '24px 0', border: 'none', borderTop: '1px solid #ddd' }} />

      {/* Conditional fields - show if authorized, otherwise show RestrictedInfo */}
      {isFull ? (
        <>
          <h2>Full Details (Authorized Access)</h2>

          <div style={{ marginBottom: '12px' }}>
            <strong>Description:</strong>
            <div style={{
              marginTop: '8px',
              padding: '12px',
              backgroundColor: '#f9f9f9',
              borderRadius: '4px'
            }}>
              {need.description}
            </div>
          </div>

          {need.city && (
            <div style={{ marginBottom: '12px' }}>
              <strong>City:</strong> {need.city}
            </div>
          )}

          {need.locationText && (
            <div style={{ marginBottom: '12px' }}>
              <strong>Location Details:</strong> {need.locationText}
            </div>
          )}

          {(need.locationLat && need.locationLng) && (
            <div style={{ marginBottom: '12px' }}>
              <strong>Coordinates:</strong> {need.locationLat}, {need.locationLng}
            </div>
          )}

          {need.vulnerabilityFlags && (
            <div style={{ marginBottom: '12px' }}>
              <strong>Detailed Vulnerability Flags:</strong> {need.vulnerabilityFlags}
            </div>
          )}

          {need.beneficiaryName && (
            <div style={{ marginBottom: '12px' }}>
              <strong>Beneficiary Name:</strong> {need.beneficiaryName}
            </div>
          )}

          {need.beneficiaryPhone && (
            <div style={{ marginBottom: '12px' }}>
              <strong>Beneficiary Phone:</strong> {need.beneficiaryPhone}
            </div>
          )}

          {need.beneficiaryEmail && (
            <div style={{ marginBottom: '12px' }}>
              <strong>Beneficiary Email:</strong> {need.beneficiaryEmail}
            </div>
          )}

          {need.sensitiveNotes && (
            <div style={{ marginBottom: '12px' }}>
              <strong>Sensitive Notes:</strong>
              <div style={{
                marginTop: '8px',
                padding: '12px',
                backgroundColor: '#fff3cd',
                borderRadius: '4px',
                border: '1px solid #ffeaa7'
              }}>
                {need.sensitiveNotes}
              </div>
            </div>
          )}

          {need.assignedOrganizationId && (
            <div style={{ marginBottom: '12px' }}>
              <strong>Assigned Organization:</strong> {need.assignedOrganizationId}
              {need.assignedAt && ` (at ${new Date(need.assignedAt).toLocaleString()})`}
            </div>
          )}

          {need.resolvedAt && (
            <div style={{ marginBottom: '12px' }}>
              <strong>Resolved:</strong> {new Date(need.resolvedAt).toLocaleString()}
            </div>
          )}

          <div style={{ marginBottom: '12px' }}>
            <strong>Last Updated:</strong> {new Date(need.updatedAt).toLocaleString()}
          </div>
        </>
      ) : (
        <>
          <h2>Restricted Information</h2>
          <p style={{ color: '#666', marginBottom: '16px' }}>
            The following information requires authorization to view. If you are from an assigned NGO or created this need, you may have access to full details.
          </p>

          <RestrictedInfo
            label="Description"
            reason="Full description is restricted to authorized users only"
          />
          <RestrictedInfo
            label="Precise Location (City, Coordinates)"
            reason="Exact location is protected to ensure beneficiary safety"
          />
          <RestrictedInfo
            label="Beneficiary Contact Information"
            reason="Personal information (name, phone, email) is encrypted and restricted"
          />
          <RestrictedInfo
            label="Sensitive Notes"
            reason="Sensitive case notes are only visible to authorized organizations"
          />
        </>
      )}

      {/* Claim button - only visible to NGO staff if need is not yet claimed */}
      {'assignedOrganizationId' in need && !need.assignedOrganizationId && (
        <ClaimNeedButton
          needId={id!}
          userRole={userRole}
          isAlreadyClaimed={false}
          onClaimSuccess={handleClaimSuccess}
        />
      )}

      <div style={{ marginTop: '24px' }}>
        <button
          onClick={() => navigate('/needs')}
          style={{
            padding: '10px 20px',
            backgroundColor: '#6c757d',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Back to Needs List
        </button>
      </div>
    </div>
  );
};

// Helper functions for styling
function getStatusColor(status: string): string {
  switch (status) {
    case 'NEW':
      return '#007bff'; // Blue
    case 'ASSIGNED':
      return '#ffc107'; // Yellow
    case 'IN_PROGRESS':
      return '#17a2b8'; // Cyan
    case 'RESOLVED':
      return '#28a745'; // Green
    case 'REJECTED':
      return '#dc3545'; // Red
    default:
      return '#6c757d'; // Gray
  }
}

function getUrgencyColor(urgency: string): string {
  switch (urgency) {
    case 'CRITICAL':
      return '#dc3545'; // Red
    case 'HIGH':
      return '#fd7e14'; // Orange
    case 'MEDIUM':
      return '#ffc107'; // Yellow
    case 'LOW':
      return '#28a745'; // Green
    default:
      return '#6c757d'; // Gray
  }
}

export default NeedDetail;
