import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Mail, Calendar } from 'lucide-react';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import Table, { Column } from '../components/ui/Table';
import api from '../services/api';
import toast from 'react-hot-toast';

interface OrganizerDetail {
  id: string;
  username: string;
  email: string;
  fullName: string;
  status: string;
  tier: string;
  eventCount: number;
  joinedAt: string;
}

interface OrganizerEvent {
  id: string;
  title: string;
  eventDate: string;
  status: string;
  currentRsvpCount: number;
  maxCapacity: number;
}

interface PaymentTransaction {
  id: string;
  amount: number;
  currency: string;
  provider: string;
  status: string;
  createdAt: string;
}

type Tab = 'events' | 'payments';

const OrganizerDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [organizer, setOrganizer]   = useState<OrganizerDetail | null>(null);
  const [events, setEvents]         = useState<OrganizerEvent[]>([]);
  const [payments, setPayments]     = useState<PaymentTransaction[]>([]);
  const [activeTab, setActiveTab]   = useState<Tab>('events');
  const [loading, setLoading]       = useState(true);

  useEffect(() => {
    const fetchDetail = async () => {
      try {
        const [orgRes, evRes, payRes] = await Promise.allSettled([
          api.get<OrganizerDetail>(`/api/admin/organizers/${id}`),
          api.get<OrganizerEvent[]>(`/api/admin/organizers/${id}/events`),
          api.get<PaymentTransaction[]>(`/api/admin/organizers/${id}/payments`),
        ]);
        if (orgRes.status === 'fulfilled') setOrganizer(orgRes.value.data);
        if (evRes.status  === 'fulfilled') setEvents(evRes.value.data);
        if (payRes.status === 'fulfilled') setPayments(payRes.value.data);
      } catch {
        toast.error('Failed to load organizer details.');
      } finally {
        setLoading(false);
      }
    };
    fetchDetail();
  }, [id]);

  const eventColumns: Column<OrganizerEvent>[] = [
    { key: 'title',    header: 'Event',    render: e => <span className="font-medium text-navy">{e.title}</span> },
    { key: 'date',     header: 'Date',     render: e => new Date(e.eventDate).toLocaleDateString() },
    { key: 'status',   header: 'Status',   render: e => <Badge variant="info">{e.status}</Badge> },
    { key: 'rsvps',    header: 'RSVPs',    render: e => `${e.currentRsvpCount} / ${e.maxCapacity}` },
  ];

  const paymentColumns: Column<PaymentTransaction>[] = [
    { key: 'amount',   header: 'Amount',   render: p => `${p.currency} ${(p.amount / 100).toFixed(2)}` },
    { key: 'provider', header: 'Provider', render: p => <Badge variant="info">{p.provider}</Badge> },
    { key: 'status',   header: 'Status',   render: p => <Badge variant={p.status === 'SUCCESS' ? 'success' : 'danger'}>{p.status}</Badge> },
    { key: 'date',     header: 'Date',     render: p => new Date(p.createdAt).toLocaleDateString() },
  ];

  if (loading) {
    return (
      <AppLayout>
        <div className="flex items-center justify-center h-64"><Spinner size="lg" /></div>
      </AppLayout>
    );
  }

  if (!organizer) {
    return (
      <AppLayout>
        <p className="text-slate-500">Organizer not found.</p>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      <div className="space-y-6">

        {/* Back button */}
        <Button variant="ghost" icon={<ArrowLeft size={16} />} onClick={() => navigate('/organizers')}>
          Back to Organizers
        </Button>

        {/* Profile card */}
        <Card>
          <div className="flex items-start gap-4">
            <div className="w-14 h-14 rounded-full bg-primary-500 flex items-center justify-center text-white text-xl font-bold flex-shrink-0">
              {organizer.fullName[0]?.toUpperCase()}
            </div>
            <div className="flex-1">
              <h2 className="text-lg font-bold text-navy">{organizer.fullName}</h2>
              <p className="text-slate-500 text-sm">@{organizer.username}</p>
              <div className="flex items-center gap-3 mt-2 flex-wrap">
                <div className="flex items-center gap-1 text-xs text-slate-500">
                  <Mail size={12} /> {organizer.email}
                </div>
                <div className="flex items-center gap-1 text-xs text-slate-500">
                  <Calendar size={12} /> Joined {new Date(organizer.joinedAt).toLocaleDateString()}
                </div>
                <Badge variant="info">{organizer.tier}</Badge>
                <Badge variant={organizer.status === 'ACTIVE' ? 'success' : 'danger'}>
                  {organizer.status}
                </Badge>
              </div>
            </div>
          </div>
        </Card>

        {/* Tabs */}
        <div className="border-b border-slate-200">
          {(['events', 'payments'] as Tab[]).map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-4 py-2 text-sm font-medium capitalize transition-colors
                ${activeTab === tab
                  ? 'border-b-2 border-primary-500 text-primary-500'
                  : 'text-slate-500 hover:text-navy'
                }`}
            >
              {tab}
            </button>
          ))}
        </div>

        {/* Tab content */}
        <Card padding={false}>
          {activeTab === 'events'
            ? <Table columns={eventColumns}   data={events}   keyExtractor={e => e.id} />
            : <Table columns={paymentColumns} data={payments} keyExtractor={p => p.id} />
          }
        </Card>
      </div>
    </AppLayout>
  );
};

export default OrganizerDetailPage;
