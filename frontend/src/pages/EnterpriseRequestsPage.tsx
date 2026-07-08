import React, { useState, useEffect, useCallback } from 'react';
import { Building2, CheckCircle, XCircle } from 'lucide-react';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import EmptyState from '../components/ui/EmptyState';
import Modal from '../components/ui/Modal';
import Table, { Column } from '../components/ui/Table';
import api from '../services/api';
import toast from 'react-hot-toast';

interface EnterpriseRequest {
  id: string;
  userId: string;
  username: string;
  email: string;
  requestedPlan: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  adminNotes?: string;
  createdAt: string;
  // legacy aliases kept for render compatibility
  organizerName?: string;
  organizerEmail?: string;
  currentTier?: string;
  requestedAt?: string;
  reason?: string;
}

const EnterpriseRequestsPage: React.FC = () => {
  const [requests, setRequests]   = useState<EnterpriseRequest[]>([]);
  const [loading, setLoading]     = useState(true);
  const [actioning, setActioning] = useState(false);

  // Action modal state
  const [modal, setModal] = useState<{
    open: boolean;
    request: EnterpriseRequest | null;
    action: 'approve' | 'reject';
    notes: string;
  }>({ open: false, request: null, action: 'approve', notes: '' });

  const fetchRequests = useCallback(async () => {
    try {
      const { data } = await api.get<any>('/api/payments/upgrade/requests');
      setRequests(Array.isArray(data) ? data : (data.content ?? []));
    } catch {
      toast.error('Failed to load enterprise requests.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchRequests(); }, [fetchRequests]);

  const openModal = (request: EnterpriseRequest, action: 'approve' | 'reject') => {
    setModal({ open: true, request, action, notes: '' });
  };

  const handleAction = async () => {
    if (!modal.request) return;
    setActioning(true);
    const endpoint = modal.action === 'approve'
      ? `/api/payments/upgrade/approve/${modal.request.id}`
      : `/api/payments/upgrade/reject/${modal.request.id}`;

    try {
      await api.patch(endpoint, { adminNotes: modal.notes });
      toast.success(
        modal.action === 'approve'
          ? `${modal.request.organizerName} upgraded to ENTERPRISE.`
          : `Request rejected.`
      );
      setModal(s => ({ ...s, open: false }));
      fetchRequests();
    } catch {
      toast.error('Action failed. Please try again.');
    } finally {
      setActioning(false);
    }
  };

  const columns: Column<EnterpriseRequest>[] = [
    {
      key: 'organizer',
      header: 'Organizer',
      render: r => (
        <div>
          <p className="font-medium text-navy">{r.organizerName ?? r.username}</p>
          <p className="text-xs text-slate-400">{r.organizerEmail ?? r.email}</p>
        </div>
      ),
    },
    {
      key: 'currentTier',
      header: 'Current Plan',
      render: r => <Badge variant="info">{r.currentTier ?? r.requestedPlan}</Badge>,
    },
    {
      key: 'reason',
      header: 'Reason',
      render: r => (
        <span className="text-slate-500 text-xs line-clamp-2 max-w-xs">{r.reason ?? r.adminNotes ?? '—'}</span>
      ),
    },
    {
      key: 'requestedAt',
      header: 'Requested',
      render: r => new Date(r.requestedAt ?? r.createdAt).toLocaleDateString(),
    },
    {
      key: 'status',
      header: 'Status',
      render: r => (
        <Badge variant={
          r.status === 'APPROVED' ? 'success' :
          r.status === 'REJECTED' ? 'danger'  : 'warning'
        }>
          {r.status}
        </Badge>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: r => r.status !== 'PENDING' ? (
        <span className="text-xs text-slate-400">Resolved</span>
      ) : (
        <div className="flex gap-2">
          <Button
            size="sm"
            variant="primary"
            icon={<CheckCircle size={14} />}
            onClick={() => openModal(r, 'approve')}
          >
            Approve
          </Button>
          <Button
            size="sm"
            variant="danger"
            icon={<XCircle size={14} />}
            onClick={() => openModal(r, 'reject')}
          >
            Reject
          </Button>
        </div>
      ),
    },
  ];

  return (
    <AppLayout>
      <Card padding={false}>
        {loading ? (
          <div className="flex items-center justify-center h-48"><Spinner size="lg" /></div>
        ) : (
          <Table
            columns={columns}
            data={requests}
            keyExtractor={r => r.id}
            emptyState={
              <EmptyState
                icon={<Building2 size={32} />}
                heading="No enterprise requests"
                subtext="Upgrade requests from organizers appear here."
              />
            }
          />
        )}
      </Card>

      {/* Approve / Reject modal */}
      <Modal
        isOpen={modal.open}
        onClose={() => setModal(s => ({ ...s, open: false }))}
        title={modal.action === 'approve' ? 'Approve Enterprise Upgrade' : 'Reject Request'}
        size="md"
      >
        <p className="text-sm text-slate-600 mb-4">
          {modal.action === 'approve'
            ? `Approve ${modal.request?.organizerName ?? modal.request?.username} for the ${modal.request?.requestedPlan ?? 'ENTERPRISE'} plan?`
            : `Reject the enterprise request from ${modal.request?.organizerName ?? modal.request?.username}?`
          }
        </p>
        <div className="mb-6">
          <label className="block text-sm font-medium text-slate-700 mb-1.5">
            Admin Notes <span className="text-slate-400">(optional)</span>
          </label>
          <textarea
            rows={3}
            value={modal.notes}
            onChange={e => setModal(s => ({ ...s, notes: e.target.value }))}
            placeholder="Add a note for the organizer…"
            className="w-full px-3 py-2 border border-slate-200 rounded-input text-sm
                       outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20
                       resize-none"
          />
        </div>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setModal(s => ({ ...s, open: false }))}>
            Cancel
          </Button>
          <Button
            variant={modal.action === 'approve' ? 'primary' : 'danger'}
            loading={actioning}
            onClick={handleAction}
          >
            {modal.action === 'approve' ? 'Confirm Approval' : 'Confirm Rejection'}
          </Button>
        </div>
      </Modal>
    </AppLayout>
  );
};

export default EnterpriseRequestsPage;
