import React from 'react';
import PageCard from '../components/PageCard';

const ConfirmedPage: React.FC = () => (
  <PageCard maxWidth={420}>
    <div style={{ textAlign: 'center', padding: '16px 0' }}>
      {/* Success illustration */}
      <div style={{
        width: 72, height: 72, borderRadius: '50%',
        background: '#DCFCE7', margin: '0 auto 20px',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 36,
      }}>
        ✓
      </div>
      <h2 style={{ fontFamily: "'Space Grotesk', sans-serif", fontSize: 24, fontWeight: 700, color: '#0F172A', marginBottom: 10 }}>
        You're confirmed!
      </h2>
      <p style={{ fontSize: 14, color: '#64748B', fontFamily: 'Inter', lineHeight: 1.6, maxWidth: 300, margin: '0 auto 24px' }}>
        Great news — your spot is secured. We'll send you a reminder closer to the event date.
      </p>
      <div style={{ padding: '14px 20px', background: '#F0FDF4', borderRadius: 12, border: '1px solid #BBF7D0' }}>
        <p style={{ fontSize: 13, color: '#166534', fontFamily: 'Inter', fontWeight: 500 }}>
          📧 Check your email for event details and updates.
        </p>
      </div>
    </div>
  </PageCard>
);

export default ConfirmedPage;
