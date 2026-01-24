import React from 'react';
import { render, screen } from '@testing-library/react';
import RestrictedInfo from './RestrictedInfo';

describe('RestrictedInfo', () => {
  it('should render with label', () => {
    render(<RestrictedInfo label="Beneficiary Name" />);

    expect(screen.getByText('Beneficiary Name')).toBeInTheDocument();
  });

  it('should display default reason when not provided', () => {
    render(<RestrictedInfo label="Beneficiary Contact" />);

    expect(screen.getByText('Information restricted - Authorization required')).toBeInTheDocument();
  });

  it('should display custom reason when provided', () => {
    render(<RestrictedInfo label="Location" reason="Available to assigned organization only" />);

    expect(screen.getByText('Available to assigned organization only')).toBeInTheDocument();
  });

  it('should render with correct styling', () => {
    const { container } = render(<RestrictedInfo label="Test Label" />);

    const wrapper = container.firstChild as HTMLElement;
    expect(wrapper).toHaveStyle({
      backgroundColor: '#f5f5f5',
      borderLeft: '4px solid #999',
    });
  });
});
