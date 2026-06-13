import React from 'react';

interface EmptyStateProps {
  icon: React.ReactNode;
  heading: string;
  subtext: string;
  action?: React.ReactNode;
}

const EmptyState: React.FC<EmptyStateProps> = ({ icon, heading, subtext, action }) => (
  <div className="flex flex-col items-center justify-center py-16 text-center">
    <div className="p-4 rounded-full bg-slate-100 text-slate-400 mb-4">
      {icon}
    </div>
    <h3 className="text-base font-semibold text-navy mb-1">{heading}</h3>
    <p className="text-sm text-slate-500 max-w-xs">{subtext}</p>
    {action && <div className="mt-4">{action}</div>}
  </div>
);

export default EmptyState;
