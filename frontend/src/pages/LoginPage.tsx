import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Lock, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import Spinner from '../components/ui/Spinner';

const LoginPage: React.FC = () => {
  const { login, isAuthenticated, isLoading } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername]       = useState('');
  const [password, setPassword]       = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting]   = useState(false);
  const [error, setError]             = useState<string | null>(null);
  // shake triggers the CSS animation on wrong credentials
  const [shake, setShake]             = useState(false);

  // If already authenticated, skip the login page entirely
  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, isLoading, navigate]);

  const triggerShake = () => {
    setShake(true);
    // Reset after animation completes so it can fire again on next bad attempt
    setTimeout(() => setShake(false), 500);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) return;

    setSubmitting(true);
    setError(null);

    try {
      await login(username.trim(), password);
      navigate('/dashboard', { replace: true });
    } catch (err: any) {
      const message =
        err?.message?.includes('Admin access only')
          ? 'Admin access only. Please use the organizer app.'
          : err?.response?.data?.message ?? 'Invalid username or password.';

      setError(message);
      triggerShake();
    } finally {
      setSubmitting(false);
    }
  };

  // Show a full-screen spinner while the silent refresh is in progress
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div className="min-h-screen flex">

      {/* ── Left panel — navy gradient, hidden on mobile ── */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-navy to-primary-600 flex-col justify-between p-12 relative overflow-hidden">

        {/* Decorative background circles — depth without images */}
        <div className="absolute top-[-80px] right-[-80px] w-80 h-80 rounded-full bg-white/5" />
        <div className="absolute bottom-[-60px] left-[-60px] w-64 h-64 rounded-full bg-white/10" />
        <div className="absolute top-1/2 left-1/3 w-32 h-32 rounded-full bg-primary-500/20" />

        {/* Logo */}
        <div className="relative z-10">
          <div className="flex items-center gap-2">
            <span className="text-4xl font-bold text-white tracking-tight">TURNOUT</span>
            <span className="w-3 h-3 rounded-full bg-primary-400 mt-1" />
          </div>
          <p className="text-blue-200 mt-2 text-lg">No guest left behind.</p>
        </div>

        {/* Centre copy */}
        <div className="relative z-10">
          <h2 className="text-3xl font-bold text-white leading-tight">
            Event management<br />at any scale.
          </h2>
          <p className="text-slate-300 mt-4 text-base leading-relaxed max-w-sm">
            Manage organizers, track RSVPs, monitor payments, and keep
            every guest experience seamless — all from one panel.
          </p>
        </div>

        {/* Bottom stat */}
        <p className="relative z-10 text-slate-400 text-sm">
          Powering events at any scale.
        </p>
      </div>

      {/* ── Right panel — login form ── */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 bg-white">
        <div
          className="w-full max-w-md"
          // inline style because Tailwind can't dynamically compose keyframe names
          style={shake ? { animation: 'shake 0.5s ease' } : {}}
        >
          {/* Header */}
          <div className="mb-8">
            {/* Show logo on mobile where left panel is hidden */}
            <div className="flex items-center gap-1 mb-6 lg:hidden">
              <span className="text-2xl font-bold text-navy">TURNOUT</span>
              <span className="w-2 h-2 rounded-full bg-primary-500 mt-0.5" />
            </div>
            <h2 className="text-2xl font-bold text-navy">Admin Sign In</h2>
            <p className="text-slate-500 mt-1 text-sm">
              Restricted to SUPER_ADMIN and ADMIN accounts.
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-5">

            {/* Username */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">
                Username
              </label>
              <div className="relative">
                <User
                  size={16}
                  className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                />
                <input
                  type="text"
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  placeholder="Enter your username"
                  autoComplete="username"
                  autoFocus
                  className="w-full pl-9 pr-4 py-2.5 border border-slate-200 rounded-input text-sm
                             text-slate-800 placeholder-slate-400 outline-none
                             focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20
                             transition-all duration-150"
                />
              </div>
            </div>

            {/* Password */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">
                Password
              </label>
              <div className="relative">
                <Lock
                  size={16}
                  className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  autoComplete="current-password"
                  className="w-full pl-9 pr-10 py-2.5 border border-slate-200 rounded-input text-sm
                             text-slate-800 placeholder-slate-400 outline-none
                             focus:border-primary-500 focus:ring-2 focus:ring-primary-500/20
                             transition-all duration-150"
                />
                {/* Eye toggle */}
                <button
                  type="button"
                  onClick={() => setShowPassword(v => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-navy transition-colors"
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            {/* Error message */}
            {error && (
              <div className="flex items-start gap-2 p-3 bg-red-50 border border-red-200 rounded-input">
                <span className="text-danger text-sm">{error}</span>
              </div>
            )}

            {/* Submit */}
            <button
              type="submit"
              disabled={submitting || !username.trim() || !password.trim()}
              className="w-full flex items-center justify-center gap-2 py-2.5 px-4
                         bg-gradient-to-r from-navy to-primary-500 text-white text-sm font-medium
                         rounded-input shadow-md
                         hover:from-primary-600 hover:to-primary-500
                         hover:-translate-y-0.5 active:translate-y-0
                         disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none
                         transition-all duration-150"
            >
              {submitting
                ? <><Spinner size="sm" className="border-white border-t-transparent" /> Signing in...</>
                : 'Sign In'
              }
            </button>

            {/* Forgot password hint */}
            <p className="text-center text-sm text-slate-400">
              Forgot your password?{' '}
              <span className="text-primary-500 cursor-pointer hover:underline">
                Contact your super admin.
              </span>
            </p>
          </form>
        </div>
      </div>

      {/* ── Shake keyframe — defined inline because CRA doesn't support arbitrary keyframes ── */}
      <style>{`
        @keyframes shake {
          0%, 100% { transform: translateX(0); }
          15%       { transform: translateX(-6px); }
          30%       { transform: translateX(6px); }
          45%       { transform: translateX(-4px); }
          60%       { transform: translateX(4px); }
          75%       { transform: translateX(-2px); }
          90%       { transform: translateX(2px); }
        }
      `}</style>
    </div>
  );
};

export default LoginPage;
