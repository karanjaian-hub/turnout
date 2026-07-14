import React, { useState, useEffect, useCallback } from 'react';
import { Save, UserPlus, CheckCircle, XCircle } from 'lucide-react';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import Badge from '../components/ui/Badge';
import api from '../services/api';
import toast from 'react-hot-toast';

// ─── Types ────────────────────────────────────────────────────────────────────
interface TierLimit {
  tier: string;
  maxEvents: number;
  maxGuestsPerEvent: number;
}

interface ServiceHealth {
  name: string;
  status: 'UP' | 'DOWN' | 'UNKNOWN';
  port: number;
}

interface CreateAdminForm {
  username: string;
  email: string;
  fullName: string;
  password: string;
}

const SERVICES: ServiceHealth[] = [
  { name: 'api-gateway',          status: 'UNKNOWN', port: 8080 },
  { name: 'auth-service',         status: 'UNKNOWN', port: 8081 },
  { name: 'event-service',        status: 'UNKNOWN', port: 8082 },
  { name: 'guest-service',        status: 'UNKNOWN', port: 8083 },
  { name: 'email-service',        status: 'UNKNOWN', port: 8084 },
  { name: 'rsvp-service',         status: 'UNKNOWN', port: 8085 },
  { name: 'notification-service', status: 'UNKNOWN', port: 8086 },
  { name: 'payment-service',      status: 'UNKNOWN', port: 8087 },
  { name: 'ai-service',           status: 'UNKNOWN', port: 8088 },
];

