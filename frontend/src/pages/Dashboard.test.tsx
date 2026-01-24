import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Dashboard from './Dashboard';
import { AuthProvider } from '../context/AuthContext';
import * as AuthContext from '../context/AuthContext';
import { UserRole } from '../types';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('Dashboard', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  const renderDashboard = (user: any) => {
    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user,
      token: 'mock-token',
      isAuthenticated: true,
      login: jest.fn(),
      logout: jest.fn(),
      loading: false,
    });

    return render(
      <BrowserRouter>
        <Dashboard />
      </BrowserRouter>
    );
  };

  it('should render dashboard with user info', () => {
    const user = {
      id: 'test-id',
      name: 'Test User',
      email: 'test@example.com',
      role: UserRole.FIELD_WORKER,
      isActive: true,
    };

    renderDashboard(user);

    expect(screen.getByText('CrisisConnect Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Test User')).toBeInTheDocument();
    expect(screen.getByText('(FIELD_WORKER)')).toBeInTheDocument();
  });

  it('should show View Needs card for all users', () => {
    const user = {
      id: 'test-id',
      name: 'Test User',
      email: 'test@example.com',
      role: UserRole.BENEFICIARY,
      isActive: true,
    };

    renderDashboard(user);

    expect(screen.getByText('View Needs')).toBeInTheDocument();
  });

  it('should show Create Need card for FIELD_WORKER', () => {
    const user = {
      id: 'test-id',
      name: 'Test User',
      email: 'test@example.com',
      role: UserRole.FIELD_WORKER,
      isActive: true,
    };

    renderDashboard(user);

    expect(screen.getByText('Create Need')).toBeInTheDocument();
  });

  it('should show Organizations card for NGO_STAFF', () => {
    const user = {
      id: 'test-id',
      name: 'Test User',
      email: 'test@example.com',
      role: UserRole.NGO_STAFF,
      isActive: true,
    };

    renderDashboard(user);

    expect(screen.getByText('Organizations')).toBeInTheDocument();
  });

  it('should show Admin Panel card for ADMIN', () => {
    const user = {
      id: 'test-id',
      name: 'Test User',
      email: 'test@example.com',
      role: UserRole.ADMIN,
      isActive: true,
    };

    renderDashboard(user);

    expect(screen.getByText('🛡️ Admin Panel')).toBeInTheDocument();
  });

  it('should not show Create Need card for BENEFICIARY', () => {
    const user = {
      id: 'test-id',
      name: 'Test User',
      email: 'test@example.com',
      role: UserRole.BENEFICIARY,
      isActive: true,
    };

    renderDashboard(user);

    expect(screen.queryByText('Create Need')).not.toBeInTheDocument();
  });

  it('should navigate to needs when View Needs card is clicked', () => {
    const user = {
      id: 'test-id',
      name: 'Test User',
      email: 'test@example.com',
      role: UserRole.ADMIN,
      isActive: true,
    };

    renderDashboard(user);

    const viewNeedsCard = screen.getByText('View Needs').closest('div');
    fireEvent.click(viewNeedsCard!);

    expect(mockNavigate).toHaveBeenCalledWith('/needs');
  });

  it('should navigate to create need when Create Need card is clicked', () => {
    const user = {
      id: 'test-id',
      name: 'Test User',
      email: 'test@example.com',
      role: UserRole.FIELD_WORKER,
      isActive: true,
    };

    renderDashboard(user);

    const createNeedCard = screen.getByText('Create Need').closest('div');
    fireEvent.click(createNeedCard!);

    expect(mockNavigate).toHaveBeenCalledWith('/needs/create');
  });

  it('should call logout and navigate to login when logout button is clicked', () => {
    const mockLogout = jest.fn();
    const user = {
      id: 'test-id',
      name: 'Test User',
      email: 'test@example.com',
      role: UserRole.ADMIN,
      isActive: true,
    };

    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user,
      token: 'mock-token',
      isAuthenticated: true,
      login: jest.fn(),
      logout: mockLogout,
      loading: false,
    });

    render(
      <BrowserRouter>
        <Dashboard />
      </BrowserRouter>
    );

    const logoutButton = screen.getByText('Logout');
    fireEvent.click(logoutButton);

    expect(mockLogout).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('should display security and privacy information', () => {
    const user = {
      id: 'test-id',
      name: 'Test User',
      email: 'test@example.com',
      role: UserRole.ADMIN,
      isActive: true,
    };

    renderDashboard(user);

    expect(screen.getByText('Security & Privacy')).toBeInTheDocument();
    expect(screen.getByText('All sensitive data is encrypted at rest')).toBeInTheDocument();
    expect(screen.getByText('GDPR, CCPA, and HIPAA compliant')).toBeInTheDocument();
  });
});
