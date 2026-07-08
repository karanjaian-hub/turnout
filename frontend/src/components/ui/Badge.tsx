import React from 'react';

interface BadgeProps {
  variant?: 'success' | 'warning' | 'danger' | 'info' | 'neutral';
  children: React.ReactNode;
  className?: string;
}

const CONFIG = {
  success: { bg: '#DCFCE7', color: '#15803D', dot: '#16A34A' },
  warning: { bg: '#FEF3C7', color: '#B45309', dot: '#D97706' },
  danger:  { bg: '#FEE2E2', color: '#B91C1C', dot: '#DC2626' },
  info:    { bg: '#DBEAFE', color: '#1D4ED8', dot: '#2563EB' },
  neutral: { bg: 'var(--bg-app)', color: 'var(--text-secondary)', dot: null },
};

const STATUS_VARIANTS = ['success', 'warning', 'danger'];

const Badge: React.FC<BadgeProps> = ({ variant = 'neutral', children, className = '' }) => {
  const { bg, color, dot } = CONFIG[variant];
  const showDot = dot && STATUS_VARIANTS.includes(variant);

  return (
    <span
      className={className}
      style={{
        display: 'inline-flex', alignItems: 'center', gap: 5,
        padding: '3px 9px', borderRadius: 999,
        background: bg, color,
        fontSize: 11, fontWeight: 500, fontFamily: 'Inter',
        letterSpacing: '0.01em', whiteSpace: 'nowrap',
      }}
    >
      {showDot && (
        <span style={{ width: 5, height: 5, borderRadius: '50%', background: dot, flexShrink: 0 }}/>
      )}
      {children}
    </span>
  );
};

export default Badge;
