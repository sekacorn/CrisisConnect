import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import CreateNeed from './CreateNeed';
import { apiClient } from '../services/api';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('../services/api', () => ({
  apiClient: {
    createNeed: jest.fn(),
  },
}));

describe('CreateNeed', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    jest.clearAllMocks();
  });

  it('should render create need form', () => {
    render(
      <BrowserRouter>
        <CreateNeed />
      </BrowserRouter>
    );

    expect(screen.getByText('Create Assistance Need')).toBeInTheDocument();
    expect(screen.getByLabelText(/Category/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Description/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Urgency Level/)).toBeInTheDocument();
  });

  it('should navigate to dashboard when back button is clicked', () => {
    render(
      <BrowserRouter>
        <CreateNeed />
      </BrowserRouter>
    );

    const backButton = screen.getByText('Back to Dashboard');
    fireEvent.click(backButton);

    expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
  });

  it('should update form fields when user types', () => {
    render(
      <BrowserRouter>
        <CreateNeed />
      </BrowserRouter>
    );

    const descriptionInput = screen.getByPlaceholderText('Describe the assistance needed');
    fireEvent.change(descriptionInput, { target: { value: 'Test description' } });

    expect(descriptionInput).toHaveValue('Test description');
  });

  it('should submit form with valid data', async () => {
    (apiClient.createNeed as jest.Mock).mockResolvedValue({ id: 'new-need-id' });

    render(
      <BrowserRouter>
        <CreateNeed />
      </BrowserRouter>
    );

    // Fill required fields
    const descriptionInput = screen.getByPlaceholderText('Describe the assistance needed');
    fireEvent.change(descriptionInput, { target: { value: 'Test description' } });

    const countryInput = screen.getByPlaceholderText('e.g., Lebanon');
    fireEvent.change(countryInput, { target: { value: 'Test Country' } });

    const submitButton = screen.getByText('Submit Need');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(apiClient.createNeed).toHaveBeenCalled();
      expect(mockNavigate).toHaveBeenCalledWith('/needs');
    });
  });

  it('should display error message on submission failure', async () => {
    const errorMessage = 'Failed to create need';
    (apiClient.createNeed as jest.Mock).mockRejectedValue({
      response: { data: { message: errorMessage } },
    });

    render(
      <BrowserRouter>
        <CreateNeed />
      </BrowserRouter>
    );

    // Fill required fields
    const descriptionInput = screen.getByPlaceholderText('Describe the assistance needed');
    fireEvent.change(descriptionInput, { target: { value: 'Test description' } });

    const countryInput = screen.getByPlaceholderText('e.g., Lebanon');
    fireEvent.change(countryInput, { target: { value: 'Test Country' } });

    const submitButton = screen.getByText('Submit Need');
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(errorMessage)).toBeInTheDocument();
    });
  });

  it('should display privacy notice', () => {
    render(
      <BrowserRouter>
        <CreateNeed />
      </BrowserRouter>
    );

    expect(screen.getByText(/Privacy & Security Notice/)).toBeInTheDocument();
    expect(screen.getByText(/All beneficiary information is encrypted/)).toBeInTheDocument();
  });

  it('should have all category options', () => {
    render(
      <BrowserRouter>
        <CreateNeed />
      </BrowserRouter>
    );

    const categorySelect = screen.getByLabelText(/Category/) as HTMLSelectElement;
    const options = Array.from(categorySelect.options).map(opt => opt.value);

    expect(options).toContain('FOOD');
    expect(options).toContain('SHELTER');
    expect(options).toContain('MEDICAL');
    expect(options).toContain('CLOTHING');
  });

  it('should have all urgency level options', () => {
    render(
      <BrowserRouter>
        <CreateNeed />
      </BrowserRouter>
    );

    const urgencySelect = screen.getByLabelText(/Urgency Level/) as HTMLSelectElement;
    const options = Array.from(urgencySelect.options).map(opt => opt.value);

    expect(options).toContain('LOW');
    expect(options).toContain('MEDIUM');
    expect(options).toContain('HIGH');
    expect(options).toContain('CRITICAL');
  });

  it('should disable submit button while loading', async () => {
    (apiClient.createNeed as jest.Mock).mockImplementation(
      () => new Promise(resolve => setTimeout(resolve, 100))
    );

    render(
      <BrowserRouter>
        <CreateNeed />
      </BrowserRouter>
    );

    // Fill required fields
    const descriptionInput = screen.getByPlaceholderText('Describe the assistance needed');
    fireEvent.change(descriptionInput, { target: { value: 'Test description' } });

    const countryInput = screen.getByPlaceholderText('e.g., Lebanon');
    fireEvent.change(countryInput, { target: { value: 'Test Country' } });

    const submitButton = screen.getByText('Submit Need');
    fireEvent.click(submitButton);

    // Button should show loading state
    await waitFor(() => {
      expect(screen.getByText('Submitting...')).toBeInTheDocument();
    });
  });
});
