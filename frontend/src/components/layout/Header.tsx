import React from 'react';
import { useLocation } from 'react-router-dom';
import { Bell } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

// Derive a human-readable page title from the current URL path
const pageTitleMap: Record<string, string> = {
  '/dashboard':           'Dashboard',
  '/organizers':          'Organizers',
  '/events':              'Events',
  '/payments':            'Payments',
  '/payments/enterprise': 'Enterprise Requests',
  '/settings':            'Settings',
};

const Header: React.FC = () => {
  const { pathname } = useLocation();
  const { user } = useAuth();

  const title = pageTitleMap[pathname] ?? 'Turnout Admin';

  return (
    <header className="fixed top-0 left-64 right-0 h-16 bg-white border-b border-slate-200 flex items-center justify-between px-6 z-30">
      <h1 className="text-lg font-semibold text-navy">{title}</h1>

      <div className="flex items-center gap-3">
        {/* Notification bell — wired to WebSocket alerts in 11.6 */}
        <button className="relative p-2 rounded-input text-slate-400 hover:text-navy hover:bg-slate-100 transition-colors">
          <Bell size={20} />
        </button>

        {/* Avatar */}
        <div className="w-8 h-8 rounded-full bg-primary-500 flex items-center justify-center text-white text-sm font-bold">
          {user?.username?.[0]?.toUpperCase() ?? 'A'}
        </div>
      </div>
    </header>
  );
};

export default Header;
