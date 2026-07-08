import React from 'react';
import PulseLine from './PulseLine';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  icon?: React.ReactNode;
}

const BASE: React.CSSProperties = {
  display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
  gap: 8, fontFamily: 'Inter', fontWeight: 500, borderRadius: 8,
  border: 'none', cursor: 'pointer', transition: 'all 150ms ease',
  letterSpacing: '-0.01em', whiteSpace: 'nowrap',
};

const SIZE: Record<string, React.CSSProperties> = {
  sm: { padding: '6px 12px',  fontSize: 13 },
  md: { padding: '8px 16px',  fontSize: 14 },
  lg: { padding: '12px 24px', fontSize: 15 },
};

const VARIANT_STYLE: Record<string, React.CSSProperties> = {
  primary:   { background: '#2563EB', color: '#fff' },
  secondary: { background: 'transparent', color: 'var(--text-primary)', border: '1px solid var(--border)' },
  danger:    { background: '#DC2626', color: '#fff' },
  ghost:     { background: 'transparent', color: 'var(--text-primary)' },
};

const HOVER_SHADOW: Record<string, string> = {
  primary:   '0 6px 20px rgba(37,99,235,0.35)',
  secondary: 'none',
  danger:    '0 6px 20px rgba(220,38,38,0.35)',
  ghost:     'none',
};

const Button: React.FC<ButtonProps> = ({
  variant = 'primary', size = 'md', loading = false,
  icon, children, disabled, style, ...rest
}) => {
  const [hovered, setHovered] = React.useState(false);
  const [pressed, setPressed] = React.useState(false);
  const isDisabled = disabled || loading;

  const hoverBg: Record<string, string> = {
    primary:   '#1D4ED8',
    secondary: 'var(--bg-app)',
    danger:    '#B91C1C',
    ghost:     'var(--bg-app)',
  };

  return (
    <button
      disabled={isDisabled}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => { setHovered(false); setPressed(false); }}
      onMouseDown={() => setPressed(true)}
      onMouseUp={() => setPressed(false)}
      style={{
        ...BASE,
        ...SIZE[size],
        ...VARIANT_STYLE[variant],
        ...(hovered && !isDisabled ? {
          background: hoverBg[variant],
          transform: 'translateY(-2px)',
          boxShadow: HOVER_SHADOW[variant],
        } : {}),
        ...(pressed ? { transform: 'scale(0.98)', boxShadow: 'none' } : {}),
        opacity: isDisabled ? 0.5 : 1,
        cursor: isDisabled ? 'not-allowed' : 'pointer',
        ...style,
      }}
      {...rest}
    >
      {loading
        ? <PulseLine color={variant === 'primary' || variant === 'danger' ? '#fff' : '#2563EB'} duration={900} />
        : icon
      }
      {!loading && children}
    </button>
  );
};

export default Button;
