import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import NeedsList from './pages/NeedsList';
import CreateNeed from './pages/CreateNeed';
import NeedDetail from './components/NeedDetail';
import AdminDashboard from './pages/AdminDashboard';
import SkipLink from './components/SkipLink';
import LoadingSpinner from './components/LoadingSpinner';

const PrivateRoute: React.FC<{ children: React.ReactElement }> = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div
        role="status"
        aria-live="polite"
        style={{ textAlign: 'center', padding: '40px' }}
      >
        <LoadingSpinner message="Authenticating user" />
      </div>
    );
  }

  return isAuthenticated ? children : <Navigate to="/login" />;
};

const NeedDetailWrapper: React.FC = () => {
  const { user } = useAuth();
  return <NeedDetail userRole={user?.role || ''} />;
};

const App: React.FC = () => {
  return (
    <AuthProvider>
      <Router>
        {/* Skip Navigation Link for Keyboard Users */}
        <SkipLink targetId="main-content" label="Skip to main content" />

        {/* Main Application Routes */}
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/dashboard"
            element={
              <PrivateRoute>
                <Dashboard />
              </PrivateRoute>
            }
          />
          <Route
            path="/needs"
            element={
              <PrivateRoute>
                <NeedsList />
              </PrivateRoute>
            }
          />
          <Route
            path="/needs/create"
            element={
              <PrivateRoute>
                <CreateNeed />
              </PrivateRoute>
            }
          />
          <Route
            path="/needs/:id"
            element={
              <PrivateRoute>
                <NeedDetailWrapper />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <PrivateRoute>
                <AdminDashboard />
              </PrivateRoute>
            }
          />
          <Route path="/" element={<Navigate to="/dashboard" />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
};

export default App;
