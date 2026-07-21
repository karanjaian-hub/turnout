import React from 'react';
import PageCard from '../components/PageCard';

const InvalidPage: React.FC = () => (
  <PageCard maxWidth={420}>
    <div style={{ textAlign: 'center', padding: '16px 0' }}>
      <div style={{
        width: 72, height: 72, borderRadius: '50%',
        background: '#FEF3C7', margin: '0 auto 20px',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 36,
      }}>
        🔗
      </div>
      <h2 style={{ fontFamily: "'Space Grotesk', sans-serif", fontSize: 24, fontWeight: 700, color: '#0F172A', marginBottom: 10 }}>
        Invalid link
      </h2>
      <p style={{ fontSize: 14, color: '#64748B', fontFamily: 'Inter', lineHeight: 1.6, maxWidth: 300, margin: '0 auto 24px' }}>
        This RSVP link is missing, expired, or has already been used. Please check your email for the correct link.
      </p>
      <div style={{ padding: '14px 20px', background: '#FFFBEB', borderRadius: 12, border: '1px solid #FDE68A' }}>
        <p style={{ fontSize: 13, color: '#92400E', fontFamily: 'Inter', fontWeight: 500 }}>
          💡 Links are single-use. If you already responded, your answer has been recorded.
        </p>
      </div>
    </div>
  </PageCard>
);

export default InvalidPage;
