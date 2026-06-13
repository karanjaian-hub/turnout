import React from 'react';
import { clsx } from 'clsx';

interface BadgeProps {
  variant?: 'success' | 'warning' | 'danger' | 'info' | 'neutral';
  children: React.ReactNode;
  className?: string;
}

const variantMap = {
  success: 'bg-green-100 text-green-700',
  warning: 'bg-amber-100 text-amber-700',
  danger:  'bg-red-100  text-red-700',
  info:    'bg-blue-100 text-blue-700',
  neutral: 'bg-slate-100 text-slate-600',
};

const Badge: React.FC<BadgeProps> = ({ variant = 'neutral', children, className }) => (
  <span
    className={clsx(
      'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
      variantMap[variant],
      className
    )}
  >
    {children}
  </span>
);

export default Badge;
