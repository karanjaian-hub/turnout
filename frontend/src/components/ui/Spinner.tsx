import React from 'react';
import { clsx } from 'clsx';

interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const sizeMap = {
  sm: 'w-4 h-4 border-2',
  md: 'w-8 h-8 border-4',
  lg: 'w-12 h-12 border-4',
};

const Spinner: React.FC<SpinnerProps> = ({ size = 'md', className }) => (
  <div
    className={clsx(
      'rounded-full border-navy border-t-transparent animate-spin',
      sizeMap[size],
      className
    )}
    role="status"
    aria-label="Loading"
  />
);

export default Spinner;
