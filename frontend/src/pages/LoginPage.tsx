import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Lock, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import PulseLine from '../components/ui/PulseLine';
import AmbientNetwork from '../components/ui/AmbientNetwork';
import api from '../services/api';

const useCountUp = (target: number, duration = 1200): number => {
  const [count, setCount] = useState(0);
  const frame = useRef<number | undefined>(undefined);
  useEffect(() => {
    if (!target) return;
    const start = performance.now();
    const tick  = (now: number) => {
      const p = Math.min((now - start) / duration, 1);
      setCount(Math.round((1 - Math.pow(1 - p, 3)) * target));
      if (p < 1) frame.current = requestAnimationFrame(tick);
    };
    frame.current = requestAnimationFrame(tick);
    return () => { if (frame.current) cancelAnimationFrame(frame.current); };
  }, [target, duration]);
  return count;
};

const LoginPage: React.FC = () => {
  const { login, isAuthenticated, isLoading } = useAuth();
  const navigate = useNavigate();

  const [username,     setUsername]     = useState('');
  const [password,     setPassword]     = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting,   setSubmitting]   = useState(false);
  const [error,        setError]        = useState<string | null>(null);
  const [shake,        setShake]        = useState(false);
  const [cardFlash,    setCardFlash]    = useState(false);
  const [mounted,      setMounted]      = useState(false);
  const [platformStat, setPlatformStat] = useState<number | null>(null);
  const animatedStat = useCountUp(platformStat ?? 0);

  useEffect(() => { if (!isLoading && isAuthenticated) navigate('/dashboard', { replace: true }); }, [isAuthenticated, isLoading, navigate]);
  useEffect(() => { const t = setTimeout(() => setMounted(true), 60); return () => clearTimeout(t); }, []);
  useEffect(() => {
    api.get('/api/admin/dashboard/stats').then(r => setPlatformStat(r.data?.totalEvents ?? null)).catch(() => {});
  }, []);

  const triggerError = (msg: string) => {
    setError(msg);
    setShake(true); setCardFlash(true);
    setTimeout(() => setShake(false), 450);
    setTimeout(() => setCardFlash(false), 600);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) return;
    setSubmitting(true); setError(null);
    try {
      await login(username.trim(), password);
      navigate('/dashboard', { replace: true });
    } catch (err: any) {
      triggerError(
        err?.message?.includes('Admin access only')
          ? 'Admin access only. Please use the organizer app.'
          : err?.response?.data?.message ?? 'Invalid username or password.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (isLoading) return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#0B1422' }}>
      <PulseLine color="#2563EB" duration={900} />
    </div>
  );

  return (
    <div style={{ minHeight: '100vh', display: 'flex', background: '#0B1422', overflow: 'hidden' }}>

      {/* ── Left panel — desktop only ── */}
      <div className="left-panel">
        <AmbientNetwork />
        {/* Centered branding block */}
        <div style={{ position: 'relative', zIndex: 1, textAlign: 'left' }}>
          <div style={{ fontFamily: "'Space Grotesk',sans-serif", fontSize: 64, fontWeight: 700, color: '#E2E8F0', letterSpacing: '-0.03em', lineHeight: 1 }}>
            TURNOUT
          </div>
          <div style={{ fontFamily: 'Inter', fontSize: 20, color: '#2563EB', marginTop: 12, fontWeight: 500 }}>
            No guest left behind.
          </div>
          <div style={{ marginTop: 24, width: 120 }}>
            <PulseLine color="#2563EB" duration={2000} />
          </div>
          {platformStat !== null && (
            <div style={{ marginTop: 32 }}>
              <div style={{ fontFamily: "'JetBrains Mono',monospace", fontSize: 44, fontWeight: 500, color: '#E2E8F0' }}>
                {animatedStat.toLocaleString()}
              </div>
              <div style={{ fontFamily: 'Inter', fontSize: 13, color: '#94A3B8', marginTop: 4 }}>
                platform-wide events tracked
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── Right panel — login form ── */}
      <div className="right-panel" style={{
        opacity:    mounted ? 1 : 0,
        transform:  mounted ? 'translateY(0)' : 'translateY(14px)',
        transition: 'opacity 400ms ease 80ms, transform 400ms ease 80ms',
      }}>
        <div className="glass-card" style={{
          border: `1px solid ${cardFlash ? 'rgba(220,38,38,0.6)' : 'rgba(255,255,255,0.08)'}`,
          transition: 'border-color 200ms ease',
        }}>

          <div style={{ marginBottom: 28 }}>
            <PulseLine color="#2563EB" duration={1700} />
          </div>

          <h2 style={{ fontFamily: "'Space Grotesk',sans-serif", fontSize: 32, fontWeight: 600, color: '#E2E8F0', marginBottom: 8, letterSpacing: '-0.02em' }}>
            Admin Sign In
          </h2>
          <p style={{ fontFamily: 'Inter', fontSize: 15, color: '#94A3B8', marginBottom: 36 }}>
            Sign in to manage your platform
          </p>

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>

            {/* Username */}
            <div>
              <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 8, fontFamily: 'Inter', letterSpacing: '0.02em', textTransform: 'uppercase' }}>
                Username
              </label>
              <div style={{ position: 'relative' }}>
                <User size={16} style={{ position: 'absolute', left: 16, top: '50%', transform: 'translateY(-50%)', color: '#94A3B8', pointerEvents: 'none' }} />
                <input
                  type="text" value={username} onChange={e => setUsername(e.target.value)}
                  placeholder="Enter your username" autoComplete="username" autoFocus
                  style={{ width: '100%', padding: '15px 18px 15px 46px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 12, color: '#E2E8F0', fontSize: 15, fontFamily: 'Inter', outline: 'none', boxSizing: 'border-box', transition: 'border-color 200ms, box-shadow 200ms' }}
                  onFocus={e => { e.target.style.borderColor = '#2563EB'; e.target.style.boxShadow = '0 0 0 3px rgba(37,99,235,0.2)'; }}
                  onBlur={e  => { e.target.style.borderColor = 'rgba(255,255,255,0.1)'; e.target.style.boxShadow = 'none'; }}
                />
              </div>
            </div>

            {/* Password */}
            <div>
              <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 8, fontFamily: 'Inter', letterSpacing: '0.02em', textTransform: 'uppercase' }}>
                Password
              </label>
              <div style={{ position: 'relative' }}>
                <Lock size={16} style={{ position: 'absolute', left: 16, top: '50%', transform: 'translateY(-50%)', color: '#94A3B8', pointerEvents: 'none' }} />
                <input
                  type={showPassword ? 'text' : 'password'} value={password} onChange={e => setPassword(e.target.value)}
                  placeholder="Enter your password" autoComplete="current-password"
                  style={{ width: '100%', padding: '15px 52px 15px 46px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 12, color: '#E2E8F0', fontSize: 15, fontFamily: 'Inter', outline: 'none', boxSizing: 'border-box', transition: 'border-color 200ms, box-shadow 200ms' }}
                  onFocus={e => { e.target.style.borderColor = '#2563EB'; e.target.style.boxShadow = '0 0 0 3px rgba(37,99,235,0.2)'; }}
                  onBlur={e  => { e.target.style.borderColor = 'rgba(255,255,255,0.1)'; e.target.style.boxShadow = 'none'; }}
                />
                <button type="button" onClick={() => setShowPassword(v => !v)}
                  style={{ position: 'absolute', right: 16, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', color: '#94A3B8', cursor: 'pointer', padding: 0, display: 'flex' }}>
                  {showPassword ? <EyeOff size={16}/> : <Eye size={16}/>}
                </button>
              </div>
            </div>

            {/* Error */}
            {error && (
              <div style={{ fontSize: 13, color: '#DC2626', fontFamily: 'Inter', padding: '12px 16px', background: 'rgba(220,38,38,0.08)', borderRadius: 10, border: '1px solid rgba(220,38,38,0.2)', animation: shake ? 'loginShake 0.45s ease-out' : 'none' }}>
                {error}
              </div>
            )}

            {/* Submit */}
            <button type="submit" disabled={submitting || !username.trim() || !password.trim()}
              style={{ width: '100%', height: 52, background: '#2563EB', border: 'none', borderRadius: 12, color: '#fff', fontSize: 16, fontWeight: 600, fontFamily: 'Inter', cursor: submitting ? 'not-allowed' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: 4, transition: 'transform 200ms ease, box-shadow 200ms ease', opacity: (submitting || !username.trim() || !password.trim()) ? 0.5 : 1 }}
              onMouseEnter={e => { if (!submitting) { e.currentTarget.style.transform = 'translateY(-2px)'; e.currentTarget.style.boxShadow = '0 10px 30px rgba(37,99,235,0.45)'; }}}
              onMouseLeave={e => { e.currentTarget.style.transform = 'none'; e.currentTarget.style.boxShadow = 'none'; }}
              onMouseDown={e  => { e.currentTarget.style.transform = 'scale(0.98)'; }}
              onMouseUp={e    => { e.currentTarget.style.transform = 'translateY(-2px)'; }}
            >
              {submitting ? <PulseLine color="#ffffff" duration={900} /> : 'Sign In'}
            </button>

            <p style={{ textAlign: 'center', fontSize: 14, color: '#64748B', fontFamily: 'Inter', marginTop: 4 }}>
              Forgot your password?{' '}
              <span style={{ color: '#2563EB', cursor: 'pointer' }}
                onMouseEnter={e => (e.currentTarget.style.textDecoration = 'underline')}
                onMouseLeave={e => (e.currentTarget.style.textDecoration = 'none')}>
                Contact your super admin.
              </span>
            </p>
          </form>
        </div>
      </div>

      <style>{`
        /* Left panel */
        .left-panel {
          display: none;
          position: relative;
          flex: 0 0 55%;
          align-items: center;
          justify-content: center;
          padding: 4rem;
        }
        @media (min-width: 1024px) {
          .left-panel { display: flex; }
        }

        /* Right panel */
        .right-panel {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 1.5rem;
          min-height: 100vh;
        }

        /* Glass card */
        .glass-card {
          width: 100%;
          max-width: 480px;
          background: rgba(255,255,255,0.04);
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
          border-radius: 24px;
          padding: 52px 48px;
          box-shadow: 0 0 60px rgba(37,99,235,0.07), 0 30px 80px rgba(0,0,0,0.5);
        }

        /* Mobile: tighter padding */
        @media (max-width: 480px) {
          .glass-card { padding: 36px 24px; border-radius: 20px; }
        }

        @keyframes loginShake {
          0%,100% { transform: translateX(0); }
          15% { transform: translateX(-5px); }
          30% { transform: translateX(5px); }
          45% { transform: translateX(-3px); }
          60% { transform: translateX(3px); }
          90% { transform: translateX(1px); }
        }
        @media (prefers-reduced-motion: reduce) {
          * { animation-duration: 0.01ms !important; transition-duration: 0.01ms !important; }
        }
      `}</style>
    </div>
  );
};

export default LoginPage;
