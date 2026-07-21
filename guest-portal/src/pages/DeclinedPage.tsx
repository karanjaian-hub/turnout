import React from 'react';
import PageCard from '../components/PageCard';

const DeclinedPage: React.FC = () => (
  <PageCard maxWidth={420}>
    <div style={{ textAlign: 'center', padding: '16px 0' }}>
      <div style={{
        width: 72, height: 72, borderRadius: '50%',
        background: '#F1F5F9', margin: '0 auto 20px',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 36,
      }}>
        😔
      </div>
      <h2 style={{ fontFamily: "'Space Grotesk', sans-serif", fontSize: 24, fontWeight: 700, color: '#0F172A', marginBottom: 10 }}>
        Maybe next time
      </h2>
      <p style={{ fontSize: 14, color: '#64748B', fontFamily: 'Inter', lineHeight: 1.6, maxWidth: 300, margin: '0 auto 24px' }}>
        No worries — we've noted your response. We hope to see you at a future event!
      </p>
      <div style={{ padding: '14px 20px', background: '#F8FAFC', borderRadius: 12, border: '1px solid #E2E8F0' }}>
        <p style={{ fontSize: 13, color: '#475569', fontFamily: 'Inter', fontWeight: 500 }}>
          Changed your mind? Contact the organizer directly.
        </p>
      </div>
    </div>
  </PageCard>
);

export default DeclinedPage;
