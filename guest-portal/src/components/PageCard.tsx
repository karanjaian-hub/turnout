import React from 'react';

interface PageCardProps {
  children: React.ReactNode;
  maxWidth?: number;
}

// The single card that wraps every page — centered on the dark gradient background
const PageCard: React.FC<PageCardProps> = ({ children, maxWidth = 480 }) => (
  <div style={{
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '24px 16px',
  }}>
    {/* Logo */}
    <div style={{ marginBottom: 24, textAlign: 'center' }}>
      <span style={{
        fontFamily: "'Space Grotesk', sans-serif",
        fontSize: 28,
        fontWeight: 700,
        color: '#E2E8F0',
        letterSpacing: '-0.02em',
      }}>
        TURNOUT
      </span>
      <span style={{
        display: 'inline-block',
        width: 6, height: 6,
        borderRadius: '50%',
        background: '#2563EB',
        marginLeft: 6,
        marginBottom: 4,
      }}/>
    </div>

    {/* Card */}
    <div style={{
      width: '100%',
      maxWidth,
      background: '#fff',
      borderRadius: 20,
      padding: '36px 32px',
      boxShadow: '0 4px 6px rgba(0,0,0,0.07), 0 24px 60px rgba(0,0,0,0.3)',
    }}>
      {children}
    </div>

    <p style={{
      marginTop: 20,
      fontSize: 12,
      color: 'rgba(226,232,240,0.4)',
      fontFamily: 'Inter',
      textAlign: 'center',
    }}>
      Powered by Turnout · No account needed
    </p>
  </div>
);

export default PageCard;
