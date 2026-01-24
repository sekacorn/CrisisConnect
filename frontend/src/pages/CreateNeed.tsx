import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../services/api';
import { NeedCategory, UrgencyLevel, CreateNeedRequest } from '../types';

const CreateNeed: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [formData, setFormData] = useState<CreateNeedRequest>({
    category: NeedCategory.FOOD,
    description: '',
    country: '',
    regionOrState: '',
    city: '',
    locationText: '',
    urgencyLevel: UrgencyLevel.MEDIUM,
    vulnerabilityFlags: '',
    beneficiaryName: '',
    beneficiaryPhone: '',
    beneficiaryEmail: '',
    sensitiveNotes: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await apiClient.createNeed(formData);
      navigate('/needs');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create need');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>Create Assistance Need</h1>
        <button onClick={() => navigate('/dashboard')} style={styles.backButton}>
          Back to Dashboard
        </button>
      </div>

      <div style={styles.card}>
        <form onSubmit={handleSubmit}>
          {error && <div style={styles.error}>{error}</div>}

          <div style={styles.section}>
            <h3 style={styles.sectionTitle}>Need Information</h3>

            <div style={styles.field}>
              <label style={styles.label}>Category *</label>
              <select name="category" value={formData.category} onChange={handleChange} style={styles.select} required>
                {Object.values(NeedCategory).map(cat => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>

            <div style={styles.field}>
              <label style={styles.label}>Description *</label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                style={styles.textarea}
                rows={4}
                required
                placeholder="Describe the assistance needed"
              />
            </div>

            <div style={styles.field}>
              <label style={styles.label}>Urgency Level *</label>
              <select name="urgencyLevel" value={formData.urgencyLevel} onChange={handleChange} style={styles.select} required>
                {Object.values(UrgencyLevel).map(level => (
                  <option key={level} value={level}>{level}</option>
                ))}
              </select>
            </div>
          </div>

          <div style={styles.section}>
            <h3 style={styles.sectionTitle}>Location</h3>

            <div style={styles.field}>
              <label style={styles.label}>Country *</label>
              <input
                type="text"
                name="country"
                value={formData.country}
                onChange={handleChange}
                style={styles.input}
                required
              />
            </div>

            <div style={styles.field}>
              <label style={styles.label}>Region/State</label>
              <input
                type="text"
                name="regionOrState"
                value={formData.regionOrState}
                onChange={handleChange}
                style={styles.input}
              />
            </div>

            <div style={styles.field}>
              <label style={styles.label}>City</label>
              <input
                type="text"
                name="city"
                value={formData.city}
                onChange={handleChange}
                style={styles.input}
              />
            </div>

            <div style={styles.field}>
              <label style={styles.label}>Location Details</label>
              <input
                type="text"
                name="locationText"
                value={formData.locationText}
                onChange={handleChange}
                style={styles.input}
                placeholder="e.g., Near main square"
              />
            </div>
          </div>

          <div style={styles.section}>
            <h3 style={styles.sectionTitle}>Sensitive Information (Encrypted)</h3>
            <p style={styles.privacyNotice}>This information will be encrypted and only accessible to authorized organizations</p>

            <div style={styles.field}>
              <label style={styles.label}>Beneficiary Name</label>
              <input
                type="text"
                name="beneficiaryName"
                value={formData.beneficiaryName}
                onChange={handleChange}
                style={styles.input}
              />
            </div>

            <div style={styles.field}>
              <label style={styles.label}>Beneficiary Phone</label>
              <input
                type="tel"
                name="beneficiaryPhone"
                value={formData.beneficiaryPhone}
                onChange={handleChange}
                style={styles.input}
              />
            </div>

            <div style={styles.field}>
              <label style={styles.label}>Beneficiary Email</label>
              <input
                type="email"
                name="beneficiaryEmail"
                value={formData.beneficiaryEmail}
                onChange={handleChange}
                style={styles.input}
              />
            </div>

            <div style={styles.field}>
              <label style={styles.label}>Sensitive Notes</label>
              <textarea
                name="sensitiveNotes"
                value={formData.sensitiveNotes}
                onChange={handleChange}
                style={styles.textarea}
                rows={3}
                placeholder="Any sensitive information about the beneficiary"
              />
            </div>
          </div>

          <div style={styles.actions}>
            <button type="button" onClick={() => navigate('/dashboard')} style={styles.cancelButton}>
              Cancel
            </button>
            <button type="submit" disabled={loading} style={styles.submitButton}>
              {loading ? 'Creating...' : 'Create Need'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    minHeight: '100vh',
    backgroundColor: '#f5f5f5',
    padding: '20px',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '20px',
  },
  title: {
    fontSize: '28px',
    fontWeight: 'bold',
  },
  backButton: {
    padding: '10px 20px',
    fontSize: '14px',
    backgroundColor: '#6c757d',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  card: {
    backgroundColor: 'white',
    borderRadius: '8px',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
    padding: '30px',
    maxWidth: '800px',
    margin: '0 auto',
  },
  section: {
    marginBottom: '30px',
  },
  sectionTitle: {
    fontSize: '18px',
    fontWeight: 'bold',
    marginBottom: '15px',
    borderBottom: '2px solid #007bff',
    paddingBottom: '8px',
  },
  field: {
    marginBottom: '15px',
  },
  label: {
    display: 'block',
    fontSize: '14px',
    fontWeight: '500',
    marginBottom: '5px',
  },
  input: {
    width: '100%',
    padding: '10px',
    fontSize: '14px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    boxSizing: 'border-box',
  },
  textarea: {
    width: '100%',
    padding: '10px',
    fontSize: '14px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    boxSizing: 'border-box',
    fontFamily: 'inherit',
  },
  select: {
    width: '100%',
    padding: '10px',
    fontSize: '14px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    boxSizing: 'border-box',
  },
  privacyNotice: {
    fontSize: '13px',
    color: '#666',
    fontStyle: 'italic',
    marginBottom: '15px',
    padding: '10px',
    backgroundColor: '#f8f9fa',
    borderLeft: '3px solid #007bff',
  },
  actions: {
    display: 'flex',
    gap: '10px',
    justifyContent: 'flex-end',
    marginTop: '30px',
  },
  cancelButton: {
    padding: '12px 24px',
    fontSize: '14px',
    backgroundColor: '#6c757d',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  submitButton: {
    padding: '12px 24px',
    fontSize: '14px',
    backgroundColor: '#007bff',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  error: {
    padding: '12px',
    backgroundColor: '#fee',
    color: '#c33',
    borderRadius: '4px',
    marginBottom: '20px',
  },
};

export default CreateNeed;
