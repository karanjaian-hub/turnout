import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { validateToken, submitResponse } from '../services/api';
import type { RsvpDetails } from '../services/api';
import PageCard from '../components/PageCard';
import Spinner from '../components/Spinner';

const RsvpPage: React.FC = () => {
  const [params]   = useSearchParams();
  const navigate   = useNavigate();
  const token      = params.get('token');

  const [details,    setDetails]    = useState<RsvpDetails | null>(null);
  const [loading,    setLoading]    = useState(true);
  const [submitting, setSubmitting] = useState<'CONFIRMED' | 'DECLINED' | null>(null);

  useEffect(() => {
    if (!token) { navigate('/rsvp/invalid', { replace: true }); return; }

    validateToken(token)
      .then(data => {
        if (data.status === 'CONFIRMED') { navigate('/rsvp/confirmed', { replace: true }); return; }
        if (data.status === 'DECLINED')  { navigate('/rsvp/declined',  { replace: true }); return; }
        setDetails(data);
      })
      .catch(err => {
        const status = err?.response?.status;
        if (status === 404 || status === 400) navigate('/rsvp/invalid', { replace: true });
        else navigate('/rsvp/error', { replace: true });
      })
      .finally(() => setLoading(false));
  }, [token, navigate]);

  const handleResponse = async (response: 'CONFIRMED' | 'DECLINED') => {
    if (!token || !details) return;
    setSubmitting(response);
    try {
      await submitResponse(token, response);
      navigate(response === 'CONFIRMED' ? '/rsvp/confirmed' : '/rsvp/declined');
    } catch {
      navigate('/rsvp/error');
    }
  };

  if (loading) return (
    <PageCard>
      <div style={{ textAlign: 'center', padding: '32px 0' }}>
        <Spinner />
        <p style={{ marginTop: 16, fontSize: 14, color: '#64748B', fontFamily: 'Inter' }}>
          Loading your invitation…
        </p>
      </div>
    </PageCard>
  );

  if (!details) return null;

  const eventDate = new Date(details.eventDate);
  const formattedDate = eventDate.toLocaleDateString('en-KE', {
    weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
  });
  const formattedTime = eventDate.toLocaleTimeString('en-KE', {
    hour: '2-digit', minute: '2-digit',
  });

  return (
    <PageCard>
      <div style={{ textAlign: 'center', marginBottom: 28 }}>
        <div style={{
          display: 'inline-flex', alignItems: 'center', gap: 6,
          background: '#EFF6FF', borderRadius: 999,
          padding: '4px 14px', marginBottom: 16,
        }}>
          <span style={{ width: 7, height: 7, borderRadius: '50%', background: '#2563EB' }}/>
          <span style={{ fontSize: 12, fontWeight: 600, color: '#2563EB', fontFamily: 'Inter', letterSpacing: '0.05em' }}>
            YOU'RE INVITED
          </span>
        </div>
        <h1 style={{
          fontFamily: "'Space Grotesk', sans-serif",
          fontSize: 26, fontWeight: 700, color: '#0F172A',
          lineHeight: 1.2, marginBottom: 8,
        }}>
          {details.eventTitle}
        </h1>
        <p style={{ fontSize: 14, color: '#64748B', fontFamily: 'Inter' }}>
          Hi <strong style={{ color: '#0F172A' }}>{details.guestName}</strong> — you've been invited!
        </p>
      </div>

      <div style={{ height: 1, background: '#F1F5F9', marginBottom: 24 }} />

      <div style={{ display: 'flex', flexDirection: 'column', gap: 14, marginBottom: 28 }}>
        <DetailRow icon="📅" label="Date" value={formattedDate} />
        <DetailRow icon="🕐" label="Time" value={formattedTime} />
        <DetailRow icon="📍" label="Location" value={details.eventLocation} />
        {details.organizerName && (
          <DetailRow icon="👤" label="Organizer" value={details.organizerName} />
        )}
        {details.eventDescription && (
          <div style={{ padding: '12px 14px', background: '#F8FAFC', borderRadius: 10, borderLeft: '3px solid #2563EB' }}>
            <p style={{ fontSize: 13, color: '#475569', fontFamily: 'Inter', lineHeight: 1.6 }}>
              {details.eventDescription}
            </p>
          </div>
        )}
      </div>

      {details.maxCapacity && details.currentRsvpCount !== undefined && (
        <div style={{ marginBottom: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
            <span style={{ fontSize: 12, color: '#64748B', fontFamily: 'Inter' }}>Spots filled</span>
            <span style={{ fontSize: 12, fontWeight: 600, color: '#0F172A', fontFamily: 'Inter' }}>
              {details.currentRsvpCount} / {details.maxCapacity}
            </span>
          </div>
          <div style={{ height: 4, background: '#F1F5F9', borderRadius: 99 }}>
            <div style={{
              height: '100%',
              width: `${Math.min((details.currentRsvpCount / details.maxCapacity) * 100, 100)}%`,
              background: 'linear-gradient(90deg, #1E3A5F, #2563EB)',
              borderRadius: 99,
              transition: 'width 600ms ease',
            }}/>
          </div>
        </div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <button
          onClick={() => handleResponse('CONFIRMED')}
          disabled={!!submitting}
          style={{
            width: '100%', height: 52,
            background: submitting === 'CONFIRMED' ? '#15803D' : '#16A34A',
            border: 'none', borderRadius: 12,
            color: '#fff', fontSize: 16, fontWeight: 600, fontFamily: 'Inter',
            cursor: submitting ? 'not-allowed' : 'pointer',
            opacity: submitting && submitting !== 'CONFIRMED' ? 0.5 : 1,
            transition: 'all 200ms ease',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          }}
        >
          {submitting === 'CONFIRMED' ? (
            <>
              <div style={{ width: 18, height: 18, border: '2px solid rgba(255,255,255,0.4)', borderTop: '2px solid #fff', borderRadius: '50%', animation: 'spin 800ms linear infinite' }}/>
              Confirming…
            </>
          ) : "✓ Yes, I'll be there"}
        </button>

        <button
          onClick={() => handleResponse('DECLINED')}
          disabled={!!submitting}
          style={{
            width: '100%', height: 52,
            background: 'transparent',
            border: '1px solid #E2E8F0', borderRadius: 12,
            color: '#64748B', fontSize: 15, fontWeight: 500, fontFamily: 'Inter',
            cursor: submitting ? 'not-allowed' : 'pointer',
            opacity: submitting && submitting !== 'DECLINED' ? 0.5 : 1,
            transition: 'all 200ms ease',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          }}
          onMouseEnter={e => { if (!submitting) { e.currentTarget.style.background = '#FEF2F2'; e.currentTarget.style.borderColor = '#FECACA'; e.currentTarget.style.color = '#DC2626'; }}}
          onMouseLeave={e => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.borderColor = '#E2E8F0'; e.currentTarget.style.color = '#64748B'; }}
        >
          {submitting === 'DECLINED' ? (
            <>
              <div style={{ width: 18, height: 18, border: '2px solid rgba(100,116,139,0.4)', borderTop: '2px solid #64748B', borderRadius: '50%', animation: 'spin 800ms linear infinite' }}/>
              Declining…
            </>
          ) : "✕ Sorry, I can't make it"}
        </button>
      </div>

      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </PageCard>
  );
};

const DetailRow: React.FC<{ icon: string; label: string; value: string }> = ({ icon, label, value }) => (
  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
    <span style={{ fontSize: 18, flexShrink: 0, marginTop: 1 }}>{icon}</span>
    <div>
      <p style={{ fontSize: 11, color: '#94A3B8', fontFamily: 'Inter', fontWeight: 500, textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 2 }}>
        {label}
      </p>
      <p style={{ fontSize: 14, color: '#0F172A', fontFamily: 'Inter', fontWeight: 500 }}>
        {value}
      </p>
    </div>
  </div>
);

export default RsvpPage;
