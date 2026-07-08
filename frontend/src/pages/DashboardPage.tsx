import React, { useState, useEffect, useRef } from 'react';
import {
  AreaChart, Area,
  XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { Calendar, Users, TrendingUp, Activity } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import AppLayout from '../components/layout/AppLayout';
import Badge from '../components/ui/Badge';
import useWebSocket from '../hooks/useWebSocket';
import api from '../services/api';

// ─── Types ────────────────────────────────────────────────────────────────────
interface DashboardStats   { totalEvents: number; activeEventsCount: number; totalOrganizers: number; totalGuestsInvited: number; totalConfirmedRsvps: number; totalRevenueKes: number; }
interface RsvpDataPoint    { date: string; rsvps: number; }
interface RecentRsvp       { id: string; guestName: string; eventTitle: string; status: 'CONFIRMED'|'DECLINED'|'MAYBE'|'WAITLISTED'; timestamp: string; }
interface TopEvent         { id: string; title: string; confirmed: number; capacity: number; }

// ─── Design tokens ────────────────────────────────────────────────────────────

const STATUS_BADGE: Record<string, 'success'|'danger'|'warning'|'neutral'> = {
  CONFIRMED: 'success', DECLINED: 'danger', MAYBE: 'warning', WAITLISTED: 'neutral',
};

// Avatar color derived from name hash — same person always same color
const AVATAR_PALETTE = ['#1D4ED8','#0F766E','#7C3AED','#B45309','#BE123C'];
const avatarColor = (name: string) =>
  AVATAR_PALETTE[name.split('').reduce((a, c) => a + c.charCodeAt(0), 0) % AVATAR_PALETTE.length];

// ─── Fallback data ────────────────────────────────────────────────────────────
const FALLBACK_STATS: DashboardStats = { totalEvents: 0, activeEventsCount: 0, totalOrganizers: 0, totalGuestsInvited: 0, totalConfirmedRsvps: 0, totalRevenueKes: 0 };
const FALLBACK_RSVP: RsvpDataPoint[] = Array.from({ length: 30 }, (_, i) => ({
  date: new Date(Date.now() - (29 - i) * 86400000).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
  rsvps: Math.floor(Math.random() * 120) + 20,
}));

// ─── Animated number component ───────────────────────────────────────────────
const AnimatedNumber: React.FC<{ value: number; className?: string }> = ({ value, className }) => {
  const [display, setDisplay] = useState(0);
  const prev    = useRef(0);
  const frameRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    const from     = prev.current;
    const to       = value;
    const duration = 600;
    const start    = performance.now();

    const tick = (now: number) => {
      const p = Math.min((now - start) / duration, 1);
      const e = 1 - Math.pow(1 - p, 3); // easeOutCubic
      setDisplay(Math.round(from + (to - from) * e));
      if (p < 1) frameRef.current = requestAnimationFrame(tick);
      else prev.current = to;
    };

    frameRef.current = requestAnimationFrame(tick);
    return () => { if (frameRef.current) cancelAnimationFrame(frameRef.current); };
  }, [value]);

  return <span className={className}>{display.toLocaleString()}</span>;
};

// ─── Skeleton loader ──────────────────────────────────────────────────────────
const Skeleton: React.FC<{ className?: string; style?: React.CSSProperties }> = ({ className = '' }) => (
  <div className={`rounded-xl bg-slate-200 overflow-hidden relative ${className}`}>
    <div style={{
      position: 'absolute', inset: 0,
      background: 'linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.6) 50%, transparent 100%)',
      animation: 'shimmer 1.4s infinite',
    }} />
  </div>
);

// ─── Custom chart tooltip ─────────────────────────────────────────────────────
const DarkTooltip: React.FC<any> = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{ background: 'rgba(11,20,34,0.95)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, padding: '8px 14px', fontSize: 12, color: '#E2E8F0', fontFamily: 'Inter', boxShadow: '0 8px 24px rgba(0,0,0,0.4)' }}>
      <p style={{ color: 'var(--text-muted)', marginBottom: 4 }}>{label}</p>
      <p style={{ color: '#2563EB', fontWeight: 600 }}>{payload[0]?.value?.toLocaleString()} RSVPs</p>
    </div>
  );
};


// ─── Stat card ────────────────────────────────────────────────────────────────
const ACCENT_MAP = {
  blue:  { border: '#2563EB', iconBg: 'rgba(37,99,235,0.1)',  iconColor: '#2563EB' },
  navy:  { border: '#1E3A5F', iconBg: 'rgba(30,58,95,0.1)',   iconColor: '#1E3A5F' },
  green: { border: '#16A34A', iconBg: 'rgba(22,163,74,0.1)',  iconColor: '#16A34A' },
  amber: { border: '#D97706', iconBg: 'rgba(217,119,6,0.1)',  iconColor: '#D97706' },
};

