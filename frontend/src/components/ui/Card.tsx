import React from 'react';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  hover?: boolean;
  padding?: boolean;
  onClick?: () => void;
}

const Card: React.FC<CardProps> = ({ children, className = '', hover = false, padding = true, onClick }) => {
  const [hovered, setHovered] = React.useState(false);
  const isClickable = hover || !!onClick;

  return (
    <div
      onClick={onClick}
      onMouseEnter={() => isClickable && setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      className={className}
      style={{
        background: 'var(--bg-card)',
        borderRadius: 12,
        padding: padding ? 24 : 0,
        boxShadow: hovered && isClickable
          ? 'var(--shadow-card)'
          : 'var(--shadow-card)',
        transform: hovered && isClickable ? 'translateY(-2px)' : 'none',
        transition: 'all 200ms ease',
        cursor: onClick ? 'pointer' : 'default',
      }}
    >
      {children}
    </div>
  );
};

export default Card;
