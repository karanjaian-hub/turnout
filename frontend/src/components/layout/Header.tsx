import React, { useState, useRef, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { Bell, X, Menu } from 'lucide-react';
import ThemeSwitcher from '../ui/ThemeSwitcher';
import { useAuth } from '../../context/AuthContext';
import useWebSocket from '../../hooks/useWebSocket';
import Badge from '../ui/Badge';
import { formatDistanceToNow } from 'date-fns';

const pageTitleMap: Record<string, string> = {
  '/dashboard':           'Dashboard',
  '/organizers':          'Organizers',
  '/events':              'Events',
  '/payments':            'Payments',
  '/payments/enterprise': 'Enterprise Requests',
  '/settings':            'Settings',
};

const ALERT_BADGE: Record<string, 'success'|'warning'|'danger'|'info'> = {
  RSVP:         'success',
  PAYMENT:      'info',
  REGISTRATION: 'warning',
  SYSTEM:       'danger',
};

const Header: React.FC<{ onMenuClick?: () => void }> = ({ onMenuClick }) => {
  const { pathname }              = useLocation();
  const isOrganizerDetail = /^\/organizers\/[^/]+$/.test(pathname);
  const { user }                  = useAuth();
  const { connected, alerts }     = useWebSocket();
  const [open, setOpen]           = useState(false);
  const [seedAlerts, setSeedAlerts] = React.useState<any[]>([]);
  const [unread, setUnread]       = useState(0);
  const dropdownRef               = useRef<HTMLDivElement>(null);

  const title = isOrganizerDetail ? 'Organizer Detail' : (pageTitleMap[pathname] ?? 'Turnout Admin');

  // Pre-populate bell with recent RSVPs when no live alerts yet
  useEffect(() => {
    if (alerts.length > 0) return;
    import('../../services/api').then(({ default: api }) => {
      api.get('/api/admin/dashboard/recent-rsvps')
        .then(r => {
          const items = Array.isArray(r.data) ? r.data : [];
          setSeedAlerts(items.slice(0, 5).map((item: any) => ({
            type: 'RSVP',
            message: `${item.guestName ?? 'Guest'} — ${item.eventTitle ?? 'Event'}`,
            timestamp: item.timestamp ?? item.createdAt ?? new Date().toISOString(),
          })));
        })
        .catch(() => {});
    });
  }, [alerts.length]);

  const displayAlerts = alerts.length > 0 ? alerts : seedAlerts;

  // Increment unread count when new alerts arrive and dropdown is closed
  useEffect(() => {
    if (!open && alerts.length > 0) setUnread(prev => prev + 1);
  }, [alerts.length]); // eslint-disable-line

  // Close dropdown on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleOpen = () => {
    setOpen(v => !v);
    setUnread(0); // mark all as read when opened
  };

  return (
    <header style={{
      position: 'fixed', top: 0, left: 256, right: 0, height: 64,
      background: 'var(--bg-card)', borderBottom: '1px solid var(--border)',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 24px', zIndex: 30,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        {/* Hamburger — mobile only */}
        <button
          onClick={onMenuClick}
          className="lg-hidden-btn"
          style={{ display: 'flex', padding: 6, borderRadius: 8, background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}
        >
          <Menu size={20}/>
        </button>
        <h1 style={{ fontFamily: "'Space Grotesk',sans-serif", fontSize: 18, fontWeight: 600, color: 'var(--text-primary)' }}>
          {title}
        </h1>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>

        <ThemeSwitcher />

        {/* Notification bell */}
        <div ref={dropdownRef} style={{ position: 'relative' }}>
          <button
            onClick={handleOpen}
            style={{
              position: 'relative', padding: 8, borderRadius: 10,
              background: open ? 'var(--bg-app)' : 'none', border: 'none',
              color: open ? 'var(--text-primary)' : 'var(--text-secondary)', cursor: 'pointer',
              transition: 'all 150ms ease', display: 'flex',
            }}
            onMouseEnter={e => { if (!open) { e.currentTarget.style.background = 'var(--bg-app)'; e.currentTarget.style.color = 'var(--text-primary)'; }}}
            onMouseLeave={e => { if (!open) { e.currentTarget.style.background = 'none'; e.currentTarget.style.color = 'var(--text-secondary)'; }}}
          >
            <Bell size={20}/>
            {/* Unread badge */}
            {unread > 0 && (
              <span style={{
                position: 'absolute', top: 4, right: 4,
                width: 16, height: 16, borderRadius: '50%',
                background: '#DC2626', color: '#fff',
                fontSize: 10, fontWeight: 700, fontFamily: 'Inter',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                border: '2px solid #fff',
              }}>
                {unread > 9 ? '9+' : unread}
              </span>
            )}
            {/* Live pulse dot */}
            {connected && unread === 0 && (
              <span style={{
                position: 'absolute', top: 6, right: 6,
                width: 7, height: 7, borderRadius: '50%',
                background: '#16A34A', border: '1.5px solid #fff',
              }}/>
            )}
          </button>

          {/* Dropdown */}
          {open && (
            <div style={{
              position: 'absolute', top: 44, right: 0,
              width: 340, background: 'var(--bg-card)',
              borderRadius: 12, border: '1px solid var(--border)',
              boxShadow: '0 4px 6px rgba(0,0,0,0.07), 0 16px 48px rgba(0,0,0,0.1)',
              zIndex: 50, overflow: 'hidden',
              animation: 'dropdownFadeIn 150ms ease',
            }}>
              {/* Header */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 16px', borderBottom: '1px solid #F1F5F9' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ fontFamily: "'Space Grotesk',sans-serif", fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' }}>
                    Notifications
                  </span>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                    <span style={{ width: 6, height: 6, borderRadius: '50%', background: connected ? '#16A34A' : '#CBD5E1' }}/>
                    <span style={{ fontSize: 11, color: connected ? '#16A34A' : '#94A3B8', fontFamily: 'Inter', fontWeight: 500 }}>
                      {connected ? 'LIVE' : 'OFFLINE'}
                    </span>
                  </div>
                </div>
                <button onClick={() => setOpen(false)} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', display: 'flex', padding: 4, borderRadius: 6 }}
                  onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-app)')}
                  onMouseLeave={e => (e.currentTarget.style.background = 'none')}
                >
                  <X size={16}/>
                </button>
              </div>

              {/* Alert list */}
              <div style={{ maxHeight: 360, overflowY: 'auto' }}>
                {displayAlerts.length === 0 ? (
                  <div style={{ padding: '32px 16px', textAlign: 'center' }}>
                    <Bell size={28} style={{ color: '#CBD5E1', margin: '0 auto 8px' }}/>
                    <p style={{ fontSize: 13, color: 'var(--text-muted)', fontFamily: 'Inter' }}>
                      No notifications yet.
                    </p>
                    <p style={{ fontSize: 12, color: '#CBD5E1', fontFamily: 'Inter', marginTop: 4 }}>
                      Live alerts will appear here.
                    </p>
                  </div>
                ) : (
                  displayAlerts.slice(0, 20).map((alert, i) => (
                    <div key={i} style={{
                      display: 'flex', alignItems: 'flex-start', gap: 12,
                      padding: '12px 16px', borderBottom: '1px solid #F8FAFC',
                      transition: 'background 150ms',
                    }}
                      onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-app)')}
                      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                    >
                      <div style={{ marginTop: 2, flexShrink: 0 }}>
                        <Badge variant={ALERT_BADGE[alert.type] ?? 'neutral'}>
                          {alert.type}
                        </Badge>
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <p style={{ fontSize: 13, color: 'var(--text-primary)', fontFamily: 'Inter', lineHeight: 1.4 }}>
                          {alert.message}
                        </p>
                        <p style={{ fontSize: 11, color: 'var(--text-muted)', fontFamily: "'JetBrains Mono',monospace", marginTop: 4 }}>
                          {formatDistanceToNow(new Date(alert.timestamp), { addSuffix: true })}
                        </p>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        {/* Avatar */}
        <div style={{ width: 34, height: 34, borderRadius: '50%', background: '#2563EB', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 13, fontWeight: 700, fontFamily: 'Inter' }}>
          {user?.username?.[0]?.toUpperCase() ?? 'A'}
        </div>
      </div>

      <style>{`
        .lg-hidden-btn { display: flex; }
        @media (min-width: 1024px) { .lg-hidden-btn { display: none !important; } }
        @keyframes dropdownFadeIn {
          from { opacity: 0; transform: translateY(-6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </header>
  );
};

export default Header;
