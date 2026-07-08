import React, { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, Users, Calendar,
  CreditCard, Building2, Settings, LogOut,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import PulseLine from '../ui/PulseLine';

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactNode;
  superAdminOnly?: boolean;
}

const navItems: NavItem[] = [
  { label: 'Dashboard',  path: '/dashboard',           icon: <LayoutDashboard size={20}/> },
  { label: 'Organizers', path: '/organizers',           icon: <Users size={20}/> },
  { label: 'Events',     path: '/events',               icon: <Calendar size={20}/> },
  { label: 'Payments',   path: '/payments',             icon: <CreditCard size={20}/> },
  { label: 'Enterprise', path: '/payments/enterprise',  icon: <Building2 size={20}/> },
  { label: 'Settings',   path: '/settings',             icon: <Settings size={20}/>, superAdminOnly: true },
];

const Sidebar: React.FC<{ onClose?: () => void }> = ({ onClose }) => {
  const { user, logout, isSuperAdmin } = useAuth();
  const navigate = useNavigate();
  const [hoveredPath, setHoveredPath] = useState<string | null>(null);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const visibleItems = navItems.filter(item => !item.superAdminOnly || isSuperAdmin());

  return (
    <aside style={{
      position: 'fixed', left: 0, top: 0, height: '100%', width: 256,
      background: '#0B1422',
      display: 'flex', flexDirection: 'column',
      zIndex: 40,
      borderRight: '1px solid rgba(255,255,255,0.06)',
    }}>

      {/* Logo */}
      <div style={{ padding: '24px 20px 20px', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <span style={{ fontFamily: "'Space Grotesk',sans-serif", fontSize: 22, fontWeight: 700, color: '#E2E8F0', letterSpacing: '-0.02em' }}>
            TURNOUT
          </span>
          <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#2563EB', marginTop: 2, flexShrink: 0 }}/>
        </div>
        {/* Pulse-line next to logo — ambient system health indicator */}
        <PulseLine color="#2563EB" duration={2400} />
        <p style={{ fontSize: 11, color: '#475569', fontFamily: 'Inter', marginTop: 6, letterSpacing: '0.05em', textTransform: 'uppercase', fontWeight: 500 }}>
          Admin Panel
        </p>
      </div>

      {/* Navigation */}
      <nav style={{ flex: 1, padding: '12px 10px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 2 }}>
        {visibleItems.map(item => (
          <NavLink
            key={item.path}
            to={item.path}
            onMouseEnter={() => setHoveredPath(item.path)}
            onMouseLeave={() => setHoveredPath(null)}
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              gap: 12,
              padding: '10px 12px',
              borderRadius: 10,
              textDecoration: 'none',
              fontFamily: 'Inter',
              fontSize: 14,
              fontWeight: isActive ? 500 : 400,
              color: isActive ? '#fff' : hoveredPath === item.path ? '#E2E8F0' : '#94A3B8',
              background: isActive
                ? 'rgba(37,99,235,0.12)'
                : hoveredPath === item.path
                  ? 'rgba(255,255,255,0.04)'
                  : 'transparent',
              position: 'relative',
              transition: 'all 150ms ease',
              letterSpacing: '-0.01em',
            })}
          >
            {({ isActive }) => (
              <>
                {/* Active left bar */}
                {isActive && (
                  <span style={{
                    position: 'absolute', left: 0, top: '50%', transform: 'translateY(-50%)',
                    width: 3, height: 20, borderRadius: 99,
                    background: '#2563EB',
                  }}/>
                )}
                <span style={{ color: isActive ? '#2563EB' : hoveredPath === item.path ? '#E2E8F0' : '#64748B', transition: 'color 150ms', flexShrink: 0 }}>
                  {item.icon}
                </span>
                {item.label}
              </>
            )}
          </NavLink>
        ))}
      </nav>

      {/* User card + logout */}
      <div style={{ padding: '12px 10px 16px', borderTop: '1px solid rgba(255,255,255,0.06)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', marginBottom: 4 }}>
          <div style={{ width: 32, height: 32, borderRadius: '50%', background: '#2563EB', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 13, fontWeight: 700, flexShrink: 0, fontFamily: 'Inter' }}>
            {user?.username?.[0]?.toUpperCase() ?? 'A'}
          </div>
          <div style={{ minWidth: 0 }}>
            <p style={{ fontSize: 13, fontWeight: 500, color: '#E2E8F0', fontFamily: 'Inter', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {user?.username}
            </p>
            <p style={{ fontSize: 11, color: '#475569', fontFamily: 'Inter', letterSpacing: '0.04em', textTransform: 'uppercase', marginTop: 1 }}>
              {user?.role?.replace('_', ' ')}
            </p>
          </div>
        </div>
        <button
          onClick={handleLogout}
          style={{ width: '100%', display: 'flex', alignItems: 'center', gap: 10, padding: '9px 12px', borderRadius: 10, background: 'none', border: 'none', color: '#64748B', cursor: 'pointer', fontSize: 14, fontFamily: 'Inter', transition: 'all 150ms ease' }}
          onMouseEnter={e => { e.currentTarget.style.background = 'rgba(255,255,255,0.04)'; e.currentTarget.style.color = '#E2E8F0'; }}
          onMouseLeave={e => { e.currentTarget.style.background = 'none'; e.currentTarget.style.color = '#64748B'; }}
        >
          <LogOut size={16}/> Sign out
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
