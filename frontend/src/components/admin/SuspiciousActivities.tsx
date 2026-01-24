import React, { useEffect, useState } from 'react';
import { apiClient } from '../../services/api';
import { SuspiciousActivity } from '../../types';

/**
 * Suspicious Activities Monitor
 *
 * Features:
 * - View flagged suspicious activities
 * - Filter by severity
 * - View user details
 * - Export for investigation
 */
const SuspiciousActivities: React.FC = () => {
  const [activities, setActivities] = useState<SuspiciousActivity[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [days, setDays] = useState(30);

  useEffect(() => {
    loadActivities();
  }, [days]);

  const loadActivities = async () => {
    setLoading(true);
    try {
      const data = await apiClient.getSuspiciousActivities(days);
      setActivities(data);
    } catch (err: any) {
      setError('Failed to load suspicious activities');
    } finally {
      setLoading(false);
    }
  };

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return '#dc3545';
      case 'HIGH': return '#fd7e14';
      case 'MEDIUM': return '#ffc107';
      case 'LOW': return '#28a745';
      default: return '#6c757d';
    }
  };

  if (loading) return <div style={styles.loading}>Loading suspicious activities...</div>;

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={styles.title}>Suspicious Activities</h2>
        <div style={styles.filters}>
          <label style={styles.filterLabel}>Time Range:</label>
          <select
            value={days}
            onChange={(e) => setDays(Number(e.target.value))}
            style={styles.filterSelect}
          >
            <option value={7}>Last 7 days</option>
            <option value={30}>Last 30 days</option>
            <option value={90}>Last 90 days</option>
          </select>
        </div>
      </div>

      {error && <div style={styles.error}>{error}</div>}

      {activities.length > 0 && (
        <div style={styles.alert}>
          <strong>⚠️ {activities.length} suspicious activities detected</strong>
          <p style={{marginTop: '8px', fontSize: '14px'}}>
            Review the activities below and investigate any concerning patterns.
          </p>
        </div>
      )}

      <div style={styles.table}>
        <table style={styles.tableElement}>
          <thead>
            <tr style={styles.tableHeader}>
              <th style={styles.th}>Severity</th>
              <th style={styles.th}>User</th>
              <th style={styles.th}>Activity Type</th>
              <th style={styles.th}>Description</th>
              <th style={styles.th}>Count</th>
              <th style={styles.th}>Detected At</th>
            </tr>
          </thead>
          <tbody>
            {activities.map((activity) => (
              <tr key={activity.id} style={styles.tableRow}>
                <td style={styles.td}>
                  <span style={{
                    ...styles.severityBadge,
                    backgroundColor: getSeverityColor(activity.severity)
                  }}>
                    {activity.severity}
                  </span>
                </td>
                <td style={styles.td}>
                  {activity.userName || 'N/A'}<br />
                  <small style={styles.email}>{activity.userEmail || 'N/A'}</small><br />
                  <small style={styles.role}>{activity.userRole}</small>
                </td>
                <td style={styles.td}>
                  <span style={styles.activityBadge}>{activity.activityType}</span>
                </td>
                <td style={styles.td}>{activity.description}</td>
                <td style={styles.td}>
                  <strong>{activity.activityCount || 0}</strong>
                </td>
                <td style={styles.td}>{new Date(activity.detectedAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {activities.length === 0 && (
          <div style={styles.empty}>
             No suspicious activities detected in the last {days} days
          </div>
        )}
      </div>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: { maxWidth: '1400px', margin: '0 auto' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' },
  title: { fontSize: '24px', fontWeight: 'bold', margin: 0 },
  filters: { display: 'flex', alignItems: 'center', gap: '12px' },
  filterLabel: { fontSize: '14px', fontWeight: '500' },
  filterSelect: { padding: '8px 12px', border: '1px solid #ddd', borderRadius: '4px', fontSize: '14px' },
  loading: { textAlign: 'center', padding: '40px', fontSize: '18px' },
  error: { padding: '16px', backgroundColor: '#f8d7da', color: '#721c24', borderRadius: '4px', marginBottom: '20px' },
  alert: { padding: '16px', backgroundColor: '#fff3cd', color: '#856404', border: '1px solid #ffeaa7', borderRadius: '4px', marginBottom: '20px' },
  table: { backgroundColor: 'white', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)', overflow: 'auto' },
  tableElement: { width: '100%', borderCollapse: 'collapse' },
  tableHeader: { backgroundColor: '#f8f9fa' },
  th: { padding: '16px', textAlign: 'left', fontSize: '14px', fontWeight: '600', color: '#333', borderBottom: '2px solid #e0e0e0' },
  tableRow: { borderBottom: '1px solid #e0e0e0' },
  td: { padding: '16px', fontSize: '14px', color: '#555' },
  severityBadge: { padding: '4px 8px', borderRadius: '4px', color: 'white', fontSize: '12px', fontWeight: '500' },
  email: { color: '#999', fontSize: '12px' },
  role: { color: '#666', fontSize: '11px', fontWeight: '500' },
  activityBadge: { padding: '4px 8px', backgroundColor: '#e9ecef', borderRadius: '4px', fontSize: '12px', fontWeight: '500' },
  empty: { padding: '40px', textAlign: 'center', color: '#666', fontSize: '16px' },
};

export default SuspiciousActivities;
