import React, { useEffect, useState } from 'react';
import { apiClient } from '../../services/api';
import { User, UserRole } from '../../types';

/**
 * User Management Component
 *
 * Features:
 * - View all users with pagination
 * - Update user roles
 * - Activate/deactivate users
 * - View user audit history
 */
const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [editedUser, setEditedUser] = useState<Partial<User>>({});
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const response = await apiClient.getAdminUsers(0, 100);
      setUsers(response.content || response);
    } catch (err: any) {
      setError('Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async () => {
    if (!selectedUser) return;

    setUpdating(true);
    try {
      await apiClient.updateUser(selectedUser.id, {
        name: editedUser.name,
        role: editedUser.role as any,
        isActive: editedUser.isActive
      });
      setSelectedUser(null);
      loadUsers();
    } catch (err: any) {
      alert('Failed to update user: ' + (err.response?.data?.message || err.message));
    } finally {
      setUpdating(false);
    }
  };

  const openEdit = (user: User) => {
    setSelectedUser(user);
    setEditedUser({
      name: user.name,
      role: user.role,
      isActive: user.isActive
    });
  };

  if (loading) return <div style={styles.loading}>Loading users...</div>;

  return (
    <div style={styles.container}>
      <h2 style={styles.title}>User Management</h2>

      {error && <div style={styles.error}>{error}</div>}

      <div style={styles.table}>
        <table style={styles.tableElement}>
          <thead>
            <tr style={styles.tableHeader}>
              <th style={styles.th}>Name</th>
              <th style={styles.th}>Email</th>
              <th style={styles.th}>Role</th>
              <th style={styles.th}>Status</th>
              <th style={styles.th}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id} style={styles.tableRow}>
                <td style={styles.td}>{user.name}</td>
                <td style={styles.td}>{user.email}</td>
                <td style={styles.td}>
                  <span style={styles.roleBadge}>{user.role}</span>
                </td>
                <td style={styles.td}>
                  <span style={{
                    ...styles.statusBadge,
                    backgroundColor: user.isActive ? '#28a745' : '#dc3545'
                  }}>
                    {user.isActive ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td style={styles.td}>
                  <button onClick={() => openEdit(user)} style={styles.actionButton}>
                    Edit
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Edit Modal */}
      {selectedUser && (
        <div style={styles.modal}>
          <div style={styles.modalContent}>
            <div style={styles.modalHeader}>
              <h3 style={styles.modalTitle}>Edit User</h3>
              <button onClick={() => setSelectedUser(null)} style={styles.closeButton}>×</button>
            </div>

            <div style={styles.modalBody}>
              <div style={styles.formGroup}>
                <label style={styles.label}>Name:</label>
                <input
                  type="text"
                  value={editedUser.name || ''}
                  onChange={(e) => setEditedUser({...editedUser, name: e.target.value})}
                  style={styles.input}
                />
              </div>

              <div style={styles.formGroup}>
                <label style={styles.label}>Role:</label>
                <select
                  value={editedUser.role || ''}
                  onChange={(e) => setEditedUser({...editedUser, role: e.target.value as UserRole})}
                  style={styles.select}
                >
                  <option value={UserRole.BENEFICIARY}>BENEFICIARY</option>
                  <option value={UserRole.FIELD_WORKER}>FIELD_WORKER</option>
                  <option value={UserRole.NGO_STAFF}>NGO_STAFF</option>
                  <option value={UserRole.ADMIN}>ADMIN</option>
                </select>
              </div>

              <div style={styles.formGroup}>
                <label style={styles.checkboxLabel}>
                  <input
                    type="checkbox"
                    checked={editedUser.isActive || false}
                    onChange={(e) => setEditedUser({...editedUser, isActive: e.target.checked})}
                  />
                  Active
                </label>
              </div>
            </div>

            <div style={styles.modalFooter}>
              <button
                onClick={handleUpdate}
                disabled={updating}
                style={{...styles.button, backgroundColor: '#007bff'}}
              >
                {updating ? 'Updating...' : 'Save Changes'}
              </button>
              <button
                onClick={() => setSelectedUser(null)}
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
  container: { maxWidth: '1400px', margin: '0 auto' },
  title: { fontSize: '24px', fontWeight: 'bold', marginBottom: '24px' },
  loading: { textAlign: 'center', padding: '40px', fontSize: '18px' },
  error: { padding: '16px', backgroundColor: '#f8d7da', color: '#721c24', borderRadius: '4px', marginBottom: '20px' },
  table: { backgroundColor: 'white', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)', overflow: 'hidden' },
  tableElement: { width: '100%', borderCollapse: 'collapse' },
  tableHeader: { backgroundColor: '#f8f9fa' },
  th: { padding: '16px', textAlign: 'left', fontSize: '14px', fontWeight: '600', color: '#333', borderBottom: '2px solid #e0e0e0' },
  tableRow: { borderBottom: '1px solid #e0e0e0' },
  td: { padding: '16px', fontSize: '14px', color: '#555' },
  roleBadge: { padding: '4px 8px', backgroundColor: '#e9ecef', borderRadius: '4px', fontSize: '12px', fontWeight: '500' },
  statusBadge: { padding: '4px 8px', borderRadius: '4px', color: 'white', fontSize: '12px', fontWeight: '500' },
  actionButton: { padding: '6px 12px', backgroundColor: '#007bff', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '13px' },
  modal: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 },
  modalContent: { backgroundColor: 'white', borderRadius: '8px', width: '90%', maxWidth: '500px' },
  modalHeader: { padding: '20px', borderBottom: '1px solid #e0e0e0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  modalTitle: { margin: 0, fontSize: '20px', fontWeight: 'bold' },
  closeButton: { background: 'none', border: 'none', fontSize: '32px', cursor: 'pointer', color: '#666' },
  modalBody: { padding: '20px' },
  formGroup: { marginBottom: '16px' },
  label: { display: 'block', marginBottom: '8px', fontSize: '14px', fontWeight: '500' },
  input: { width: '100%', padding: '10px', border: '1px solid #ddd', borderRadius: '4px', fontSize: '14px' },
  select: { width: '100%', padding: '10px', border: '1px solid #ddd', borderRadius: '4px', fontSize: '14px' },
  checkboxLabel: { display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px' },
  modalFooter: { padding: '20px', borderTop: '1px solid #e0e0e0', display: 'flex', gap: '12px', justifyContent: 'flex-end' },
  button: { padding: '10px 20px', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '14px', fontWeight: '500' },
};

export default UserManagement;
