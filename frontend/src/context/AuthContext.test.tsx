import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';
import { apiClient } from '../services/api';

jest.mock('../services/api');

const TestComponent = () => {
  const { user, isAuthenticated } = useAuth();
  return (
    <div>
      <div data-testid="authenticated">{isAuthenticated ? 'true' : 'false'}</div>
      <div data-testid="user">{user ? user.name : 'null'}</div>
    </div>
  );
};

describe('AuthContext', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  it('should provide auth context', () => {
    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
    expect(screen.getByTestId('user')).toHaveTextContent('null');
  });

  it('should load user when token exists', async () => {
    localStorage.setItem('token', 'test-token');

    const mockUser = {
      id: 'test-id',
      name: 'Test User',
      email: 'test@example.com',
      role: 'ADMIN' as any,
      isActive: true,
    };

    (apiClient.getCurrentUser as jest.Mock).mockResolvedValue(mockUser);

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('Test User');
    });
  });
});
