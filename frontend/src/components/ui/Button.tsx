import React from 'react';
import { clsx } from 'clsx';
import Spinner from './Spinner';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  icon?: React.ReactNode;
}

const variantMap = {
  primary:   'bg-gradient-to-r from-navy to-primary-500 text-white hover:from-primary-600 hover:to-primary-500 shadow-md',
  secondary: 'bg-white text-navy border border-slate-200 hover:bg-slate-50',
  danger:    'bg-danger text-white hover:bg-red-700 shadow-md',
  ghost:     'bg-transparent text-navy hover:bg-slate-100',
};

const sizeMap = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-sm',
  lg: 'px-6 py-3 text-base',
};

const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  loading = false,
  icon,
  children,
  disabled,
  className,
  ...rest
}) => (
  <button
    disabled={disabled || loading}
    className={clsx(
      'inline-flex items-center justify-center gap-2 font-medium rounded-input',
      'transition-all duration-150',
      // Lift effect on hover — gives the button a tactile feel
      'hover:-translate-y-0.5 active:translate-y-0',
      'disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none',
      variantMap[variant],
      sizeMap[size],
      className
    )}
    {...rest}
  >
    {loading ? <Spinner size="sm" className="border-white border-t-transparent" /> : icon}
    {children}
  </button>
);

export default Button;
