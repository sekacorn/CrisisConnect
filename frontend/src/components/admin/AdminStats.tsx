import React, { useEffect, useState } from 'react';
import { apiClient } from '../../services/api';
import { AdminStats as AdminStatsType } from '../../types';

/**
 * Admin Statistics Dashboard
 *
 * Displays comprehensive system statistics:
 * - User counts by role and status
 * - Organization verification status
 * - Need distribution by status/category/urgency
 * - Recent activity (24h)
 * - Security metrics (30d)
 */
const AdminStats: React.FC = () => {
  const [stats, setStats] = useState<AdminStatsType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const data = await apiClient.getAdminStats();
      setStats(data);
    } catch (err: any) {
      setError('Failed to load statistics');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div style={styles.loading}>Loading statistics...</div>;
  }

  if (error || !stats) {
    return <div style={styles.error}>{error || 'No data available'}</div>;
  }

  return (
    <div style={styles.container}>
      {/* User Statistics */}
      <section style={styles.section}>
        <h2 style={styles.sectionTitle}>User Statistics</h2>
        <div style={styles.grid}>
          <div style={styles.card}>
            <div style={styles.cardValue}>{stats.totalUsers}</div>
            <div style={styles.cardLabel}>Total Users</div>
          </div>
          <div style={styles.card}>
            <div style={styles.cardValue}>{stats.activeUsers}</div>
            <div style={styles.cardLabel}>Active Users</div>
          </div>
          {Object.entries(stats.usersByRole).map(([role, count]) => (
            <div key={role} style={styles.card}>
              <div style={styles.cardValue}>{count}</div>
              <div style={styles.cardLabel}>{role}</div>
            </div>
          ))}
        </div>
      </section>

      {/* Organization Statistics */}
      <section style={styles.section}>
        <h2 style={styles.sectionTitle}>Organization Statistics</h2>
        <div style={styles.grid}>
          <div style={styles.card}>
            <div style={styles.cardValue}>{stats.totalOrganizations}</div>
            <div style={styles.cardLabel}>Total Organizations</div>
          </div>
          <div style={{...styles.card, borderLeft: '4px solid #28a745'}}>
            <div style={styles.cardValue}>{stats.verifiedOrganizations}</div>
            <div style={styles.cardLabel}>Verified</div>
          </div>
          <div style={{...styles.card, borderLeft: '4px solid #ffc107'}}>
            <div style={styles.cardValue}>{stats.pendingOrganizations}</div>
            <div style={styles.cardLabel}>Pending Verification</div>
          </div>
        </div>
      </section>

      {/* Need Statistics */}
      <section style={styles.section}>
        <h2 style={styles.sectionTitle}>Need Statistics</h2>
        <div style={styles.grid}>
          <div style={styles.card}>
            <div style={styles.cardValue}>{stats.totalNeeds}</div>
            <div style={styles.cardLabel}>Total Needs</div>
          </div>
        </div>

        <h3 style={styles.subsectionTitle}>By Status</h3>
        <div style={styles.grid}>
          {Object.entries(stats.needsByStatus).map(([status, count]) => (
            <div key={status} style={styles.card}>
              <div style={styles.cardValue}>{count}</div>
              <div style={styles.cardLabel}>{status}</div>
            </div>
          ))}
        </div>

        <h3 style={styles.subsectionTitle}>By Category</h3>
        <div style={styles.grid}>
          {Object.entries(stats.needsByCategory).map(([category, count]) => (
            <div key={category} style={styles.card}>
              <div style={styles.cardValue}>{count}</div>
              <div style={styles.cardLabel}>{category}</div>
            </div>
          ))}
        </div>

        <h3 style={styles.subsectionTitle}>By Urgency</h3>
        <div style={styles.grid}>
          {Object.entries(stats.needsByUrgency).map(([urgency, count]) => (
            <div key={urgency} style={{
              ...styles.card,
              borderLeft: `4px solid ${getUrgencyColor(urgency)}`
            }}>
              <div style={styles.cardValue}>{count}</div>
              <div style={styles.cardLabel}>{urgency}</div>
            </div>
          ))}
        </div>
      </section>

      {/* Activity Statistics (Last 24 Hours) */}
      <section style={styles.section}>
        <h2 style={styles.sectionTitle}>Recent Activity (24 Hours)</h2>
        <div style={styles.grid}>
          <div style={styles.card}>
            <div style={styles.cardValue}>{stats.recentLogins24h}</div>
            <div style={styles.cardLabel}>Logins</div>
          </div>
          <div style={styles.card}>
            <div style={styles.cardValue}>{stats.recentNeedsCreated24h}</div>
            <div style={styles.cardLabel}>Needs Created</div>
          </div>
          <div style={styles.card}>
            <div style={styles.cardValue}>{stats.recentNeedsClaimed24h}</div>
            <div style={styles.cardLabel}>Needs Claimed</div>
          </div>
        </div>
      </section>

      {/* Security Statistics */}
      <section style={styles.section}>
        <h2 style={styles.sectionTitle}>Security Metrics</h2>
        <div style={styles.grid}>
          <div style={{...styles.card, borderLeft: '4px solid #dc3545'}}>
            <div style={styles.cardValue}>{stats.suspiciousActivities30d}</div>
            <div style={styles.cardLabel}>Suspicious Activities (30d)</div>
          </div>
          <div style={{...styles.card, borderLeft: '4px solid #ffc107'}}>
            <div style={styles.cardValue}>{stats.rateLimitViolations24h}</div>
            <div style={styles.cardLabel}>Rate Limit Violations (24h)</div>
          </div>
          <div style={{...styles.card, borderLeft: '4px solid #fd7e14'}}>
            <div style={styles.cardValue}>{stats.failedLoginAttempts24h}</div>
            <div style={styles.cardLabel}>Failed Logins (24h)</div>
          </div>
        </div>
      </section>

      {/* Refresh Button */}
      <button onClick={loadStats} style={styles.refreshButton}>
        Refresh Statistics
      </button>
    </div>
  );
};

