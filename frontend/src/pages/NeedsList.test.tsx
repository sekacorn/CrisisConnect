import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import NeedsList from './NeedsList';
import { apiClient } from '../services/api';
import { NeedCategory, UrgencyLevel, NeedStatus } from '../types';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('../services/api', () => ({
  apiClient: {
    getAllNeeds: jest.fn(),
  },
}));

describe('NeedsList', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    jest.clearAllMocks();
  });

  const mockNeeds = [
    {
      id: 'need-1',
      category: NeedCategory.FOOD,
      status: NeedStatus.NEW,
      urgencyLevel: UrgencyLevel.HIGH,
      country: 'Test Country',
      regionOrState: 'Test Region',
      generalizedVulnerabilityFlags: 'Elderly',
      createdAt: '2024-01-01T00:00:00Z',
    },
    {
      id: 'need-2',
      category: NeedCategory.SHELTER,
      status: NeedStatus.ASSIGNED,
      urgencyLevel: UrgencyLevel.CRITICAL,
      country: 'Another Country',
      regionOrState: 'Another Region',
      generalizedVulnerabilityFlags: 'Children',
      createdAt: '2024-01-02T00:00:00Z',
    },
  ];

  it('should display loading state initially', () => {
    (apiClient.getAllNeeds as jest.Mock).mockImplementation(
      () => new Promise(() => {}) // Never resolves
    );

    render(
      <BrowserRouter>
        <NeedsList />
      </BrowserRouter>
    );

    expect(screen.getByText('Loading needs...')).toBeInTheDocument();
  });

  it('should display needs list after loading', async () => {
    (apiClient.getAllNeeds as jest.Mock).mockResolvedValue(mockNeeds);

    render(
      <BrowserRouter>
        <NeedsList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Assistance Needs')).toBeInTheDocument();
    });

    expect(screen.getByText('FOOD')).toBeInTheDocument();
    expect(screen.getByText('SHELTER')).toBeInTheDocument();
    expect(screen.getByText('Test Country')).toBeInTheDocument();
  });

  it('should display privacy notice', async () => {
    (apiClient.getAllNeeds as jest.Mock).mockResolvedValue(mockNeeds);

    render(
      <BrowserRouter>
        <NeedsList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Privacy Notice:/)).toBeInTheDocument();
    });

    expect(screen.getByText(/This list shows redacted information only/)).toBeInTheDocument();
  });

  it('should display error message on failure', async () => {
    const errorMessage = 'Failed to load needs';
    (apiClient.getAllNeeds as jest.Mock).mockRejectedValue({
      response: { data: { message: errorMessage } },
    });

    render(
      <BrowserRouter>
        <NeedsList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(errorMessage)).toBeInTheDocument();
    });
  });

  it('should display empty state when no needs', async () => {
    (apiClient.getAllNeeds as jest.Mock).mockResolvedValue([]);

    render(
      <BrowserRouter>
        <NeedsList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('No assistance needs found.')).toBeInTheDocument();
    });
  });

  it('should navigate to dashboard when back button is clicked', async () => {
    (apiClient.getAllNeeds as jest.Mock).mockResolvedValue(mockNeeds);

    render(
      <BrowserRouter>
        <NeedsList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Assistance Needs')).toBeInTheDocument();
    });

    const backButton = screen.getByText('Back to Dashboard');
    fireEvent.click(backButton);

    expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
  });

  it('should navigate to need detail when card is clicked', async () => {
    (apiClient.getAllNeeds as jest.Mock).mockResolvedValue(mockNeeds);

    render(
      <BrowserRouter>
        <NeedsList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('FOOD')).toBeInTheDocument();
    });

    const firstCard = screen.getByText('FOOD').closest('div')?.parentElement;
    fireEvent.click(firstCard!);

    expect(mockNavigate).toHaveBeenCalledWith('/needs/need-1');
  });

  it('should display status badge with correct color', async () => {
    (apiClient.getAllNeeds as jest.Mock).mockResolvedValue(mockNeeds);

    render(
      <BrowserRouter>
        <NeedsList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('NEW')).toBeInTheDocument();
    });

    const newBadge = screen.getByText('NEW');
    expect(newBadge).toHaveStyle({ backgroundColor: '#17a2b8' });
  });

  it('should display urgency badge with correct color', async () => {
    (apiClient.getAllNeeds as jest.Mock).mockResolvedValue(mockNeeds);

    render(
      <BrowserRouter>
        <NeedsList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('HIGH')).toBeInTheDocument();
    });

    const highBadge = screen.getByText('HIGH');
    expect(highBadge).toHaveStyle({ backgroundColor: '#fd7e14' });
  });

  it('should display created date formatted correctly', async () => {
    (apiClient.getAllNeeds as jest.Mock).mockResolvedValue(mockNeeds);

    render(
      <BrowserRouter>
        <NeedsList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/1\/1\/2024/)).toBeInTheDocument();
    });
  });

  it('should display vulnerability flags when present', async () => {
    (apiClient.getAllNeeds as jest.Mock).mockResolvedValue(mockNeeds);

    render(
      <BrowserRouter>
        <NeedsList />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Elderly')).toBeInTheDocument();
      expect(screen.getByText('Children')).toBeInTheDocument();
    });
  });
});
