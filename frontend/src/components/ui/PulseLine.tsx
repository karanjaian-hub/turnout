import React from 'react';

interface PulseLineProps {
  color?: string;
  duration?: number;
  className?: string;
}

const id = 'pl' + Math.random().toString(36).slice(2, 6);

const PulseLine: React.FC<PulseLineProps> = ({
  color   = '#2563EB',
  duration = 1800,
  className = '',
}) => (
  <svg width="100%" height="8" viewBox="0 0 300 8" preserveAspectRatio="none" className={className} aria-hidden="true">
    <defs>
      <style>{`
        @keyframes ${id} {
          0%   { stroke-dashoffset: 300; opacity: 0; }
          5%   { opacity: 1; }
          85%  { opacity: 1; }
          100% { stroke-dashoffset: 0; opacity: 0; }
        }
        .${id} { stroke-dasharray: 300; stroke-dashoffset: 300; animation: ${id} ${duration}ms ease-in-out infinite; }
        @media (prefers-reduced-motion: reduce) { .${id} { animation: none; opacity: 0.2; } }
      `}</style>
      <filter id={`${id}g`}>
        <feGaussianBlur stdDeviation="1.5" result="b"/>
        <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
      </filter>
    </defs>
    <line x1="0" y1="4" x2="300" y2="4" stroke={color} strokeWidth="1" strokeOpacity="0.12"/>
    <line x1="0" y1="4" x2="300" y2="4" stroke={color} strokeWidth="2" filter={`url(#${id}g)`} className={id}/>
  </svg>
);

export default PulseLine;
