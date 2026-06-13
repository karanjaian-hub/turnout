import React from 'react';
import { clsx } from 'clsx';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  // hover enables the lifted shadow on mouse-over
  hover?: boolean;
  padding?: boolean;
}

const Card: React.FC<CardProps> = ({
  children,
  className,
  hover = false,
  padding = true,
}) => (
  <div
    className={clsx(
      'bg-white rounded-card shadow-card',
      hover && 'transition-shadow duration-200 hover:shadow-hover cursor-pointer',
      padding && 'p-6',
      className
    )}
  >
    {children}
  </div>
);

export default Card;
