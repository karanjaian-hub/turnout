import React, { useEffect, useRef, useState } from 'react';
import { clsx } from 'clsx';

interface StatCardProps {
  label: string;
  value: number;
  icon: React.ReactNode;
  // accent controls the left border colour and icon background
  accent?: 'blue' | 'navy' | 'green' | 'amber';
  trend?: { value: number; label: string };
}

const accentMap = {
  blue:  { border: 'border-primary-500', iconBg: 'bg-blue-50 text-primary-500' },
  navy:  { border: 'border-navy',        iconBg: 'bg-slate-100 text-navy' },
  green: { border: 'border-success',     iconBg: 'bg-green-50 text-success' },
  amber: { border: 'border-warning',     iconBg: 'bg-amber-50 text-warning' },
};

// Animates a number counting up from 0 to `target` over `duration` ms.
// Why: a static number feels dead; the count-up draws the eye and signals "live data".
const useCountUp = (target: number, duration = 800): number => {
  const [count, setCount] = useState(0);
  const frameRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    const start = performance.now();

    const tick = (now: number) => {
      const elapsed = now - start;
      const progress = Math.min(elapsed / duration, 1);
      // Ease-out: fast start, slow finish
      const eased = 1 - Math.pow(1 - progress, 3);
      setCount(Math.round(eased * target));

      if (progress < 1) {
        frameRef.current = requestAnimationFrame(tick);
      }
    };

    frameRef.current = requestAnimationFrame(tick);
    return () => { if (frameRef.current) cancelAnimationFrame(frameRef.current); };
  }, [target, duration]);

  return count;
};

const StatCard: React.FC<StatCardProps> = ({
  label,
  value,
  icon,
  accent = 'blue',
  trend,
}) => {
  const displayValue = useCountUp(value);
  const { border, iconBg } = accentMap[accent];

  return (
    <div className={clsx(
      'bg-white rounded-card shadow-card p-6',
      'border-l-4', border
    )}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-slate-500 font-medium">{label}</p>
          <p className="mt-1 text-3xl font-bold text-navy">
            {displayValue.toLocaleString()}
          </p>
          {trend && (
            <p className={clsx(
              'mt-1 text-xs font-medium',
              trend.value >= 0 ? 'text-success' : 'text-danger'
            )}>
              {trend.value >= 0 ? '↑' : '↓'} {Math.abs(trend.value)}% {trend.label}
            </p>
          )}
        </div>
        <div className={clsx('p-3 rounded-input', iconBg)}>
          {icon}
        </div>
      </div>
    </div>
  );
};

export default StatCard;
