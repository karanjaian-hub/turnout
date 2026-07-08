import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, UserX, UserCheck, ChevronRight } from 'lucide-react';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import EmptyState from '../components/ui/EmptyState';
import Modal from '../components/ui/Modal';
import Table, { Column } from '../components/ui/Table';
import Pagination from '../components/ui/Pagination';
import api from '../services/api';
import toast from 'react-hot-toast';
import { Users } from 'lucide-react';

// ─── Types ────────────────────────────────────────────────────────────────────
interface Organizer {
  id: string;
  username: string;
  email: string;
  fullName: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION' | 'DEACTIVATED';
  tier?: 'FREE' | 'PRO' | 'ENTERPRISE';
  role?: string;
  eventCount?: number;
  joinedAt?: string;
  createdAt?: string;
}

const STATUS_BADGE: Record<string, 'success' | 'danger' | 'warning' | 'neutral'> = {
  ACTIVE:               'success',
  SUSPENDED:            'danger',
  PENDING_VERIFICATION: 'warning',
  DEACTIVATED:          'neutral',
};

const TIER_BADGE: Record<string, 'info' | 'warning' | 'success'> = {
  FREE:       'info',
  PRO:        'warning',
  ENTERPRISE: 'success',
};

// ─── Component ────────────────────────────────────────────────────────────────
const OrganizersPage: React.FC = () => {
  const navigate = useNavigate();

  const [organizers, setOrganizers] = useState<Organizer[]>([]);
  const [filtered, setFiltered]     = useState<Organizer[]>([]);
  const [search, setSearch]         = useState('');
  const [loading, setLoading]       = useState(true);

  // Confirm modal state
  const [confirmModal, setConfirmModal] = useState<{
    open: boolean;
    organizer: Organizer | null;
    action: 'suspend' | 'reactivate';
  }>({ open: false, organizer: null, action: 'suspend' });
  const [actioning,   setActioning]  = useState(false);
  const [page,        setPage]        = useState(0);
  const [totalPages,  setTotalPages]  = useState(0);

  const fetchOrganizers = useCallback(async (p: number = 0) => {
    try {
      const { data } = await api.get<any>(`/api/admin/users?page=${p}&size=10`);
      const list = Array.isArray(data) ? data : (data.content ?? []);
        setOrganizers(list);
        setFiltered(list);
      setTotalPages(data.totalPages ?? 0);
    } catch {
      toast.error('Failed to load organizers.');
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => { fetchOrganizers(page); }, [fetchOrganizers, page]);


  // Client-side search — filters on name, email, or username
  useEffect(() => {
    const q = search.toLowerCase();
    setFiltered(
      organizers.filter(o =>
        o.fullName.toLowerCase().includes(q) ||
        o.email.toLowerCase().includes(q) ||
        o.username.toLowerCase().includes(q)
      )
    );
  }, [search, organizers]);

  const openConfirm = (organizer: Organizer, action: 'suspend' | 'reactivate') => {
    setConfirmModal({ open: true, organizer, action });
  };

  const handleAction = async () => {
    if (!confirmModal.organizer) return;
    setActioning(true);
    const { id, fullName } = confirmModal.organizer;
    const endpoint = confirmModal.action === 'suspend'
      ? `/api/admin/users/${id}/suspend`
      : `/api/admin/users/${id}/suspend`;

    try {
      await api.patch(endpoint);
      toast.success(
        confirmModal.action === 'suspend'
          ? `${fullName} suspended.`
          : `${fullName} reactivated.`
      );
      // Optimistic update — flip status immediately without waiting for refetch
      const newStatus = confirmModal.action === 'suspend' ? 'SUSPENDED' : 'ACTIVE';
      setOrganizers(prev => prev.map(o => o.id === id ? { ...o, status: newStatus as any } : o));
      setFiltered(prev => prev.map(o => o.id === id ? { ...o, status: newStatus as any } : o));
      setConfirmModal({ open: false, organizer: null, action: 'suspend' });
    } catch {
      toast.error('Action failed. Please try again.');
    } finally {
      setActioning(false);
    }
  };

  const columns: Column<Organizer>[] = [
    {
      key: 'name',
      header: 'Organizer',
      sortable: true,
      render: o => (
        <div>
          <p className="font-medium text-navy">{o.fullName}</p>
          <p className="text-xs text-slate-400">@{o.username}</p>
        </div>
      ),
    },
    {
      key: 'email',
      header: 'Email',
      render: o => <span className="text-slate-600">{o.email}</span>,
    },
    {
      key: 'tier',
      header: 'Plan',
      render: o => o.tier ? <Badge variant={TIER_BADGE[o.tier]}>{o.tier}</Badge> : <span className='text-slate-400 text-xs'>—</span>,
    },
    {
      key: 'eventCount',
      header: 'Events',
      sortable: true,
      render: o => <span className="font-medium">{o.eventCount ?? '—'}</span>,
    },
    {
      key: 'status',
      header: 'Status',
      render: o => (
        <Badge variant={STATUS_BADGE[o.status] ?? 'neutral'}>
          {o.status.replace('_', ' ')}
        </Badge>
      ),
    },
    {
      key: 'joinedAt',
      header: 'Joined',
      render: o => (
        <span className="text-slate-500 text-xs">
          {new Date(o.joinedAt ?? o.createdAt ?? '').toLocaleDateString()}
        </span>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: o => (
        <div className="flex items-center gap-2">
          {o.status === 'ACTIVE' ? (
            <Button
              size="sm"
              variant="danger"
              icon={<UserX size={14} />}
              onClick={() => openConfirm(o, 'suspend')}
            >
              Suspend
            </Button>
          ) : (
            <Button
              size="sm"
              variant="secondary"
              icon={<UserCheck size={14} />}
              onClick={() => openConfirm(o, 'reactivate')}
            >
              Reactivate
            </Button>
          )}
          <Button
            size="sm"
            variant="ghost"
            icon={<ChevronRight size={14} />}
            onClick={() => navigate(`/organizers/${o.id}`)}
          >
            View
          </Button>
        </div>
      ),
    },
  ];

  return (
    <AppLayout>
      <div className="space-y-4">

        {/* Search bar */}
        <Card padding={false} className="p-4">
          <div className="relative max-w-sm">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search by name, email or username…"
              value={search}
              onChange={e => setSearch(e.target.value)}
              className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-input text-sm
                         outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
            />
          </div>
        </Card>

        {/* Table */}
        <Card padding={false}>
          {loading ? (
            <div className="flex items-center justify-center h-48">
              <Spinner size="lg" />
            </div>
          ) : (
            <>
              <Table
                columns={columns}
                data={filtered}
                keyExtractor={o => o.id}
                emptyState={
                  <EmptyState
                    icon={<Users size={32} />}
                    heading="No organizers found"
                    subtext="Organizers appear here once they register."
                  />
                }
              />
              <Pagination page={page} totalPages={totalPages} onPageChange={p => setPage(p)}/>
            </>
          )}
        </Card>
      </div>

      {/* Confirm modal */}
      <Modal
        isOpen={confirmModal.open}
        onClose={() => setConfirmModal(s => ({ ...s, open: false }))}
        title={confirmModal.action === 'suspend' ? 'Suspend Organizer' : 'Reactivate Organizer'}
        size="sm"
      >
        <p className="text-sm text-slate-600 mb-6">
          {confirmModal.action === 'suspend'
            ? `Suspend ${confirmModal.organizer?.fullName}? They will lose access immediately.`
            : `Reactivate ${confirmModal.organizer?.fullName}? They will regain full access.`
          }
        </p>
        <div className="flex justify-end gap-2">
          <Button
            variant="secondary"
            onClick={() => setConfirmModal(s => ({ ...s, open: false }))}
          >
            Cancel
          </Button>
          <Button
            variant={confirmModal.action === 'suspend' ? 'danger' : 'primary'}
            loading={actioning}
            onClick={handleAction}
          >
            {confirmModal.action === 'suspend' ? 'Yes, Suspend' : 'Yes, Reactivate'}
          </Button>
        </div>
      </Modal>
    </AppLayout>
  );
};

export default OrganizersPage;
