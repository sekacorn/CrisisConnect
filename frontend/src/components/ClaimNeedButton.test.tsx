import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ClaimNeedButton from './ClaimNeedButton';
import { apiClient } from '../services/api';

jest.mock('../services/api', () => ({
  apiClient: {
    claimNeed: jest.fn(),
  },
}));

describe('ClaimNeedButton', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should not render for BENEFICIARY role', () => {
    const { container } = render(
      <ClaimNeedButton
        needId="test-id"
        userRole="BENEFICIARY"
        isAlreadyClaimed={false}
      />
    );

    expect(container.firstChild).toBeNull();
  });

  it('should not render for FIELD_WORKER role', () => {
    const { container } = render(
      <ClaimNeedButton
        needId="test-id"
        userRole="FIELD_WORKER"
        isAlreadyClaimed={false}
      />
    );

    expect(container.firstChild).toBeNull();
  });

  it('should render for NGO_STAFF role', () => {
    render(
      <ClaimNeedButton
        needId="test-id"
        userRole="NGO_STAFF"
        isAlreadyClaimed={false}
      />
    );

    expect(screen.getByText('Claim This Need')).toBeInTheDocument();
  });

  it('should render for ADMIN role', () => {
    render(
      <ClaimNeedButton
        needId="test-id"
        userRole="ADMIN"
        isAlreadyClaimed={false}
      />
    );

    expect(screen.getByText('Claim This Need')).toBeInTheDocument();
  });

  it('should show disabled state when already claimed', () => {
    render(
      <ClaimNeedButton
        needId="test-id"
        userRole="NGO_STAFF"
        isAlreadyClaimed={true}
      />
    );

    const button = screen.getByText('Already Claimed');
    expect(button).toBeDisabled();
  });

  it('should call API and onClaimSuccess on successful claim', async () => {
    const mockOnClaimSuccess = jest.fn();
    (apiClient.claimNeed as jest.Mock).mockResolvedValue({});

    render(
      <ClaimNeedButton
        needId="test-id"
        userRole="NGO_STAFF"
        isAlreadyClaimed={false}
        onClaimSuccess={mockOnClaimSuccess}
      />
    );

    const button = screen.getByText('Claim This Need');
    fireEvent.click(button);

    await waitFor(() => {
      expect(apiClient.claimNeed).toHaveBeenCalledWith('test-id');
      expect(mockOnClaimSuccess).toHaveBeenCalled();
    });
  });

  it('should show loading state while claiming', async () => {
    (apiClient.claimNeed as jest.Mock).mockImplementation(
      () => new Promise(resolve => setTimeout(resolve, 100))
    );

    render(
      <ClaimNeedButton
        needId="test-id"
        userRole="NGO_STAFF"
        isAlreadyClaimed={false}
      />
    );

    const button = screen.getByText('Claim This Need');
    fireEvent.click(button);

    expect(await screen.findByText('Claiming...')).toBeInTheDocument();
  });

  it('should display error message on 403 response', async () => {
    const mockOnClaimError = jest.fn();
    (apiClient.claimNeed as jest.Mock).mockRejectedValue({
      response: { status: 403 },
    });

    render(
      <ClaimNeedButton
        needId="test-id"
        userRole="NGO_STAFF"
        isAlreadyClaimed={false}
        onClaimError={mockOnClaimError}
      />
    );

    const button = screen.getByText('Claim This Need');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText(/outside your service area/)).toBeInTheDocument();
      expect(mockOnClaimError).toHaveBeenCalled();
    });
  });

  it('should display error message on 404 response', async () => {
    (apiClient.claimNeed as jest.Mock).mockRejectedValue({
      response: { status: 404 },
    });

    render(
      <ClaimNeedButton
        needId="test-id"
        userRole="NGO_STAFF"
        isAlreadyClaimed={false}
      />
    );

    const button = screen.getByText('Claim This Need');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText(/Need not found/)).toBeInTheDocument();
    });
  });

  it('should display error message on 429 response', async () => {
    (apiClient.claimNeed as jest.Mock).mockRejectedValue({
      response: { status: 429 },
      message: 'Rate limit exceeded',
    });

    render(
      <ClaimNeedButton
        needId="test-id"
        userRole="NGO_STAFF"
        isAlreadyClaimed={false}
      />
    );

    const button = screen.getByText('Claim This Need');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText(/Rate limit exceeded/)).toBeInTheDocument();
    });
  });

  it('should display custom error message from API', async () => {
    (apiClient.claimNeed as jest.Mock).mockRejectedValue({
      response: {
        status: 400,
        data: { message: 'Custom error message' },
      },
    });

    render(
      <ClaimNeedButton
        needId="test-id"
        userRole="NGO_STAFF"
        isAlreadyClaimed={false}
      />
    );

    const button = screen.getByText('Claim This Need');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Custom error message')).toBeInTheDocument();
    });
  });

  it('should display generic error message on unknown error', async () => {
    (apiClient.claimNeed as jest.Mock).mockRejectedValue(new Error('Network error'));

    render(
      <ClaimNeedButton
        needId="test-id"
        userRole="NGO_STAFF"
        isAlreadyClaimed={false}
      />
    );

    const button = screen.getByText('Claim This Need');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText(/Failed to claim need/)).toBeInTheDocument();
    });
  });
});
