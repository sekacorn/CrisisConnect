import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { isAdmin } from '../types';
import AdminStats from '../components/admin/AdminStats';
import OrganizationManagement from '../components/admin/OrganizationManagement';
import UserManagement from '../components/admin/UserManagement';
import AuditLogs from '../components/admin/AuditLogs';
import SuspiciousActivities from '../components/admin/SuspiciousActivities';

/**
 * Admin Dashboard - Main admin panel
 *
 * Features:
 * - Dashboard statistics and analytics
 * - Organization verification
 * - User management
 * - Audit log viewing
 * - Suspicious activity monitoring
 *
 * Only accessible to ADMIN role
 */
const AdminDashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'stats' | 'orgs' | 'users' | 'audit' | 'suspicious'>('stats');

  // Redirect if not admin
  if (!user || !isAdmin(user.role)) {
    navigate('/dashboard');
    return null;
  }

  return (
    <div style={styles.container}>
      {/* Header */}
      <div style={styles.header}>
        <div>
          <h1 style={styles.title}>Admin Dashboard</h1>
          <p style={styles.subtitle}>CrisisConnect Administration Panel</p>
        </div>
        <div style={styles.headerActions}>
          <button onClick={() => navigate('/dashboard')} style={styles.backButton}>
            Main Dashboard
          </button>
          <button onClick={logout} style={styles.logoutButton}>
            Logout
          </button>
        </div>
      </div>

      {/* Tab Navigation */}
      <div style={styles.tabContainer}>
        <button
          onClick={() => setActiveTab('stats')}
          style={activeTab === 'stats' ? styles.tabActive : styles.tab}
        >
          Statistics
        </button>
        <button
          onClick={() => setActiveTab('orgs')}
          style={activeTab === 'orgs' ? styles.tabActive : styles.tab}
        >
          Organizations
        </button>
        <button
          onClick={() => setActiveTab('users')}
          style={activeTab === 'users' ? styles.tabActive : styles.tab}
        >
          Users
        </button>
        <button
          onClick={() => setActiveTab('audit')}
          style={activeTab === 'audit' ? styles.tabActive : styles.tab}
        >
          Audit Logs
        </button>
        <button
          onClick={() => setActiveTab('suspicious')}
          style={activeTab === 'suspicious' ? styles.tabActive : styles.tab}
        >
          Suspicious Activities
        </button>
      </div>

      {/* Tab Content */}
      <div style={styles.content}>
        {activeTab === 'stats' && <AdminStats />}
        {activeTab === 'orgs' && <OrganizationManagement />}
        {activeTab === 'users' && <UserManagement />}
        {activeTab === 'audit' && <AuditLogs />}
        {activeTab === 'suspicious' && <SuspiciousActivities />}
      </div>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    minHeight: '100vh',
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: 'white',
    padding: '20px 40px',
    borderBottom: '2px solid #e0e0e0',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: {
    fontSize: '28px',
    fontWeight: 'bold',
    margin: 0,
    color: '#333',
  },
  subtitle: {
    fontSize: '14px',
    color: '#666',
    margin: '4px 0 0 0',
  },
  headerActions: {
    display: 'flex',
    gap: '12px',
  },
  backButton: {
    padding: '10px 20px',
    backgroundColor: '#6c757d',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '14px',
  },
  logoutButton: {
    padding: '10px 20px',
    backgroundColor: '#dc3545',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '14px',
  },
  tabContainer: {
    backgroundColor: 'white',
    borderBottom: '1px solid #e0e0e0',
    display: 'flex',
    padding: '0 40px',
  },
  tab: {
    padding: '16px 24px',
    backgroundColor: 'transparent',
    border: 'none',
    borderBottom: '3px solid transparent',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: '500',
    color: '#666',
    transition: 'all 0.2s',
  },
  tabActive: {
    padding: '16px 24px',
    backgroundColor: 'transparent',
    border: 'none',
    borderBottom: '3px solid #007bff',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: '500',
    color: '#007bff',
  },
  content: {
    padding: '40px',
  },
};

export default AdminDashboard;
