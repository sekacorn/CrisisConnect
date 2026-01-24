import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import AdminDashboard from './AdminDashboard';
import * as AuthContext from '../context/AuthContext';
import { UserRole } from '../types';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

// Mock admin components
jest.mock('../components/admin/AdminStats', () => ({
  __esModule: true,
  default: () => <div>Admin Stats Component</div>,
}));

jest.mock('../components/admin/OrganizationManagement', () => ({
  __esModule: true,
  default: () => <div>Organization Management Component</div>,
}));

jest.mock('../components/admin/UserManagement', () => ({
  __esModule: true,
  default: () => <div>User Management Component</div>,
}));

jest.mock('../components/admin/AuditLogs', () => ({
  __esModule: true,
  default: () => <div>Audit Logs Component</div>,
}));

jest.mock('../components/admin/SuspiciousActivities', () => ({
  __esModule: true,
  default: () => <div>Suspicious Activities Component</div>,
}));

describe('AdminDashboard', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  const mockLogout = jest.fn();

  it('should render admin dashboard for admin user', () => {
    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: {
        id: 'admin-id',
        name: 'Admin User',
        email: 'admin@example.com',
        role: UserRole.ADMIN,
        isActive: true,
      },
      token: 'mock-token',
      isAuthenticated: true,
      login: jest.fn(),
      logout: mockLogout,
      loading: false,
    });

    render(
      <BrowserRouter>
        <AdminDashboard />
      </BrowserRouter>
    );

    expect(screen.getByText('Admin Dashboard')).toBeInTheDocument();
    expect(screen.getByText('CrisisConnect Administration Panel')).toBeInTheDocument();
  });

  it('should redirect non-admin users to dashboard', () => {
    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: {
        id: 'user-id',
        name: 'Regular User',
        email: 'user@example.com',
        role: UserRole.FIELD_WORKER,
        isActive: true,
      },
      token: 'mock-token',
      isAuthenticated: true,
      login: jest.fn(),
      logout: mockLogout,
      loading: false,
    });

    render(
      <BrowserRouter>
        <AdminDashboard />
      </BrowserRouter>
    );

    expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
  });

  it('should display stats tab by default', () => {
    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: {
        id: 'admin-id',
        name: 'Admin User',
        email: 'admin@example.com',
        role: UserRole.ADMIN,
        isActive: true,
      },
      token: 'mock-token',
      isAuthenticated: true,
      login: jest.fn(),
      logout: mockLogout,
      loading: false,
    });

    render(
      <BrowserRouter>
        <AdminDashboard />
      </BrowserRouter>
    );

    expect(screen.getByText('Admin Stats Component')).toBeInTheDocument();
  });

  it('should switch to organizations tab when clicked', () => {
    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: {
        id: 'admin-id',
        name: 'Admin User',
        email: 'admin@example.com',
        role: UserRole.ADMIN,
        isActive: true,
      },
      token: 'mock-token',
      isAuthenticated: true,
      login: jest.fn(),
      logout: mockLogout,
      loading: false,
    });

    render(
      <BrowserRouter>
        <AdminDashboard />
      </BrowserRouter>
    );

    const orgsTab = screen.getByText('Organizations');
    fireEvent.click(orgsTab);

    expect(screen.getByText('Organization Management Component')).toBeInTheDocument();
  });

  it('should switch to users tab when clicked', () => {
    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: {
        id: 'admin-id',
        name: 'Admin User',
        email: 'admin@example.com',
        role: UserRole.ADMIN,
        isActive: true,
      },
      token: 'mock-token',
      isAuthenticated: true,
      login: jest.fn(),
      logout: mockLogout,
      loading: false,
    });

    render(
      <BrowserRouter>
        <AdminDashboard />
      </BrowserRouter>
    );

    const usersTab = screen.getByText('Users');
    fireEvent.click(usersTab);

    expect(screen.getByText('User Management Component')).toBeInTheDocument();
  });

  it('should navigate to main dashboard when back button is clicked', () => {
    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: {
        id: 'admin-id',
        name: 'Admin User',
        email: 'admin@example.com',
        role: UserRole.ADMIN,
        isActive: true,
      },
      token: 'mock-token',
      isAuthenticated: true,
      login: jest.fn(),
      logout: mockLogout,
      loading: false,
    });

    render(
      <BrowserRouter>
        <AdminDashboard />
      </BrowserRouter>
    );

    const backButton = screen.getByText('Main Dashboard');
    fireEvent.click(backButton);

    expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
  });

  it('should call logout when logout button is clicked', () => {
    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: {
        id: 'admin-id',
        name: 'Admin User',
        email: 'admin@example.com',
        role: UserRole.ADMIN,
        isActive: true,
      },
      token: 'mock-token',
      isAuthenticated: true,
      login: jest.fn(),
      logout: mockLogout,
      loading: false,
    });

    render(
      <BrowserRouter>
        <AdminDashboard />
      </BrowserRouter>
    );

    const logoutButton = screen.getByText('Logout');
    fireEvent.click(logoutButton);

    expect(mockLogout).toHaveBeenCalled();
  });
});
