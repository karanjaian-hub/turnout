import React, { useState, useEffect } from 'react';
import {
  AreaChart, Area, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { Calendar, Users, TrendingUp, Activity } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import AppLayout from '../components/layout/AppLayout';
import StatCard from '../components/ui/StatCard';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Spinner from '../components/ui/Spinner';
import useWebSocket from '../hooks/useWebSocket';
import api from '../services/api';

// ─── Types ────────────────────────────────────────────────────────────────────

interface DashboardStats {
  totalEvents: number;
  totalOrganizers: number;
  rsvpsToday: number;
  activeEvents: number;
}

interface RsvpDataPoint {
  date: string;
  rsvps: number;
}

interface TierDataPoint {
  name: string;
  value: number;
}

interface RecentRsvp {
  id: string;
  guestName: string;
  eventTitle: string;
  status: 'CONFIRMED' | 'DECLINED' | 'MAYBE' | 'WAITLISTED';
  timestamp: string;
}

interface TopEvent {
  id: string;
  title: string;
  confirmed: number;
  capacity: number;
}

// ─── Chart colours — Turnout design tokens ───────────────────────────────────
const TIER_COLOURS = ['#1E3A5F', '#2563EB', '#16A34A'];

const STATUS_BADGE: Record<string, 'success' | 'danger' | 'warning' | 'neutral'> = {
  CONFIRMED:  'success',
  DECLINED:   'danger',
  MAYBE:      'warning',
  WAITLISTED: 'neutral',
};

// ─── Fallback data — shown while the backend is loading or unreachable ────────
// This keeps the UI looking alive during development without a running backend.
const FALLBACK_STATS: DashboardStats = {
  totalEvents: 0, totalOrganizers: 0, rsvpsToday: 0, activeEvents: 0,
};

const FALLBACK_RSVP_DATA: RsvpDataPoint[] = Array.from({ length: 30 }, (_, i) => ({
  date: new Date(Date.now() - (29 - i) * 86400000)
    .toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
  rsvps: Math.floor(Math.random() * 120) + 20,
}));

const FALLBACK_TIER_DATA: TierDataPoint[] = [
  { name: 'FREE', value: 60 },
  { name: 'PRO', value: 30 },
  { name: 'ENTERPRISE', value: 10 },
];

// ─── Component ────────────────────────────────────────────────────────────────
const DashboardPage: React.FC = () => {
  const { connected, alerts } = useWebSocket();

  const [stats, setStats]           = useState<DashboardStats>(FALLBACK_STATS);
  const [rsvpData, setRsvpData]     = useState<RsvpDataPoint[]>(FALLBACK_RSVP_DATA);
  const [tierData, setTierData]     = useState<TierDataPoint[]>(FALLBACK_TIER_DATA);
  const [topEvents, setTopEvents]   = useState<TopEvent[]>([]);
  const [recentRsvps, setRecentRsvps] = useState<RecentRsvp[]>([]);
  const [loading, setLoading]       = useState(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        // Fire all requests in parallel — no reason to wait for each sequentially
        const [statsRes, rsvpRes, tierRes, topRes, recentRes] = await Promise.allSettled([
          api.get<DashboardStats>('/api/admin/stats'),
          api.get<RsvpDataPoint[]>('/api/admin/rsvp-trend'),
          api.get<TierDataPoint[]>('/api/admin/tier-distribution'),
          api.get<TopEvent[]>('/api/admin/top-events'),
          api.get<RecentRsvp[]>('/api/admin/recent-rsvps'),
        ]);

        // allSettled means one failing endpoint won't break the whole dashboard
        if (statsRes.status === 'fulfilled') setStats(statsRes.value.data);
        if (rsvpRes.status  === 'fulfilled') setRsvpData(rsvpRes.value.data);
        if (tierRes.status  === 'fulfilled') setTierData(tierRes.value.data);
        if (topRes.status   === 'fulfilled') setTopEvents(topRes.value.data);
        if (recentRes.status === 'fulfilled') setRecentRsvps(recentRes.value.data);

      } catch {
        // Silently fall back to placeholder data — dashboard still renders
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  // Prepend live WebSocket RSVPs to the recent activity feed
  useEffect(() => {
    if (alerts.length === 0) return;
    const latest = alerts[0];
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
      {loading ? (
        <div className="flex items-center justify-center h-64">
          <Spinner size="lg" />
        </div>
      ) : (
        <div className="space-y-6">

          {/* ── Stat cards ── */}
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
            <StatCard
              label="Total Events"
              value={stats.totalEvents}
              icon={<Calendar size={20} />}
              accent="blue"
            />
            <StatCard
              label="Total Organizers"
              value={stats.totalOrganizers}
              icon={<Users size={20} />}
              accent="navy"
            />
            <StatCard
              label="RSVPs Today"
              value={stats.rsvpsToday}
              icon={<TrendingUp size={20} />}
              accent="green"
            />
            <StatCard
              label="Active Events"
              value={stats.activeEvents}
              icon={<Activity size={20} />}
              accent="amber"
            />
          </div>

          {/* ── Charts row ── */}
          <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">

            {/* Area chart — RSVPs over 30 days */}
            <Card className="xl:col-span-3 p-6">
              <h3 className="text-sm font-semibold text-navy mb-4">
                RSVPs — Last 30 Days
              </h3>
              <ResponsiveContainer width="100%" height={220}>
                <AreaChart data={rsvpData} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
                  <defs>
                    {/* Gradient fill under the line — looks far better than a flat fill */}
                    <linearGradient id="rsvpGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%"  stopColor="#2563EB" stopOpacity={0.25} />
                      <stop offset="95%" stopColor="#2563EB" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                  <XAxis
                    dataKey="date"
                    tick={{ fontSize: 11, fill: '#94A3B8' }}
                    tickLine={false}
                    axisLine={false}
                    interval={4}
                  />
                  <YAxis
                    tick={{ fontSize: 11, fill: '#94A3B8' }}
                    tickLine={false}
                    axisLine={false}
                  />
                  <Tooltip
                    contentStyle={{
                      background: '#fff',
                      border: '1px solid #E2E8F0',
                      borderRadius: '8px',
                      fontSize: '12px',
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="rsvps"
                    stroke="#2563EB"
                    strokeWidth={2}
                    fill="url(#rsvpGradient)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </Card>

            {/* Pie chart — tier distribution */}
            <Card className="xl:col-span-2 p-6">
              <h3 className="text-sm font-semibold text-navy mb-4">
                Organizer Tiers
              </h3>
              <ResponsiveContainer width="100%" height={160}>
                <PieChart>
                  <Pie
                    data={tierData}
                    cx="50%"
                    cy="50%"
                    innerRadius={45}
                    outerRadius={70}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {tierData.map((_, i) => (
                      <Cell key={i} fill={TIER_COLOURS[i % TIER_COLOURS.length]} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      background: '#fff',
                      border: '1px solid #E2E8F0',
                      borderRadius: '8px',
                      fontSize: '12px',
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
              {/* Custom legend below the chart */}
              <div className="flex justify-center gap-4 mt-2">
                {tierData.map((tier, i) => (
                  <div key={tier.name} className="flex items-center gap-1.5">
                    <span
                      className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                      style={{ background: TIER_COLOURS[i % TIER_COLOURS.length] }}
                    />
                    <span className="text-xs text-slate-500">{tier.name}</span>
                    <span className="text-xs font-semibold text-navy">{tier.value}%</span>
                  </div>
                ))}
              </div>
            </Card>
          </div>

          {/* ── Bottom row ── */}
          <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">

            {/* Live activity feed */}
            <Card className="xl:col-span-2 p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-navy">Recent Activity</h3>
                {/* Pulsing dot — shows WebSocket connection status */}
                <div className="flex items-center gap-1.5">
                  <span className={`w-2 h-2 rounded-full ${
                    connected ? 'bg-success animate-pulse' : 'bg-slate-300'
                  }`} />
                  <span className={`text-xs font-medium ${
                    connected ? 'text-success' : 'text-slate-400'
                  }`}>
                    {connected ? 'LIVE' : 'OFFLINE'}
                  </span>
                </div>
              </div>

              <div className="space-y-3 max-h-64 overflow-y-auto pr-1">
                {recentRsvps.length === 0 ? (
                  <p className="text-sm text-slate-400 text-center py-8">
                    No recent activity yet.
                  </p>
                ) : (
                  recentRsvps.map(rsvp => (
                    <div
                      key={rsvp.id}
                      className="flex items-center gap-3 p-2.5 rounded-input hover:bg-slate-50 transition-colors"
                    >
                      {/* Avatar — initials */}
                      <div className="w-8 h-8 rounded-full bg-primary-500/10 text-primary-500 flex items-center justify-center text-xs font-bold flex-shrink-0">
                        {rsvp.guestName[0]?.toUpperCase()}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-navy truncate">
                          {rsvp.guestName}
                        </p>
                        <p className="text-xs text-slate-400 truncate">
                          {rsvp.eventTitle}
                        </p>
                      </div>
                      <div className="flex flex-col items-end gap-1 flex-shrink-0">
                        <Badge variant={STATUS_BADGE[rsvp.status] ?? 'neutral'}>
                          {rsvp.status}
                        </Badge>
                        <span className="text-xs text-slate-400">
                          {formatDistanceToNow(new Date(rsvp.timestamp), { addSuffix: true })}
                        </span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </Card>

            {/* Top events */}
            <Card className="xl:col-span-1 p-6">
              <h3 className="text-sm font-semibold text-navy mb-4">Top Events</h3>

              <div className="space-y-4">
                {topEvents.length === 0 ? (
                  <p className="text-sm text-slate-400 text-center py-8">
                    No events yet.
                  </p>
                ) : (
                  topEvents.slice(0, 5).map((event, i) => {
                    const pct = event.capacity > 0
                      ? Math.round((event.confirmed / event.capacity) * 100)
                      : 0;

                    return (
                      <div key={event.id}>
                        <div className="flex items-center justify-between mb-1">
                          <div className="flex items-center gap-2 min-w-0">
                            {/* Rank number */}
                            <span className="text-xs font-bold text-slate-400 w-4 flex-shrink-0">
                              {i + 1}
                            </span>
                            <span className="text-sm font-medium text-navy truncate">
                              {event.title}
                            </span>
                          </div>
                          <span className="text-xs text-slate-400 flex-shrink-0 ml-2">
                            {event.confirmed}/{event.capacity}
                          </span>
                        </div>
                        {/* Capacity progress bar */}
                        <div className="w-full bg-slate-100 rounded-full h-1.5 ml-6">
                          <div
                            className="h-1.5 rounded-full bg-gradient-to-r from-navy to-primary-500 transition-all duration-500"
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </Card>

          </div>
        </div>
      )}
    </AppLayout>
  );
};

export default DashboardPage;
