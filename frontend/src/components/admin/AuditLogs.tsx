import React, { useEffect, useState } from 'react';
import { apiClient } from '../../services/api';
import { AuditLogEntry } from '../../types';

/**
 * Audit Logs Viewer
 *
 * Features:
 * - View all system audit logs
 * - Filter by action type
 * - Pagination support
 * - Export capabilities (future)
 */
const AuditLogs: React.FC = () => {
  const [logs, setLogs] = useState<AuditLogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionFilter, setActionFilter] = useState('');
  const [page, setPage] = useState(0);

  useEffect(() => {
    loadLogs();
  }, [page, actionFilter]);

  const loadLogs = async () => {
    setLoading(true);
    try {
      const response = await apiClient.getAuditLogs(page, 50, actionFilter || undefined);
      setLogs(response.content || response);
    } catch (err: any) {
      setError('Failed to load audit logs');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div style={styles.loading}>Loading audit logs...</div>;

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={styles.title}>Audit Logs</h2>
        <div style={styles.filters}>
          <input
            type="text"
            value={actionFilter}
            onChange={(e) => setActionFilter(e.target.value)}
            placeholder="Filter by action type..."
            style={styles.filterInput}
          />
        </div>
      </div>

      {error && <div style={styles.error}>{error}</div>}

      <div style={styles.table}>
        <table style={styles.tableElement}>
          <thead>
            <tr style={styles.tableHeader}>
              <th style={styles.th}>Timestamp</th>
              <th style={styles.th}>User</th>
              <th style={styles.th}>Action</th>
              <th style={styles.th}>Entity</th>
              <th style={styles.th}>Details</th>
              <th style={styles.th}>IP Address</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id} style={styles.tableRow}>
                <td style={styles.td}>{new Date(log.createdAt).toLocaleString()}</td>
                <td style={styles.td}>
                  {log.userName || 'N/A'}<br />
                  <small style={styles.email}>{log.userEmail || 'N/A'}</small>
                </td>
                <td style={styles.td}>
                  <span style={styles.actionBadge}>{log.actionType}</span>
                </td>
                <td style={styles.td}>
                  {log.entityType}<br />
                  <small style={styles.entityId}>{log.entityId?.substring(0, 8)}...</small>
                </td>
                <td style={styles.td}>
                  <div style={styles.details}>{log.details || '-'}</div>
                </td>
                <td style={styles.td}>{log.ipAddress}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {logs.length === 0 && <div style={styles.empty}>No audit logs found</div>}
      </div>

      <div style={styles.pagination}>
        <button
          onClick={() => setPage(Math.max(0, page - 1))}
          disabled={page === 0}
          style={styles.paginationButton}
        >
          Previous
        </button>
        <span style={styles.pageInfo}>Page {page + 1}</span>
        <button
          onClick={() => setPage(page + 1)}
          disabled={logs.length < 50}
          style={styles.paginationButton}
        >
          Next
        </button>
      </div>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: { maxWidth: '1400px', margin: '0 auto' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' },
  title: { fontSize: '24px', fontWeight: 'bold', margin: 0 },
  filters: { display: 'flex', gap: '12px' },
  filterInput: { padding: '8px 12px', border: '1px solid #ddd', borderRadius: '4px', fontSize: '14px', width: '250px' },
  loading: { textAlign: 'center', padding: '40px', fontSize: '18px' },
  error: { padding: '16px', backgroundColor: '#f8d7da', color: '#721c24', borderRadius: '4px', marginBottom: '20px' },
  table: { backgroundColor: 'white', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)', overflow: 'auto' },
  tableElement: { width: '100%', borderCollapse: 'collapse' },
  tableHeader: { backgroundColor: '#f8f9fa' },
  th: { padding: '16px', textAlign: 'left', fontSize: '14px', fontWeight: '600', color: '#333', borderBottom: '2px solid #e0e0e0', whiteSpace: 'nowrap' },
  tableRow: { borderBottom: '1px solid #e0e0e0' },
  td: { padding: '16px', fontSize: '13px', color: '#555', maxWidth: '200px' },
  email: { color: '#999', fontSize: '12px' },
  actionBadge: { padding: '4px 8px', backgroundColor: '#007bff', color: 'white', borderRadius: '4px', fontSize: '11px', fontWeight: '500' },
  entityId: { color: '#999', fontSize: '11px', fontFamily: 'monospace' },
  details: { maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  empty: { padding: '40px', textAlign: 'center', color: '#666' },
  pagination: { marginTop: '24px', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '16px' },
  paginationButton: { padding: '8px 16px', backgroundColor: '#007bff', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '14px' },
  pageInfo: { fontSize: '14px', color: '#555' },
};

export default AuditLogs;
