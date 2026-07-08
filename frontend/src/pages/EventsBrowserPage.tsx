import React, { useState, useEffect, useCallback } from 'react';
import { Search, Calendar } from 'lucide-react';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Spinner from '../components/ui/Spinner';
import EmptyState from '../components/ui/EmptyState';
import Table, { Column } from '../components/ui/Table';
import Pagination from '../components/ui/Pagination';
import api from '../services/api';
import toast from 'react-hot-toast';

interface AdminEvent {
  id: string;
  title: string;
  organizerName?: string;
  createdBy?: string;
  eventDate: string;
  location: string;
  status: 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  currentRsvpCount: number;
  maxCapacity: number;
}

const STATUS_BADGE: Record<string, 'info' | 'success' | 'neutral' | 'danger'> = {
  DRAFT:     'info',
  ACTIVE:    'success',
  COMPLETED: 'neutral',
  CANCELLED: 'danger',
};

const STATUSES = ['ALL', 'DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED'];

const EventsBrowserPage: React.FC = () => {
  const [events, setEvents]     = useState<AdminEvent[]>([]);
  const [filtered, setFiltered] = useState<AdminEvent[]>([]);
  const [search, setSearch]     = useState('');
  const [status, setStatus]     = useState('ALL');
  const [loading, setLoading]   = useState(true);
  const [page,       setPage]       = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchEvents = useCallback(async (p: number = 0) => {
    try {
      const { data } = await api.get<any>(`/api/events?page=${p}&size=10`);
      const list = Array.isArray(data) ? data : (data.content ?? []);
      setEvents(list);
      setFiltered(list);
      setTotalPages(data.totalPages ?? 0);
    } catch {
      toast.error('Failed to load events.');
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => { fetchEvents(page); }, [fetchEvents, page]);



  useEffect(() => {
    let result = events;
    if (status !== 'ALL') result = result.filter(e => e.status === status);
    if (search) {
      const q = search.toLowerCase();
      result = result.filter(e =>
        e.title.toLowerCase().includes(q) ||
        ( e.organizerName ?? '').toLowerCase().includes(q) ||
        e.location.toLowerCase().includes(q)
      );
    }
    setFiltered(result);
  }, [search, status, events]);

  const columns: Column<AdminEvent>[] = [
    {
      key: 'title',
      header: 'Event',
      sortable: true,
      render: e => (
        <div>
          <p className="font-medium text-navy">{e.title}</p>
          <p className="text-xs text-slate-400">{e.location}</p>
        </div>
      ),
    },
    {
      key: 'organizer',
      header: 'Organizer',
      render: e => <span className="text-slate-600">{e.organizerName ?? e.createdBy ?? '—'}</span>,
    },
    {
      key: 'date',
      header: 'Date',
      sortable: true,
      render: e => new Date(e.eventDate).toLocaleDateString(),
    },
    {
      key: 'status',
      header: 'Status',
      render: e => <Badge variant={STATUS_BADGE[e.status] ?? 'neutral'}>{e.status}</Badge>,
    },
    {
      key: 'capacity',
      header: 'RSVPs',
      render: e => (
        <div>
          <span className="font-medium">{e.currentRsvpCount}</span>
          <span className="text-slate-400"> / {e.maxCapacity}</span>
        </div>
      ),
    },
  ];

  return (
    <AppLayout>
      <div className="space-y-4">

        {/* Filters */}
        <Card padding={false} className="p-4">
          <div className="flex flex-wrap items-center gap-3">
            <div className="relative">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                placeholder="Search events…"
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="pl-9 pr-4 py-2 border border-slate-200 rounded-input text-sm
                           outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
              />
            </div>
            <div className="flex gap-1">
              {STATUSES.map(s => (
                <button
                  key={s}
                  onClick={() => setStatus(s)}
                  className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors
                    ${status === s
                      ? 'bg-navy text-white'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        </Card>

        <Card padding={false}>
          {loading ? (
            <div className="flex items-center justify-center h-48"><Spinner size="lg" /></div>
          ) : (
            <>
              <Table
                columns={columns}
                data={filtered}
                keyExtractor={e => e.id}
                emptyState={
                  <EmptyState
                    icon={<Calendar size={32} />}
                    heading="No events found"
                    subtext="Events created by organizers appear here."
                  />
                }
              />
              <Pagination page={page} totalPages={totalPages} onPageChange={p => setPage(p)}/>
            </>
          )}
        </Card>
      </div>
    </AppLayout>
  );
};

export default EventsBrowserPage;
