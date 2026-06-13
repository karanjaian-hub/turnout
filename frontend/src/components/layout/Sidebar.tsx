import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { clsx } from 'clsx';
import {
  LayoutDashboard, Users, Calendar,
  CreditCard, Building2, Settings, LogOut,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import Badge from '../ui/Badge';

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactNode;
  superAdminOnly?: boolean;
}

const navItems: NavItem[] = [
  { label: 'Dashboard',  path: '/dashboard',  icon: <LayoutDashboard size={18} /> },
  { label: 'Organizers', path: '/organizers',  icon: <Users size={18} /> },
  { label: 'Events',     path: '/events',      icon: <Calendar size={18} /> },
  { label: 'Payments',   path: '/payments',    icon: <CreditCard size={18} /> },
  { label: 'Enterprise', path: '/payments/enterprise', icon: <Building2 size={18} /> },
  { label: 'Settings',   path: '/settings',   icon: <Settings size={18} />, superAdminOnly: true },
];

const Sidebar: React.FC = () => {
  const { user, logout, isSuperAdmin } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const visibleItems = navItems.filter(
    item => !item.superAdminOnly || isSuperAdmin()
  );

  return (
    <aside className="fixed left-0 top-0 h-full w-64 bg-navy flex flex-col z-40">

      {/* Logo */}
      <div className="px-6 py-6 border-b border-white/10">
        <div className="flex items-center gap-2">
          <span className="text-2xl font-bold text-white tracking-tight">TURNOUT</span>
          <span className="w-2 h-2 rounded-full bg-primary-500 mt-1" />
        </div>
        <p className="text-xs text-slate-400 mt-1 font-medium">Admin Panel</p>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        {visibleItems.map(item => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) => clsx(
              'flex items-center gap-3 px-3 py-2.5 rounded-input text-sm font-medium',
              'transition-all duration-150',
              isActive
                ? 'bg-primary-500/20 text-white'
                : 'text-slate-300 hover:bg-white/10 hover:text-white'
            )}
          >
            {item.icon}
            {item.label}
          </NavLink>
        ))}
      </nav>

      {/* User card + logout */}
      <div className="px-4 py-4 border-t border-white/10">
        <div className="flex items-center gap-3 mb-3 px-1">
          {/* Avatar — initials from username */}
          <div className="w-8 h-8 rounded-full bg-primary-500 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
            {user?.username?.[0]?.toUpperCase() ?? 'A'}
          </div>
          <div className="min-w-0">
            <p className="text-sm font-medium text-white truncate">{user?.username}</p>
            <Badge variant="info" className="text-xs mt-0.5">
              {user?.role?.replace('_', ' ')}
            </Badge>
          </div>
        </div>
        <button
          onClick={handleLogout}
          className="w-full flex items-center gap-2 px-3 py-2 rounded-input text-sm text-slate-300 hover:bg-white/10 hover:text-white transition-all duration-150"
        >
          <LogOut size={16} />
          Sign out
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
