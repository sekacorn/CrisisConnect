import React, { useEffect, useState } from 'react';
import { apiClient } from '../../services/api';
import { Organization, OrganizationStatus } from '../../types';

/**
 * Organization Management Component
 *
 * Features:
 * - View all organizations with pagination
 * - Filter by status (PENDING, VERIFIED, REJECTED)
 * - Verify/reject organizations
 * - Add verification notes
 */
const OrganizationManagement: React.FC = () => {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [selectedOrg, setSelectedOrg] = useState<Organization | null>(null);
  const [verificationNotes, setVerificationNotes] = useState('');
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    loadOrganizations();
  }, [statusFilter]);

  const loadOrganizations = async () => {
    setLoading(true);
    setError(null);

    try {
      let data;
      if (statusFilter === 'ALL') {
        const response = await apiClient.getAdminOrganizations(0, 100);
        data = response.content || response;
      } else {
        data = await apiClient.getOrganizationsByStatus(statusFilter);
      }
      setOrganizations(Array.isArray(data) ? data : []);
    } catch (err: any) {
      setError('Failed to load organizations');
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async (orgId: string, status: OrganizationStatus) => {
    setUpdating(true);
    try {
      await apiClient.updateOrganization(orgId, status, verificationNotes || undefined);
      setSelectedOrg(null);
      setVerificationNotes('');
      loadOrganizations();
    } catch (err: any) {
      alert('Failed to update organization: ' + (err.response?.data?.message || err.message));
    } finally {
      setUpdating(false);
    }
  };

  const getStatusColor = (status: OrganizationStatus) => {
    switch (status) {
      case OrganizationStatus.VERIFIED:
        return '#28a745';
      case OrganizationStatus.PENDING:
        return '#ffc107';
      case OrganizationStatus.REJECTED:
        return '#dc3545';
      default:
        return '#6c757d';
    }
  };

  if (loading) {
    return <div style={styles.loading}>Loading organizations...</div>;
  }

  return (
    <div style={styles.container}>
      {/* Header */}
      <div style={styles.header}>
        <h2 style={styles.title}>Organization Management</h2>
        <div style={styles.filters}>
          <label style={styles.filterLabel}>Filter by Status:</label>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            style={styles.filterSelect}
          >
            <option value="ALL">All</option>
            <option value="PENDING">Pending</option>
            <option value="VERIFIED">Verified</option>
            <option value="REJECTED">Rejected</option>
          </select>
        </div>
      </div>

      {error && (
        <div style={styles.error}>{error}</div>
      )}

      {/* Organization List */}
      <div style={styles.table}>
        <table style={styles.tableElement}>
          <thead>
            <tr style={styles.tableHeader}>
              <th style={styles.th}>Name</th>
              <th style={styles.th}>Email</th>
              <th style={styles.th}>Phone</th>
              <th style={styles.th}>Country</th>
              <th style={styles.th}>Status</th>
              <th style={styles.th}>Created</th>
              <th style={styles.th}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {organizations.map((org) => (
              <tr key={org.id} style={styles.tableRow}>
                <td style={styles.td}>{org.name}</td>
                <td style={styles.td}>{org.email}</td>
                <td style={styles.td}>{org.phone}</td>
                <td style={styles.td}>{org.country}</td>
                <td style={styles.td}>
                  <span style={{
                    ...styles.statusBadge,
                    backgroundColor: getStatusColor(org.status)
                  }}>
                    {org.status}
                  </span>
                </td>
                <td style={styles.td}>{new Date(org.createdAt).toLocaleDateString()}</td>
                <td style={styles.td}>
                  <button
                    onClick={() => setSelectedOrg(org)}
                    style={styles.actionButton}
                  >
                    View/Edit
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {organizations.length === 0 && (
          <div style={styles.empty}>No organizations found</div>
        )}
      </div>

      {/* Verification Modal */}
      {selectedOrg && (
        <div style={styles.modal}>
          <div style={styles.modalContent}>
            <div style={styles.modalHeader}>
              <h3 style={styles.modalTitle}>Organization Details</h3>
              <button
                onClick={() => setSelectedOrg(null)}
                style={styles.closeButton}
              >
                ×
              </button>
            </div>

            <div style={styles.modalBody}>
              <div style={styles.detailRow}>
                <strong>Name:</strong> {selectedOrg.name}
              </div>
              <div style={styles.detailRow}>
                <strong>Email:</strong> {selectedOrg.email}
              </div>
              <div style={styles.detailRow}>
                <strong>Phone:</strong> {selectedOrg.phone}
              </div>
              <div style={styles.detailRow}>
                <strong>Country:</strong> {selectedOrg.country}
              </div>
              <div style={styles.detailRow}>
                <strong>Current Status:</strong>{' '}
                <span style={{
                  ...styles.statusBadge,
                  backgroundColor: getStatusColor(selectedOrg.status)
                }}>
                  {selectedOrg.status}
                </span>
              </div>
              {selectedOrg.verificationNotes && (
                <div style={styles.detailRow}>
                  <strong>Previous Notes:</strong> {selectedOrg.verificationNotes}
                </div>
              )}

              <div style={styles.formGroup}>
                <label style={styles.label}>Verification Notes:</label>
                <textarea
                  value={verificationNotes}
                  onChange={(e) => setVerificationNotes(e.target.value)}
                  style={styles.textarea}
                  placeholder="Enter verification notes (optional)..."
                  rows={4}
                />
              </div>
            </div>

            <div style={styles.modalFooter}>
              <button
                onClick={() => handleVerify(selectedOrg.id, OrganizationStatus.VERIFIED)}
                disabled={updating}
                style={{...styles.button, backgroundColor: '#28a745'}}
              >
                {updating ? 'Updating...' : 'Verify'}
              </button>
              <button
                onClick={() => handleVerify(selectedOrg.id, OrganizationStatus.REJECTED)}
                disabled={updating}
                style={{...styles.button, backgroundColor: '#dc3545'}}
              >
                {updating ? 'Updating...' : 'Reject'}
              </button>
              <button
                onClick={() => setSelectedOrg(null)}
                style={{...styles.button, backgroundColor: '#6c757d'}}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    maxWidth: '1400px',
    margin: '0 auto',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '24px',
  },
  title: {
    fontSize: '24px',
    fontWeight: 'bold',
    margin: 0,
  },
  filters: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
  },
  filterLabel: {
    fontSize: '14px',
    fontWeight: '500',
  },
  filterSelect: {
    padding: '8px 12px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    fontSize: '14px',
  },
  loading: {
    textAlign: 'center',
    padding: '40px',
    fontSize: '18px',
  },
  error: {
    padding: '16px',
    backgroundColor: '#f8d7da',
    color: '#721c24',
    borderRadius: '4px',
    marginBottom: '20px',
  },
  table: {
    backgroundColor: 'white',
    borderRadius: '8px',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
    overflow: 'hidden',
  },
  tableElement: {
    width: '100%',
    borderCollapse: 'collapse',
  },
  tableHeader: {
    backgroundColor: '#f8f9fa',
  },
  th: {
    padding: '16px',
    textAlign: 'left',
    fontSize: '14px',
    fontWeight: '600',
    color: '#333',
    borderBottom: '2px solid #e0e0e0',
  },
  tableRow: {
    borderBottom: '1px solid #e0e0e0',
  },
  td: {
    padding: '16px',
    fontSize: '14px',
    color: '#555',
  },
  statusBadge: {
    padding: '4px 8px',
    borderRadius: '4px',
    color: 'white',
    fontSize: '12px',
    fontWeight: '500',
  },
  actionButton: {
    padding: '6px 12px',
    backgroundColor: '#007bff',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '13px',
  },
  empty: {
    padding: '40px',
    textAlign: 'center',
    color: '#666',
  },
  modal: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
  },
  modalContent: {
    backgroundColor: 'white',
    borderRadius: '8px',
    width: '90%',
    maxWidth: '600px',
    maxHeight: '80vh',
    overflow: 'auto',
  },
  modalHeader: {
    padding: '20px',
    borderBottom: '1px solid #e0e0e0',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  modalTitle: {
    margin: 0,
    fontSize: '20px',
    fontWeight: 'bold',
  },
  closeButton: {
    background: 'none',
    border: 'none',
    fontSize: '32px',
    cursor: 'pointer',
    color: '#666',
  },
  modalBody: {
    padding: '20px',
  },
  detailRow: {
    marginBottom: '12px',
    fontSize: '14px',
    lineHeight: '1.6',
  },
  formGroup: {
    marginTop: '24px',
  },
  label: {
    display: 'block',
    marginBottom: '8px',
    fontSize: '14px',
    fontWeight: '500',
  },
  textarea: {
    width: '100%',
    padding: '10px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    fontSize: '14px',
    fontFamily: 'inherit',
    resize: 'vertical',
  },
  modalFooter: {
    padding: '20px',
    borderTop: '1px solid #e0e0e0',
    display: 'flex',
    gap: '12px',
    justifyContent: 'flex-end',
  },
  button: {
    padding: '10px 20px',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: '500',
  },
};

export default OrganizationManagement;
