import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import NeedDetail from './NeedDetail';
import { apiClient } from '../services/api';
import { NeedCategory, UrgencyLevel, NeedStatus } from '../types';

jest.mock('../services/api', () => ({
  apiClient: {
    getNeedById: jest.fn(),
  },
}));

jest.mock('./RestrictedInfo', () => ({
  __esModule: true,
  default: () => <div>Restricted Info Component</div>,
}));

jest.mock('./ClaimNeedButton', () => ({
  __esModule: true,
  default: () => <div>Claim Need Button Component</div>,
}));

// Mock useParams to return test ID
const mockParams = { id: 'test-need-id' };
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useParams: () => mockParams,
  useNavigate: () => jest.fn(),
}));

describe('NeedDetail', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderComponent = (userRole: string) => {
    return render(<NeedDetail userRole={userRole} />);
  };

  const mockRedactedNeed = {
    id: 'test-need-id',
    category: NeedCategory.FOOD,
    status: NeedStatus.NEW,
    urgencyLevel: UrgencyLevel.HIGH,
    country: 'Test Country',
    regionOrState: 'Test Region',
    generalizedVulnerabilityFlags: 'Elderly',
    createdAt: '2024-01-01T00:00:00Z',
  };

  const mockFullNeed = {
    ...mockRedactedNeed,
    description: 'Full description',
    city: 'Test City',
    locationText: 'Test Location',
    vulnerabilityFlags: 'Detailed flags',
    beneficiaryName: 'John Doe',
    beneficiaryPhone: '+1234567890',
    beneficiaryEmail: 'john@example.com',
    sensitiveNotes: 'Sensitive information',
    assignedOrganizationId: 'org-id',
    assignedOrganizationName: 'Test Org',
  };

  it('should display loading state initially', () => {
    (apiClient.getNeedById as jest.Mock).mockImplementation(
      () => new Promise(() => {}) // Never resolves
    );

    renderComponent('ADMIN');

    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('should call API to load redacted need for unauthorized user', async () => {
    (apiClient.getNeedById as jest.Mock).mockResolvedValue(mockRedactedNeed);

    renderComponent('BENEFICIARY');

    await waitFor(() => {
      expect(apiClient.getNeedById).toHaveBeenCalledWith('test-need-id');
      expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    });
  });

  it('should call API to load full need for authorized user', async () => {
    (apiClient.getNeedById as jest.Mock).mockResolvedValue(mockFullNeed);

    renderComponent('ADMIN');

    await waitFor(() => {
      expect(apiClient.getNeedById).toHaveBeenCalledWith('test-need-id');
      expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    });
  });

  it('should display 404 error for not found need', async () => {
    (apiClient.getNeedById as jest.Mock).mockRejectedValue({
      response: { status: 404 },
    });

    renderComponent('ADMIN');

    await waitFor(() => {
      expect(screen.getByText(/Need not found or you do not have permission/)).toBeInTheDocument();
    });
  });

  it('should display rate limit error for 429 response', async () => {
    (apiClient.getNeedById as jest.Mock).mockRejectedValue({
      response: { status: 429 },
      message: 'Too many requests',
    });

    renderComponent('ADMIN');

    await waitFor(() => {
      expect(screen.getByText('Too many requests')).toBeInTheDocument();
    });
  });

  it('should display generic error for other failures', async () => {
    (apiClient.getNeedById as jest.Mock).mockRejectedValue({
      response: { status: 500 },
    });

    renderComponent('ADMIN');

    await waitFor(() => {
      expect(screen.getByText(/Failed to load need details/)).toBeInTheDocument();
    });
  });

  it('should call API with correct need ID', async () => {
    (apiClient.getNeedById as jest.Mock).mockResolvedValue(mockRedactedNeed);

    renderComponent('ADMIN');

    await waitFor(() => {
      expect(apiClient.getNeedById).toHaveBeenCalledWith('test-need-id');
    });
  });
});
