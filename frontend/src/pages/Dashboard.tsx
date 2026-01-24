import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { isAdmin } from '../types';
import { handleKeyPress } from '../utils/accessibility';

const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navigateToPage = (path: string) => {
    navigate(path);
  };

  return (
    <div style={styles.container}>
      {/* Semantic header with navigation */}
      <header style={styles.header} role="banner">
        <h1 style={styles.title} id="page-title">CrisisConnect Dashboard</h1>
        <nav aria-label="User navigation" style={styles.userInfo}>
          <span style={styles.userName} aria-label={`Logged in as ${user?.name}`}>
            {user?.name}
          </span>
          <span style={styles.userRole} aria-label={`Role: ${user?.role}`}>
            ({user?.role})
          </span>
          <button
            onClick={handleLogout}
            style={styles.logoutButton}
            aria-label="Logout from CrisisConnect"
          >
            Logout
          </button>
        </nav>
      </header>

      {/* Main content area */}
      <main id="main-content" tabIndex={-1} style={styles.content} role="main" aria-labelledby="page-title">
        <section style={styles.welcome} aria-label="Welcome message">
          <h2>Welcome to CrisisConnect</h2>
          <p>Humanitarian Aid Coordination Platform</p>
        </section>

        {/* Navigation cards */}
        <nav aria-label="Main navigation" style={styles.cardGrid}>
          <button
            onClick={() => navigateToPage('/needs')}
            onKeyDown={(e) => handleKeyPress(e, () => navigateToPage('/needs'))}
            style={styles.card}
            className="clickable-card"
            aria-label="View assistance needs. Browse all assistance requests in the system."
          >
            <h3 style={styles.cardTitle}>View Needs</h3>
            <p style={styles.cardText}>Browse assistance requests</p>
          </button>

          {(user?.role === 'FIELD_WORKER' || user?.role === 'NGO_STAFF' || user?.role === 'ADMIN') && (
            <button
              onClick={() => navigateToPage('/needs/create')}
              onKeyDown={(e) => handleKeyPress(e, () => navigateToPage('/needs/create'))}
              style={styles.card}
              className="clickable-card"
              aria-label="Create new need. Submit a new assistance request."
            >
              <h3 style={styles.cardTitle}>Create Need</h3>
              <p style={styles.cardText}>Submit new assistance request</p>
            </button>
          )}

          {(user?.role === 'NGO_STAFF' || user?.role === 'ADMIN') && (
            <button
              onClick={() => navigateToPage('/organizations')}
              onKeyDown={(e) => handleKeyPress(e, () => navigateToPage('/organizations'))}
              style={styles.card}
              className="clickable-card"
              aria-label="View organizations. See all registered aid organizations."
            >
              <h3 style={styles.cardTitle}>Organizations</h3>
              <p style={styles.cardText}>View aid organizations</p>
            </button>
          )}

          {user && isAdmin(user.role) && (
            <button
              onClick={() => navigateToPage('/admin')}
              onKeyDown={(e) => handleKeyPress(e, () => navigateToPage('/admin'))}
              style={{...styles.card, ...styles.adminCard}}
              className="clickable-card"
              aria-label="Admin Panel. System administration and monitoring. Requires administrator privileges."
            >
              <h3 style={styles.cardTitle}>
                <span aria-hidden="true">🛡️</span> Admin Panel
              </h3>
              <p style={styles.cardText}>System administration and monitoring</p>
            </button>
          )}
        </nav>

        {/* Info section */}
        <section style={styles.infoSection} aria-labelledby="security-heading">
          <h3 id="security-heading">Security & Privacy</h3>
          <ul style={styles.infoList}>
            <li>All sensitive data is encrypted at rest</li>
            <li>Role-based access control enforced</li>
            <li>GDPR, CCPA, and HIPAA compliant</li>
            <li>Comprehensive audit logging</li>
            <li>Privacy by design architecture</li>
          </ul>
        </section>
      </main>
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
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: {
    fontSize: '24px',
    fontWeight: 'bold',
    color: '#333',
  },
  userInfo: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
  },
  userName: {
    fontSize: '14px',
    fontWeight: '500',
  },
  userRole: {
    fontSize: '12px',
    color: '#666',
  },
  logoutButton: {
    padding: '8px 16px',
    fontSize: '14px',
    color: 'white',
    backgroundColor: '#dc3545',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  content: {
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '40px 20px',
  },
  welcome: {
    textAlign: 'center',
    marginBottom: '40px',
  },
  cardGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
    gap: '20px',
    marginBottom: '40px',
  },
  card: {
    backgroundColor: 'white',
    padding: '30px',
    borderRadius: '8px',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
    cursor: 'pointer',
    transition: 'transform 0.2s, box-shadow 0.2s',
    border: '2px solid transparent',
    textAlign: 'left',
    width: '100%',
    minHeight: '120px',
  },
  adminCard: {
    borderLeft: '4px solid #dc3545',
  },
  cardTitle: {
    fontSize: '18px',
    fontWeight: 'bold',
    marginBottom: '8px',
    margin: '0 0 8px 0',
  },
  cardText: {
    fontSize: '14px',
    color: '#666',
    margin: 0,
  },
  infoSection: {
    backgroundColor: 'white',
    padding: '30px',
    borderRadius: '8px',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  },
  infoList: {
    paddingLeft: '20px',
    lineHeight: '1.8',
    margin: '16px 0 0 0',
  },
};

export default Dashboard;