// ─── Component ────────────────────────────────────────────────────────────────
const SettingsPage: React.FC = () => {
  const [tierLimits, setTierLimits]   = useState<TierLimit[]>([]);
  const [services, setServices]       = useState<ServiceHealth[]>(SERVICES);
  const [savingTiers, setSavingTiers] = useState(false);
  const [creatingAdmin, setCreatingAdmin] = useState(false);
  const [loadingTiers, setLoadingTiers]   = useState(true);

  const [adminForm, setAdminForm] = useState<CreateAdminForm>({
    username: '', email: '', fullName: '', password: '',
  });

  // ── Fetch tier limits ────────────────────────────────────────────────────────
  const fetchTierLimits = useCallback(async () => {
    try {
      const { data } = await api.get<TierLimit[]>('/api/admin/tier-limits');
      setTierLimits(data);
    } catch {
      // Backend not running yet — show editable placeholders
      setTierLimits([
        { tier: 'FREE',       maxEvents: 5,  maxGuestsPerEvent: 500 },
        { tier: 'PRO',        maxEvents: -1, maxGuestsPerEvent: 10000 },
        { tier: 'ENTERPRISE', maxEvents: -1, maxGuestsPerEvent: -1 },
      ]);
    } finally {
      setLoadingTiers(false);
    }
  }, []);

  // ── Poll service health ──────────────────────────────────────────────────────
  const checkHealth = useCallback(async () => {
    const results = await Promise.allSettled(
      SERVICES.map(svc =>
        api.get('/api/admin/system-health')
          .then(() => ({ ...svc, status: 'UP' as const }))
          .catch(() => ({ ...svc, status: 'DOWN' as const }))
      )
    );
    setServices(results.map(r => r.status === 'fulfilled' ? r.value : { ...SERVICES[0], status: 'DOWN' }));
  }, []);

  useEffect(() => {
    fetchTierLimits();
    checkHealth();
    // Re-check health every 30 seconds
    const interval = setInterval(checkHealth, 30_000);
    return () => clearInterval(interval);
  }, [fetchTierLimits, checkHealth]);

  // ── Update a tier limit field ────────────────────────────────────────────────
  const updateTierLimit = (tier: string, field: keyof Omit<TierLimit, 'tier'>, value: string) => {
    setTierLimits(prev =>
      prev.map(t => t.tier === tier ? { ...t, [field]: parseInt(value) || 0 } : t)
    );
  };

  const saveTierLimits = async () => {
    setSavingTiers(true);
    try {
      await api.put('/api/admin/tier-limits', tierLimits);
      toast.success('Tier limits saved.');
    } catch {
      toast.error('Failed to save tier limits.');
    } finally {
      setSavingTiers(false);
    }
  };

  // ── Create admin account ─────────────────────────────────────────────────────
  const createAdmin = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreatingAdmin(true);
    try {
      await api.post('/api/auth/admin/create-admin', adminForm);
      toast.success(`Admin account created for ${adminForm.username}.`);
      setAdminForm({ username: '', email: '', fullName: '', password: '' });
    } catch (err: any) {
      toast.error(err?.response?.data?.message ?? 'Failed to create admin.');
    } finally {
      setCreatingAdmin(false);
    }
  };

  return (
    <AppLayout>
      <div className="space-y-6 max-w-4xl">

        {/* ── Tier limits ── */}
        <Card>
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-base font-semibold text-navy">Tier Limits</h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Set -1 for unlimited. Changes apply to new events immediately.
              </p>
            </div>
            <Button
              size="sm"
              icon={<Save size={14} />}
              loading={savingTiers}
              onClick={saveTierLimits}
            >
              Save Changes
            </Button>
          </div>

          {loadingTiers ? (
            <div className="flex justify-center py-8"><Spinner /></div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100">
                    <th className="text-left py-2 px-3 text-xs font-semibold text-slate-500 uppercase">Tier</th>
                    <th className="text-left py-2 px-3 text-xs font-semibold text-slate-500 uppercase">Max Events</th>
                    <th className="text-left py-2 px-3 text-xs font-semibold text-slate-500 uppercase">Max Guests / Event</th>
                  </tr>
                </thead>
                <tbody>
                  {tierLimits.map(t => (
                    <tr key={t.tier} className="border-b border-slate-50">
                      <td className="py-3 px-3">
                        <Badge variant={t.tier === 'ENTERPRISE' ? 'success' : t.tier === 'PRO' ? 'warning' : 'info'}>
                          {t.tier}
                        </Badge>
                      </td>
                      <td className="py-3 px-3">
                        <input
                          type="number"
                          value={t.maxEvents}
                          onChange={e => updateTierLimit(t.tier, 'maxEvents', e.target.value)}
                          className="w-28 px-2 py-1.5 border border-slate-200 rounded-input text-sm
                                     outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                        />
                      </td>
                      <td className="py-3 px-3">
                        <input
                          type="number"
                          value={t.maxGuestsPerEvent}
                          onChange={e => updateTierLimit(t.tier, 'maxGuestsPerEvent', e.target.value)}
                          className="w-28 px-2 py-1.5 border border-slate-200 rounded-input text-sm
                                     outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        {/* ── Create admin account ── */}
        <Card>
          <h2 className="text-base font-semibold text-navy mb-6">Create Admin Account</h2>
          <form onSubmit={createAdmin} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {(
              [
                { field: 'fullName',  label: 'Full Name',  type: 'text',     placeholder: 'Jane Doe' },
                { field: 'username',  label: 'Username',   type: 'text',     placeholder: 'jane_admin' },
                { field: 'email',     label: 'Email',      type: 'email',    placeholder: 'jane@turnout.app' },
                { field: 'password',  label: 'Password',   type: 'password', placeholder: 'Min. 8 characters' },
              ] as const
            ).map(({ field, label, type, placeholder }) => (
              <div key={field}>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">{label}</label>
                <input
                  type={type}
                  value={adminForm[field]}
                  onChange={e => setAdminForm(s => ({ ...s, [field]: e.target.value }))}
                  placeholder={placeholder}
                  className="w-full px-3 py-2.5 border border-slate-200 rounded-input text-sm
                             outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20"
                />
              </div>
            ))}
            <div className="sm:col-span-2 flex justify-end">
              <Button
                type="submit"
                icon={<UserPlus size={16} />}
                loading={creatingAdmin}
                disabled={Object.values(adminForm).some(v => !v.trim())}
              >
                Create Admin
              </Button>
            </div>
          </form>
        </Card>

        {/* ── Service health ── */}
        <Card>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-semibold text-navy">System Health</h2>
            <Button size="sm" variant="secondary" onClick={checkHealth}>
              Refresh
            </Button>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {services.map(svc => (
              <div
                key={svc.name}
                className="flex items-center gap-3 p-3 rounded-input border border-slate-100 bg-slate-50"
              >
                {svc.status === 'UP' ? (
                  <CheckCircle size={18} className="text-success flex-shrink-0" />
                ) : svc.status === 'DOWN' ? (
                  <XCircle size={18} className="text-danger flex-shrink-0" />
                ) : (
                  <div className="w-4 h-4 rounded-full bg-slate-300 flex-shrink-0" />
                )}
                <div className="min-w-0">
                  <p className="text-xs font-medium text-navy truncate">{svc.name}</p>
                  <p className="text-xs text-slate-400">:{svc.port}</p>
                </div>
                <Badge
                  variant={svc.status === 'UP' ? 'success' : svc.status === 'DOWN' ? 'danger' : 'neutral'}
                  className="ml-auto flex-shrink-0"
                >
                  {svc.status}
                </Badge>
              </div>
            ))}
          </div>
        </Card>

      </div>
    </AppLayout>
  );
};

export default SettingsPage;