function getUrgencyColor(urgency: string): string {
  switch (urgency) {
    case 'CRITICAL':
      return '#dc3545';
    case 'HIGH':
      return '#fd7e14';
    case 'MEDIUM':
      return '#ffc107';
    case 'LOW':
      return '#28a745';
    default:
      return '#6c757d';
  }
}

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    maxWidth: '1400px',
    margin: '0 auto',
  },
  loading: {
    textAlign: 'center',
    padding: '40px',
    fontSize: '18px',
  },
  error: {
    padding: '20px',
    backgroundColor: '#f8d7da',
    color: '#721c24',
    borderRadius: '4px',
    border: '1px solid #f5c6cb',
  },
  section: {
    marginBottom: '40px',
  },
  sectionTitle: {
    fontSize: '22px',
    fontWeight: 'bold',
    marginBottom: '20px',
    color: '#333',
  },
  subsectionTitle: {
    fontSize: '16px',
    fontWeight: '600',
    marginTop: '24px',
    marginBottom: '12px',
    color: '#555',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
    gap: '16px',
  },
  card: {
    backgroundColor: 'white',
    padding: '24px',
    borderRadius: '8px',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
    textAlign: 'center',
  },
  cardValue: {
    fontSize: '32px',
    fontWeight: 'bold',
    color: '#007bff',
    marginBottom: '8px',
  },
  cardLabel: {
    fontSize: '14px',
    color: '#666',
    textTransform: 'uppercase',
  },
  refreshButton: {
    padding: '12px 24px',
    backgroundColor: '#007bff',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    fontSize: '16px',
    fontWeight: '500',
    cursor: 'pointer',
    marginTop: '20px',
  },
};

export default AdminStats;