const StatCard: React.FC<{ label: string; value: number; icon: React.ReactNode; accent: keyof typeof ACCENT_MAP }> = ({ label, value, icon, accent }) => {
  const { border, iconBg, iconColor } = ACCENT_MAP[accent];
  const [hovered, setHovered] = useState(false);

  return (
    <div
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: 'var(--bg-card)',
        borderRadius: 12,
        padding: '24px',
        borderLeft: `4px solid ${border}`,
        boxShadow: hovered ? '0 8px 24px rgba(0,0,0,0.12)' : 'var(--shadow-card)',
        transform: hovered ? 'translateY(-2px)' : 'none',
        transition: 'all 200ms ease',
        cursor: 'default',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <div>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', fontFamily: 'Inter', fontWeight: 500, marginBottom: 4 }}>{label}</p>
          <p style={{ fontSize: 32, fontFamily: "'Space Grotesk',sans-serif", fontWeight: 700, color: 'var(--text-primary)', lineHeight: 1 }}>
            <AnimatedNumber value={value} />
          </p>
        </div>
        <div style={{ padding: 10, borderRadius: 10, background: iconBg, color: iconColor }}>
          {icon}
        </div>
      </div>
    </div>
  );
};

// ─── Main component ───────────────────────────────────────────────────────────
const DashboardPage: React.FC = () => {
  const { connected, alerts } = useWebSocket();

  const [stats,      setStats]      = useState<DashboardStats>(FALLBACK_STATS);
  const [rsvpData] = useState<RsvpDataPoint[]>(FALLBACK_RSVP);
  const [topEvents] = useState<TopEvent[]>([]);
  const [recentRsvps, setRecentRsvps] = useState<RecentRsvp[]>([]);
  const [loading,    setLoading]    = useState(true);

  // Pulse-line flash on new WebSocket message
  const [wsPulse, setWsPulse] = useState(false);
  const pulseTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        const [sR, recR] = await Promise.allSettled([
          api.get<DashboardStats>('/api/admin/dashboard/stats'),
          api.get<RecentRsvp[]>('/api/admin/dashboard/recent-rsvps'),
        ]);
        if (sR.status   === 'fulfilled') setStats(sR.value.data);
        if (recR.status === 'fulfilled') setRecentRsvps(recR.value.data);
      } catch {}
      finally { setLoading(false); }
    };
    fetchAll();
  }, []);

  // Prepend live WebSocket RSVPs + trigger pulse flash
  useEffect(() => {
    if (!alerts.length) return;
    const latest = alerts[0];

    // Flash the pulse line
    setWsPulse(true);
    if (pulseTimer.current) clearTimeout(pulseTimer.current);
    pulseTimer.current = setTimeout(() => setWsPulse(false), 600);

    if (latest.type !== 'RSVP') return;
    const liveRsvp: RecentRsvp = {
      id:         `live-${latest.timestamp}`,
      guestName:  latest.metadata?.guestName  ?? 'Guest',
      eventTitle: latest.metadata?.eventTitle ?? 'Event',
      status:     (latest.metadata?.status as RecentRsvp['status']) ?? 'CONFIRMED',
      timestamp:  latest.timestamp,
    };
    setRecentRsvps(prev => [liveRsvp, ...prev].slice(0, 20));
  }, [alerts]);

  return (
    <AppLayout>
      <style>{`
        @keyframes shimmer {
          0%   { transform: translateX(-100%); }
          100% { transform: translateX(100%); }
        }
        @keyframes slideIn {
          from { opacity: 0; transform: translateY(-8px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @keyframes rowFadeIn {
          from { opacity: 0; transform: translateY(4px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @keyframes wsPulse {
          0%,100% { opacity: 1; }
          50%     { opacity: 0.3; }
        }
      `}</style>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>

        {/* ── Page header with pulse-line ── */}
        <div>
          <h1 style={{ fontFamily: "'Space Grotesk',sans-serif", fontSize: 24, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 8 }}>
            Dashboard
          </h1>
          {/* Pulse-line tied to WebSocket state */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ flex: 1, maxWidth: 300 }}>
              <svg width="100%" height="8" viewBox="0 0 300 8" preserveAspectRatio="none">
                <line x1="0" y1="4" x2="300" y2="4" stroke={connected ? '#2563EB' : '#CBD5E1'} strokeWidth="1" strokeOpacity="0.2"/>
                <line x1="0" y1="4" x2="300" y2="4"
                  stroke={connected ? '#2563EB' : '#CBD5E1'}
                  strokeWidth="2"
                  strokeDasharray="300"
                  strokeDashoffset="300"
                  style={{ animation: connected ? `wsPulse ${wsPulse ? '0.3s' : '2s'} ease-in-out infinite` : 'none', opacity: connected ? 1 : 0.3 }}
                />
              </svg>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <div style={{ width: 7, height: 7, borderRadius: '50%', background: connected ? '#16A34A' : '#CBD5E1', animation: connected ? 'wsPulse 2s ease-in-out infinite' : 'none' }} />
              <span style={{ fontSize: 12, fontFamily: 'Inter', fontWeight: 500, color: connected ? '#16A34A' : 'var(--text-muted)' }}>
                {connected ? 'LIVE' : 'Reconnecting...'}
              </span>
            </div>
          </div>
        </div>

        {/* ── Stat cards ── */}
        {loading ? (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 16 }}>
            {[1,2,3,4].map(i => <Skeleton key={i} style={{ height: 100 }} />)}
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 16 }}>
            <StatCard label="Total Events"     value={stats.totalEvents}      icon={<Calendar size={20}/>}   accent="blue"  />
            <StatCard label="Total Organizers" value={stats.totalOrganizers}  icon={<Users size={20}/>}      accent="navy"  />
            <StatCard label="RSVPs Today"      value={stats.totalConfirmedRsvps}       icon={<TrendingUp size={20}/>} accent="green" />
            <StatCard label="Active Events"    value={stats.activeEventsCount}     icon={<Activity size={20}/>}   accent="amber" />
          </div>
        )}

        {/* ── Charts row ── */}
        <div style={{ display: 'grid', gridTemplateColumns: '3fr 2fr', gap: 20 }}>

          {/* Area chart */}
          <div style={{ background: 'var(--bg-card)', borderRadius: 12, padding: 24, boxShadow: 'var(--shadow-card)' }}>
            <h3 style={{ fontFamily: "'Space Grotesk',sans-serif", fontSize: 14, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 16 }}>
              RSVPs — Last 30 Days
            </h3>
            {loading ? <Skeleton style={{ height: 220 }} /> : (
              <ResponsiveContainer width="100%" height={220}>
                <AreaChart data={rsvpData} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
                  <defs>
                    <linearGradient id="rsvpGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%"  stopColor="#2563EB" stopOpacity={0.35}/>
                      <stop offset="95%" stopColor="#2563EB" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)"/>
                  <XAxis dataKey="date" tick={{ fontSize: 11, fill: 'var(--text-muted)' }} tickLine={false} axisLine={false} interval={4}/>
                  <YAxis tick={{ fontSize: 11, fill: 'var(--text-muted)' }} tickLine={false} axisLine={false}/>
                  <Tooltip content={<DarkTooltip/>}/>
                  <Area type="monotone" dataKey="rsvps" stroke="#2563EB" strokeWidth={2} fill="url(#rsvpGrad)"/>
                </AreaChart>
              </ResponsiveContainer>
            )}
          </div>

          {/* Platform summary */}
          <div style={{ background: 'var(--bg-card)', borderRadius: 12, padding: 24, boxShadow: 'var(--shadow-card)', display: 'flex', flexDirection: 'column', gap: 24 }}>
            <h3 style={{ fontFamily: "'Space Grotesk',sans-serif", fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' }}>
              Platform Summary
            </h3>
            {loading ? <Skeleton style={{ height: 160 }} /> : (
              <>
                <div style={{ borderLeft: '3px solid #16A34A', paddingLeft: 16 }}>
                  <p style={{ fontSize: 12, color: 'var(--text-secondary)', fontFamily: 'Inter', marginBottom: 4 }}>Total Guests Invited</p>
                  <p style={{ fontSize: 28, fontFamily: "'Space Grotesk',sans-serif", fontWeight: 700, color: 'var(--text-primary)' }}>
                    <AnimatedNumber value={stats.totalGuestsInvited} />
                  </p>
                </div>
                <div style={{ borderLeft: '3px solid #D97706', paddingLeft: 16 }}>
                  <p style={{ fontSize: 12, color: 'var(--text-secondary)', fontFamily: 'Inter', marginBottom: 4 }}>Total Revenue (KES)</p>
                  <p style={{ fontSize: 28, fontFamily: "'Space Grotesk',sans-serif", fontWeight: 700, color: 'var(--text-primary)' }}>
                    <AnimatedNumber value={stats.totalRevenueKes} />
                  </p>
                </div>
              </>
            )}
          </div>
        </div>

        {/* ── Bottom row ── */}
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 20 }}>

          {/* Recent activity */}
          <div style={{ background: 'var(--bg-card)', borderRadius: 12, padding: 24, boxShadow: 'var(--shadow-card)' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
              <h3 style={{ fontFamily: "'Space Grotesk',sans-serif", fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' }}>Recent Activity</h3>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <div style={{ width: 7, height: 7, borderRadius: '50%', background: connected ? '#16A34A' : '#CBD5E1', animation: connected ? 'wsPulse 2s ease-in-out infinite' : 'none' }}/>
                <span style={{ fontSize: 11, fontFamily: 'Inter', fontWeight: 500, color: connected ? '#16A34A' : 'var(--text-muted)' }}>
                  {connected ? 'LIVE' : 'OFFLINE'}
                </span>
              </div>
            </div>

            <div style={{ maxHeight: 280, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 4 }}>
              {loading ? (
                [1,2,3,4].map(i => <Skeleton key={i} style={{ height: 52 }}/>)
              ) : recentRsvps.length === 0 ? (
                <p style={{ fontSize: 13, color: 'var(--text-muted)', textAlign: 'center', padding: '32px 0', fontFamily: 'Inter' }}>No recent activity yet.</p>
              ) : (
                recentRsvps.map((rsvp, idx) => (
                  <div key={rsvp.id} style={{
                    display: 'flex', alignItems: 'center', gap: 12, padding: '10px 8px', borderRadius: 8,
                    animation: idx === 0 ? 'slideIn 300ms ease' : `rowFadeIn 300ms ease ${Math.min(idx, 9) * 30}ms both`,
                    transition: 'background 150ms',
                  }}
                    onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-app)')}
                    onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                  >
                    <div style={{ width: 36, height: 36, borderRadius: '50%', background: avatarColor(rsvp.guestName), display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--bg-card)', fontSize: 13, fontWeight: 700, flexShrink: 0, fontFamily: 'Inter' }}>
                      {rsvp.guestName[0]?.toUpperCase()}
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', fontFamily: 'Inter', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{rsvp.guestName}</p>
                      <p style={{ fontSize: 12, color: 'var(--text-secondary)', fontFamily: 'Inter', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{rsvp.eventTitle}</p>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4, flexShrink: 0 }}>
                      <Badge variant={STATUS_BADGE[rsvp.status] ?? 'neutral'}>{rsvp.status}</Badge>
                      <span style={{ fontSize: 11, color: 'var(--text-muted)', fontFamily: "'JetBrains Mono',monospace" }}>
                        {formatDistanceToNow(new Date(rsvp.timestamp), { addSuffix: true })}
                      </span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Top events */}
          <div style={{ background: 'var(--bg-card)', borderRadius: 12, padding: 24, boxShadow: 'var(--shadow-card)' }}>
            <h3 style={{ fontFamily: "'Space Grotesk',sans-serif", fontSize: 14, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 16 }}>Top Events</h3>
            {loading ? (
              [1,2,3].map(i => <Skeleton key={i} style={{ height: 40, marginBottom: 12 }}/>)
            ) : topEvents.length === 0 ? (
              <p style={{ fontSize: 13, color: 'var(--text-muted)', textAlign: 'center', padding: '32px 0', fontFamily: 'Inter' }}>No events yet.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {topEvents.slice(0, 5).map((ev, i) => {
                  const pct = ev.capacity > 0 ? Math.round((ev.confirmed / ev.capacity) * 100) : 0;
                  return (
                    <div key={ev.id}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0 }}>
                          <span style={{ fontSize: 12, fontWeight: 700, color: 'var(--text-muted)', width: 16, flexShrink: 0, fontFamily: "'Space Grotesk',sans-serif" }}>{i+1}</span>
                          <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', fontFamily: 'Inter', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{ev.title}</span>
                        </div>
                        <span style={{ fontSize: 12, color: 'var(--text-secondary)', fontFamily: "'JetBrains Mono',monospace", flexShrink: 0, marginLeft: 8 }}>{ev.confirmed}/{ev.capacity}</span>
                      </div>
                      <div style={{ height: 4, background: 'var(--border)', borderRadius: 99, marginLeft: 24 }}>
                        <div style={{ height: '100%', width: `${pct}%`, borderRadius: 99, background: 'linear-gradient(90deg, #1E3A5F, #2563EB)', transition: 'width 500ms ease' }}/>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </AppLayout>
  );
};

export default DashboardPage;
