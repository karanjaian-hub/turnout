import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';

// ─── Page placeholders ────────────────────────────────────────────────────────
// We stub every page now so the router compiles. Each gets replaced in 11.5–11.7.
const LoginPage        = React.lazy(() => import('./pages/LoginPage'));
const DashboardPage    = React.lazy(() => import('./pages/DashboardPage'));
const OrganizersPage   = React.lazy(() => import('./pages/OrganizersPage'));
const EventsBrowserPage = React.lazy(() => import('./pages/EventsBrowserPage'));
const PaymentsPage     = React.lazy(() => import('./pages/PaymentsPage'));
const SettingsPage     = React.lazy(() => import('./pages/SettingsPage'));
const UnauthorizedPage = React.lazy(() => import('./pages/UnauthorizedPage'));
const OrganizerDetailPage = React.lazy(() => import('./pages/OrganizerDetailPage'));
const EnterpriseRequestsPage = React.lazy(() => import('./pages/EnterpriseRequestsPage'));

// ─── Query client ─────────────────────────────────────────────────────────────
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000, // 30 seconds before a cached result is considered stale
    },
  },
});

// ─── Route guards ─────────────────────────────────────────────────────────────

// Blocks unauthenticated users — sends them to /login
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="w-8 h-8 border-4 border-navy border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

// Blocks anyone who isn't SUPER_ADMIN
const SuperAdminRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isSuperAdmin, isLoading } = useAuth();

  if (isLoading) return null;

  return isSuperAdmin() ? <>{children}</> : <Navigate to="/unauthorized" replace />;
};

// ─── App ──────────────────────────────────────────────────────────────────────
const App: React.FC = () => (
  <QueryClientProvider client={queryClient}>
    <BrowserRouter>
      <AuthProvider>
        {/* Global toast notifications — top-right, navy accent */}
        <Toaster
          position="top-right"
          toastOptions={{
            style: {
              fontFamily: 'DM Sans, sans-serif',
              fontSize: '14px',
            },
            success: { iconTheme: { primary: '#16A34A', secondary: '#fff' } },
            error:   { iconTheme: { primary: '#DC2626', secondary: '#fff' } },
          }}
        />

        <React.Suspense fallback={
          <div className="min-h-screen flex items-center justify-center bg-slate-50">
            <div className="w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin" />
          </div>
        }>
          <Routes>
            {/* Public */}
            <Route path="/login"        element={<LoginPage />} />
            <Route path="/unauthorized" element={<UnauthorizedPage />} />

            {/* Protected — any authenticated admin */}
            <Route path="/dashboard" element={
              <ProtectedRoute><DashboardPage /></ProtectedRoute>
            } />
            <Route path="/organizers/:id" element={
              <ProtectedRoute><OrganizerDetailPage /></ProtectedRoute>
            } />
            <Route path="/organizers" element={
              <ProtectedRoute><OrganizersPage /></ProtectedRoute>
            } />
            <Route path="/events" element={
              <ProtectedRoute><EventsBrowserPage /></ProtectedRoute>
            } />
            <Route path="/payments/enterprise" element={
              <ProtectedRoute><EnterpriseRequestsPage /></ProtectedRoute>
            } />
            <Route path="/payments" element={
              <ProtectedRoute><PaymentsPage /></ProtectedRoute>
            } />

            {/* Super admin only */}
            <Route path="/settings" element={
              <SuperAdminRoute><SettingsPage /></SuperAdminRoute>
            } />

            {/* Default redirect */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </React.Suspense>
      </AuthProvider>
    </BrowserRouter>
  </QueryClientProvider>
);

export default App;
