import React from 'react';
import PageCard from '../components/PageCard';

const ErrorPage: React.FC = () => (
  <PageCard maxWidth={420}>
    <div style={{ textAlign: 'center', padding: '16px 0' }}>
      <div style={{
        width: 72, height: 72, borderRadius: '50%',
        background: '#FEE2E2', margin: '0 auto 20px',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 36,
      }}>
        ⚠️
      </div>
      <h2 style={{ fontFamily: "'Space Grotesk', sans-serif", fontSize: 24, fontWeight: 700, color: '#0F172A', marginBottom: 10 }}>
        Something went wrong
      </h2>
      <p style={{ fontSize: 14, color: '#64748B', fontFamily: 'Inter', lineHeight: 1.6, maxWidth: 300, margin: '0 auto 24px' }}>
        We couldn't process your response right now. Please try again or contact the event organizer.
      </p>
      <button
        onClick={() => window.history.back()}
        style={{
          padding: '12px 24px', background: '#2563EB', border: 'none',
          borderRadius: 10, color: '#fff', fontSize: 14, fontWeight: 600,
          fontFamily: 'Inter', cursor: 'pointer',
        }}
      >
        Try again
      </button>
    </div>
  </PageCard>
);

export default ErrorPage;
