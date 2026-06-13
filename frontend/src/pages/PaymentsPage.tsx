import React, { useState, useEffect, useCallback } from 'react';
import { CreditCard, DollarSign, Smartphone } from 'lucide-react';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import StatCard from '../components/ui/StatCard';
import Badge from '../components/ui/Badge';
import Spinner from '../components/ui/Spinner';
import Table, { Column } from '../components/ui/Table';
import api from '../services/api';
import toast from 'react-hot-toast';

interface PaymentSummary {
  totalRevenue: number;
  mpesaRevenue: number;
  stripeRevenue: number;
  transactionCount: number;
}

interface Transaction {
  id: string;
  organizerName: string;
  amount: number;
  currency: string;
  provider: 'MPESA' | 'STRIPE';
  status: 'SUCCESS' | 'FAILED' | 'PENDING' | 'REFUNDED';
  description: string;
  createdAt: string;
}

const STATUS_BADGE: Record<string, 'success' | 'danger' | 'warning' | 'neutral'> = {
  SUCCESS:  'success',
  FAILED:   'danger',
  PENDING:  'warning',
  REFUNDED: 'neutral',
};

const PROVIDERS = ['ALL', 'MPESA', 'STRIPE'];
const STATUSES  = ['ALL', 'SUCCESS', 'FAILED', 'PENDING', 'REFUNDED'];

const PaymentsPage: React.FC = () => {
  const [summary, setSummary]       = useState<PaymentSummary | null>(null);
  const [transactions, setTx]       = useState<Transaction[]>([]);
  const [filtered, setFiltered]     = useState<Transaction[]>([]);
  const [provider, setProvider]     = useState('ALL');
  const [status, setStatus]         = useState('ALL');
  const [loading, setLoading]       = useState(true);

  const fetchPayments = useCallback(async () => {
    try {
      const [sumRes, txRes] = await Promise.allSettled([
        api.get<PaymentSummary>('/api/admin/payments/summary'),
        api.get<Transaction[]>('/api/admin/payments/transactions'),
      ]);
      if (sumRes.status === 'fulfilled') setSummary(sumRes.value.data);
      if (txRes.status  === 'fulfilled') {
        setTx(txRes.value.data);
        setFiltered(txRes.value.data);
      }
    } catch {
      toast.error('Failed to load payment data.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchPayments(); }, [fetchPayments]);

  useEffect(() => {
    let result = transactions;
    if (provider !== 'ALL') result = result.filter(t => t.provider === provider);
    if (status  !== 'ALL')  result = result.filter(t => t.status   === status);
    setFiltered(result);
  }, [provider, status, transactions]);

  const columns: Column<Transaction>[] = [
    {
      key: 'organizer',
      header: 'Organizer',
      render: t => <span className="font-medium text-navy">{t.organizerName}</span>,
    },
    {
      key: 'amount',
      header: 'Amount',
      sortable: true,
      render: t => (
        <span className="font-semibold">
          {t.currency} {(t.amount / 100).toFixed(2)}
        </span>
      ),
    },
    {
      key: 'provider',
      header: 'Provider',
      render: t => (
        <Badge variant={t.provider === 'MPESA' ? 'success' : 'info'}>
          {t.provider}
        </Badge>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: t => (
        <Badge variant={STATUS_BADGE[t.status] ?? 'neutral'}>{t.status}</Badge>
      ),
    },
    {
      key: 'description',
      header: 'Description',
      render: t => <span className="text-slate-500 text-xs">{t.description}</span>,
    },
    {
      key: 'date',
      header: 'Date',
      sortable: true,
      render: t => new Date(t.createdAt).toLocaleDateString(),
    },
  ];

  return (
    <AppLayout>
      <div className="space-y-6">

        {/* Summary stat cards */}
        {summary && (
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
            <StatCard label="Total Revenue"    value={summary.totalRevenue / 100}    icon={<DollarSign size={20} />}  accent="blue" />
            <StatCard label="M-Pesa Revenue"   value={summary.mpesaRevenue / 100}    icon={<Smartphone size={20} />}  accent="green" />
            <StatCard label="Stripe Revenue"   value={summary.stripeRevenue / 100}   icon={<CreditCard size={20} />}  accent="navy" />
            <StatCard label="Transactions"     value={summary.transactionCount}      icon={<CreditCard size={20} />}  accent="amber" />
          </div>
        )}

        {/* Filters */}
        <Card padding={false} className="p-4">
          <div className="flex flex-wrap gap-4">
            <div>
              <p className="text-xs text-slate-500 mb-1.5 font-medium">Provider</p>
              <div className="flex gap-1">
                {PROVIDERS.map(p => (
                  <button
                    key={p}
                    onClick={() => setProvider(p)}
                    className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors
                      ${provider === p ? 'bg-navy text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}
                  >
                    {p}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <p className="text-xs text-slate-500 mb-1.5 font-medium">Status</p>
              <div className="flex gap-1">
                {STATUSES.map(s => (
                  <button
                    key={s}
                    onClick={() => setStatus(s)}
                    className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors
                      ${status === s ? 'bg-navy text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </Card>

        <Card padding={false}>
          {loading ? (
            <div className="flex items-center justify-center h-48"><Spinner size="lg" /></div>
          ) : (
            <Table
              columns={columns}
              data={filtered}
              keyExtractor={t => t.id}
            />
          )}
        </Card>
      </div>
    </AppLayout>
  );
};

export default PaymentsPage;
