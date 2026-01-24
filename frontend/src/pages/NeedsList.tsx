import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../services/api';
import { RedactedNeedResponse } from '../types';

const NeedsList: React.FC = () => {
  const [needs, setNeeds] = useState<RedactedNeedResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    loadNeeds();
  }, []);

  const loadNeeds = async () => {
    try {
      const data = await apiClient.getAllNeeds();
      setNeeds(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load needs');
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'NEW': return '#17a2b8';
      case 'ASSIGNED': return '#ffc107';
      case 'IN_PROGRESS': return '#007bff';
      case 'RESOLVED': return '#28a745';
      case 'REJECTED': return '#dc3545';
      default: return '#6c757d';
    }
  };

  const getUrgencyColor = (urgency: string) => {
    switch (urgency) {
      case 'CRITICAL': return '#dc3545';
      case 'HIGH': return '#fd7e14';
      case 'MEDIUM': return '#ffc107';
      case 'LOW': return '#28a745';
      default: return '#6c757d';
    }
  };

  if (loading) return <div style={styles.loading}>Loading needs...</div>;
  if (error) return <div style={styles.error}>{error}</div>;

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>Assistance Needs</h1>
        <button onClick={() => navigate('/dashboard')} style={styles.backButton}>
          Back to Dashboard
        </button>
      </div>

      <div style={styles.notice}>
        <strong>Privacy Notice:</strong> This list shows redacted information only. Sensitive details (beneficiary contact info, precise location, descriptions) are protected and only visible to authorized users. Click on a need to view full details if you have access.
      </div>

      <div style={styles.grid}>
        {needs.map((need) => (
          <div key={need.id} style={styles.card} onClick={() => navigate(`/needs/${need.id}`)}>
            <div style={styles.cardHeader}>
              <span style={{...styles.badge, backgroundColor: getStatusColor(need.status)}}>
                {need.status}
              </span>
              <span style={{...styles.badge, backgroundColor: getUrgencyColor(need.urgencyLevel)}}>
                {need.urgencyLevel}
              </span>
            </div>

            <div style={styles.cardBody}>
              <div style={styles.cardRow}>
                <strong>Category:</strong> {need.category}
              </div>
              <div style={styles.cardRow}>
                <strong>Location:</strong> {need.regionOrState ? `${need.regionOrState}, ` : ''}{need.country}
              </div>
              {need.generalizedVulnerabilityFlags && (
                <div style={styles.cardRow}>
                  <strong>Flags:</strong> {need.generalizedVulnerabilityFlags}
                </div>
              )}
              <div style={styles.cardRow}>
                <strong>Created:</strong> {new Date(need.createdAt).toLocaleDateString()}
              </div>
            </div>

            <div style={styles.cardFooter}>
              <div style={{ color: '#007bff', marginBottom: '4px' }}>
                Click to view details
              </div>
              <div style={{ fontSize: '11px', color: '#999', fontStyle: 'italic' }}>
                Full details require authorization
              </div>
            </div>
          </div>
        ))}
      </div>

      {needs.length === 0 && (
        <div style={styles.empty}>No assistance needs found.</div>
      )}
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    minHeight: '100vh',
    backgroundColor: '#f5f5f5',
    padding: '20px',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '20px',
  },
  title: {
    fontSize: '28px',
    fontWeight: 'bold',
  },
  backButton: {
    padding: '10px 20px',
    fontSize: '14px',
    backgroundColor: '#6c757d',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  notice: {
    padding: '15px',
    backgroundColor: '#fff3cd',
    border: '1px solid #ffeaa7',
    borderRadius: '4px',
    marginBottom: '20px',
    fontSize: '14px',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
    gap: '20px',
  },
  card: {
    backgroundColor: 'white',
    borderRadius: '8px',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
    padding: '20px',
    cursor: 'pointer',
    transition: 'transform 0.2s',
  },
  cardHeader: {
    display: 'flex',
    gap: '10px',
    marginBottom: '15px',
  },
  badge: {
    padding: '4px 8px',
    borderRadius: '4px',
    fontSize: '12px',
    fontWeight: '500',
    color: 'white',
  },
  cardBody: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    marginBottom: '15px',
  },
  cardRow: {
    fontSize: '14px',
  },
  cardFooter: {
    fontSize: '12px',
    color: '#007bff',
    borderTop: '1px solid #eee',
    paddingTop: '10px',
  },
  loading: {
    textAlign: 'center',
    padding: '40px',
    fontSize: '18px',
  },
  error: {
    padding: '20px',
    backgroundColor: '#fee',
    color: '#c33',
    borderRadius: '4px',
    margin: '20px',
  },
  empty: {
    textAlign: 'center',
    padding: '40px',
    fontSize: '16px',
    color: '#666',
  },
};

export default NeedsList;
