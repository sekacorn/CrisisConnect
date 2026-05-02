import React from 'react';
import { Link } from 'react-router-dom';

const LandingPage: React.FC = () => {
  return (
    <div style={styles.page}>
      <header style={styles.header}>
        <Link to="/" style={styles.brand} aria-label="CrisisConnect home">
          CrisisConnect
        </Link>
        <nav style={styles.nav} aria-label="Public navigation">
          <a href="#features" style={styles.navLink}>Features</a>
          <a href="#trust" style={styles.navLink}>Trust</a>
          <Link to="/login" style={styles.navButton}>Sign in</Link>
        </nav>
      </header>

      <main id="main-content" tabIndex={-1} style={styles.main}>
        <section style={styles.hero} aria-labelledby="hero-title">
          <div style={styles.heroContent}>
            <p style={styles.kicker}>Humanitarian coordination software</p>
            <h1 id="hero-title" style={styles.title}>
              Coordinate urgent needs without losing control of sensitive data.
            </h1>
            <p style={styles.lede}>
              CrisisConnect gives mission-driven teams a practical way to intake assistance
              requests, match them to verified organizations, and keep beneficiary details
              restricted to the people who need them.
            </p>
            <div style={styles.actions}>
              <Link to="/login" style={styles.primaryAction}>Sign in</Link>
              <a href="#features" style={styles.secondaryAction}>See what it does</a>
            </div>
          </div>

          <aside style={styles.summaryPanel} aria-label="Project summary">
            <div style={styles.metricRow}>
              <span style={styles.metricValue}>RBAC</span>
              <span style={styles.metricLabel}>Role-based access</span>
            </div>
            <div style={styles.metricRow}>
              <span style={styles.metricValue}>Redacted</span>
              <span style={styles.metricLabel}>Need listings by default</span>
            </div>
            <div style={styles.metricRow}>
              <span style={styles.metricValue}>Audit</span>
              <span style={styles.metricLabel}>Sensitive actions logged</span>
            </div>
          </aside>
        </section>

        <section id="features" style={styles.section} aria-labelledby="features-title">
          <div style={styles.sectionIntro}>
            <p style={styles.kicker}>Current application</p>
            <h2 id="features-title" style={styles.sectionTitle}>Built for practical coordination</h2>
          </div>
          <div style={styles.grid}>
            <article style={styles.feature}>
              <h3 style={styles.featureTitle}>Field intake</h3>
              <p style={styles.featureText}>
                Field workers can create assistance needs with category, urgency, location,
                contact details, and sensitive notes.
              </p>
            </article>
            <article style={styles.feature}>
              <h3 style={styles.featureTitle}>Verified organizations</h3>
              <p style={styles.featureText}>
                Organization status and service areas help control who can claim needs and
                view full details.
              </p>
            </article>
            <article style={styles.feature}>
              <h3 style={styles.featureTitle}>Admin oversight</h3>
              <p style={styles.featureText}>
                Admin screens cover users, organizations, audit logs, suspicious activities,
                and operational statistics.
              </p>
            </article>
          </div>
        </section>

        <section id="trust" style={styles.trustBand} aria-labelledby="trust-title">
          <div>
            <p style={styles.kicker}>Trust posture</p>
            <h2 id="trust-title" style={styles.sectionTitle}>Designed for review before real deployment</h2>
            <p style={styles.trustText}>
              The project includes privacy filtering, encrypted sensitive fields, JWT authentication,
              audit logs, accessibility work, tests, Docker files, and deployment documentation.
              It is a working starter system, not a certified production platform.
            </p>
          </div>
          <Link to="/login" style={styles.trustAction}>Go to login</Link>
        </section>
      </main>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  page: {
    minHeight: '100vh',
    backgroundColor: '#f7f8fb',
    color: '#1f2933',
  },
  header: {
    minHeight: '72px',
    padding: '0 32px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#ffffff',
    borderBottom: '1px solid #dde3ea',
    position: 'sticky',
    top: 0,
    zIndex: 10,
  },
  brand: {
    color: '#102a43',
    fontSize: '22px',
    fontWeight: 800,
    textDecoration: 'none',
  },
  nav: {
    display: 'flex',
    alignItems: 'center',
    gap: '18px',
    flexWrap: 'wrap',
    justifyContent: 'flex-end',
  },
  navLink: {
    color: '#486581',
    fontSize: '15px',
    fontWeight: 600,
    textDecoration: 'none',
  },
  navButton: {
    minHeight: '40px',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '0 16px',
    borderRadius: '6px',
    backgroundColor: '#0b7285',
    color: '#ffffff',
    fontSize: '15px',
    fontWeight: 700,
    textDecoration: 'none',
  },
  main: {
    outline: 'none',
  },
  hero: {
    maxWidth: '1180px',
    margin: '0 auto',
    padding: '76px 28px 64px',
    display: 'grid',
    gridTemplateColumns: 'minmax(0, 1.5fr) minmax(280px, 0.7fr)',
    gap: '36px',
    alignItems: 'center',
  },
  heroContent: {
    maxWidth: '760px',
  },
  kicker: {
    color: '#0b7285',
    fontSize: '13px',
    fontWeight: 800,
    textTransform: 'uppercase',
    letterSpacing: '0.08em',
    marginBottom: '14px',
  },
  title: {
    color: '#102a43',
    fontSize: 'clamp(40px, 6vw, 64px)',
    lineHeight: 1.02,
    margin: '0 0 24px',
  },
  lede: {
    maxWidth: '680px',
    color: '#334e68',
    fontSize: '20px',
    lineHeight: 1.55,
    margin: 0,
  },
  actions: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '14px',
    marginTop: '32px',
  },
  primaryAction: {
    minHeight: '48px',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '0 22px',
    borderRadius: '6px',
    backgroundColor: '#0b7285',
    color: '#ffffff',
    fontSize: '16px',
    fontWeight: 800,
    textDecoration: 'none',
  },
  secondaryAction: {
    minHeight: '48px',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '0 22px',
    borderRadius: '6px',
    border: '1px solid #9fb3c8',
    color: '#102a43',
    backgroundColor: '#ffffff',
    fontSize: '16px',
    fontWeight: 800,
    textDecoration: 'none',
  },
  summaryPanel: {
    backgroundColor: '#ffffff',
    border: '1px solid #d9e2ec',
    borderRadius: '8px',
    padding: '10px 24px',
    boxShadow: '0 18px 44px rgba(16, 42, 67, 0.12)',
  },
  metricRow: {
    display: 'grid',
    gridTemplateColumns: '110px 1fr',
    gap: '16px',
    alignItems: 'center',
    minHeight: '72px',
    borderBottom: '1px solid #edf2f7',
  },
  metricValue: {
    color: '#102a43',
    fontSize: '20px',
    fontWeight: 800,
  },
  metricLabel: {
    color: '#486581',
    fontSize: '15px',
    lineHeight: 1.45,
  },
  section: {
    maxWidth: '1180px',
    margin: '0 auto',
    padding: '24px 28px 56px',
  },
  sectionIntro: {
    marginBottom: '24px',
  },
  sectionTitle: {
    color: '#102a43',
    fontSize: '32px',
    lineHeight: 1.2,
    margin: 0,
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
    gap: '18px',
  },
  feature: {
    backgroundColor: '#ffffff',
    border: '1px solid #d9e2ec',
    borderRadius: '8px',
    padding: '24px',
    minHeight: '190px',
  },
  featureTitle: {
    color: '#102a43',
    fontSize: '20px',
    margin: '0 0 12px',
  },
  featureText: {
    color: '#486581',
    fontSize: '16px',
    lineHeight: 1.55,
    margin: 0,
  },
  trustBand: {
    maxWidth: '1180px',
    margin: '0 auto 48px',
    padding: '32px 28px',
    display: 'grid',
    gridTemplateColumns: '1fr auto',
    gap: '24px',
    alignItems: 'center',
    backgroundColor: '#e6f6f8',
    border: '1px solid #b8e3e8',
    borderRadius: '8px',
  },
  trustText: {
    color: '#334e68',
    fontSize: '17px',
    lineHeight: 1.55,
    maxWidth: '780px',
    margin: '16px 0 0',
  },
  trustAction: {
    minHeight: '44px',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    whiteSpace: 'nowrap',
    padding: '0 18px',
    borderRadius: '6px',
    backgroundColor: '#102a43',
    color: '#ffffff',
    fontSize: '15px',
    fontWeight: 800,
    textDecoration: 'none',
  },
};

export default LandingPage;
