import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useAnnouncement } from '../hooks/useAnnouncement';
import LoadingSpinner from '../components/LoadingSpinner';

const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [rateLimited, setRateLimited] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const { announce } = useAnnouncement();
  const emailInputRef = useRef<HTMLInputElement>(null);

  // Focus email input on mount for better UX
  useEffect(() => {
    emailInputRef.current?.focus();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setRateLimited(false);
    setLoading(true);

    // Announce to screen readers
    announce('Logging in, please wait', 'polite');

    try {
      await login({ email, password });
      announce('Login successful. Redirecting to dashboard', 'polite');
      navigate('/dashboard');
    } catch (err: any) {
      // Handle rate limiting (429 Too Many Requests)
      if (err.response?.status === 429) {
        setRateLimited(true);
        const errorMsg = err.response?.data?.message ||
          'Too many failed login attempts. Your account is temporarily locked for 15 minutes for security. Please try again later.';
        setError(errorMsg);
        announce(`Error: ${errorMsg}`, 'assertive');
      } else {
        // Generic error for 401 or other errors (prevents user enumeration)
        const errorMsg = err.response?.data?.message || 'Login failed. Please check your credentials.';
        setError(errorMsg);
        announce(`Error: ${errorMsg}`, 'assertive');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      {/* Main content with semantic HTML */}
      <main id="main-content" tabIndex={-1} style={styles.card}>
        <header>
          <h1 style={styles.title}>CrisisConnect</h1>
          <p style={styles.subtitle}>Humanitarian Aid Coordination Platform</p>
        </header>

        <form
          onSubmit={handleSubmit}
          style={styles.form}
          aria-label="Login form"
          noValidate
        >
          {/* Error message with proper ARIA */}
          {error && (
            <div
              role="alert"
              aria-live="assertive"
              aria-atomic="true"
              style={rateLimited ? styles.rateLimitError : styles.error}
              id="login-error"
            >
              {error}
              {rateLimited && (
                <div style={{ marginTop: '8px', fontSize: '12px' }}>
                  Rate limit: 5 failed attempts per 15 minutes. This is a security feature to protect your account.
                </div>
              )}
            </div>
          )}

          <div style={styles.field}>
            <label htmlFor="email" style={styles.label}>
              Email Address
            </label>
            <input
              id="email"
              name="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
              style={styles.input}
              placeholder="Enter your email"
              aria-required="true"
              aria-invalid={!!error}
              aria-describedby={error ? 'login-error' : undefined}
              ref={emailInputRef}
            />
          </div>

          <div style={styles.field}>
            <label htmlFor="password" style={styles.label}>
              Password
            </label>
            <input
              id="password"
              name="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
              style={styles.input}
              placeholder="Enter your password"
              aria-required="true"
              aria-invalid={!!error}
              aria-describedby={error ? 'login-error' : undefined}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            style={loading ? { ...styles.button, ...styles.buttonDisabled } : styles.button}
            aria-label={loading ? 'Logging in, please wait' : 'Login to CrisisConnect'}
          >
            {loading ? <LoadingSpinner message="Logging in" size="small" /> : 'Login'}
          </button>
        </form>

        <footer style={styles.footer}>
          <p style={styles.footerText}>Secure by Design | GDPR & HIPAA Compliant</p>
        </footer>
      </main>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f5f5f5',
  },
  card: {
    backgroundColor: 'white',
    padding: '40px',
    borderRadius: '8px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
    width: '100%',
    maxWidth: '400px',
    outline: 'none',
  },
  title: {
    fontSize: '32px',
    fontWeight: 'bold',
    color: '#333',
    marginBottom: '8px',
    textAlign: 'center',
  },
  subtitle: {
    fontSize: '14px',
    color: '#666',
    marginBottom: '32px',
    textAlign: 'center',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '20px',
  },
  field: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
  },
  label: {
    fontSize: '14px',
    fontWeight: '500',
    color: '#333',
  },
  input: {
    padding: '10px 12px',
    fontSize: '14px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    transition: 'border-color 0.2s, box-shadow 0.2s',
  },
  button: {
    padding: '12px',
    fontSize: '16px',
    fontWeight: '500',
    color: 'white',
    backgroundColor: '#0066cc',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    marginTop: '8px',
    transition: 'background-color 0.2s, opacity 0.2s',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonDisabled: {
    opacity: 0.6,
    cursor: 'not-allowed',
  },
  error: {
    padding: '12px',
    backgroundColor: '#fee',
    color: '#c33',
    borderRadius: '4px',
    fontSize: '14px',
    border: '2px solid #c33',
  },
  rateLimitError: {
    padding: '12px',
    backgroundColor: '#fff3cd',
    color: '#856404',
    borderRadius: '4px',
    fontSize: '14px',
    border: '2px solid #ffeaa7',
  },
  footer: {
    marginTop: '32px',
    paddingTop: '20px',
    borderTop: '1px solid #eee',
  },
  footerText: {
    fontSize: '12px',
    color: '#999',
    textAlign: 'center',
  },
};

export default Login;
