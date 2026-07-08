import React, { useEffect, useState } from 'react';

interface EmptyStateProps {
  icon: React.ReactNode;
  heading: string;
  subtext: string;
  action?: React.ReactNode;
}

const EmptyState: React.FC<EmptyStateProps> = ({ icon, heading, subtext, action }) => {
  const [visible, setVisible] = useState(false);
  useEffect(() => { const t = setTimeout(() => setVisible(true), 50); return () => clearTimeout(t); }, []);

  return (
    <div style={{
      display: 'flex', flexDirection: 'column', alignItems: 'center',
      justifyContent: 'center', padding: '56px 24px', textAlign: 'center',
      opacity: visible ? 1 : 0, transform: visible ? 'translateY(0)' : 'translateY(8px)',
      transition: 'opacity 300ms ease, transform 300ms ease',
    }}>
      {/* Geometric SVG illustration */}
      <div style={{ position: 'relative', width: 72, height: 72, marginBottom: 20 }}>
        <svg width="72" height="72" viewBox="0 0 72 72" fill="none">
          <rect x="8" y="8" width="56" height="56" rx="16" fill="#F1F5F9"/>
          <rect x="20" y="24" width="32" height="4" rx="2" fill="#CBD5E1"/>
          <rect x="20" y="34" width="22" height="4" rx="2" fill="#E2E8F0"/>
          <rect x="20" y="44" width="28" height="4" rx="2" fill="#E2E8F0"/>
          <circle cx="56" cy="18" r="10" fill="#EFF6FF" stroke="#BFDBFE" strokeWidth="2"/>
          <path d="M52 18h8M56 14v8" stroke="#2563EB" strokeWidth="2" strokeLinecap="round"/>
        </svg>
      </div>
      <h3 style={{ fontFamily: "'Space Grotesk',sans-serif", fontSize: 16, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 6 }}>
        {heading}
      </h3>
      <p style={{ fontFamily: 'Inter', fontSize: 13, color: 'var(--text-secondary)', maxWidth: 280, lineHeight: 1.6 }}>
        {subtext}
      </p>
      {action && <div style={{ marginTop: 20 }}>{action}</div>}
    </div>
  );
};

export default EmptyState;
